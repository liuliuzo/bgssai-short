package com.bgssai.shortai.production.service;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.elasticsearch.ElasticsearchClientHolder;
import com.bgssai.shortai.model.ShortLookMaterial;
import com.bgssai.shortai.production.service.LookKnowledgeService.LookHit;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

/**
 * 定妆素材的检索。图片说明写入开发共用 Elasticsearch，生成时按外观描述召回一张参考图。
 * 集群不可用时调用方继续用库内名称匹配。
 */
@Service
public class LookRagIndex {

    private static final Logger log = LoggerFactory.getLogger(LookRagIndex.class);

    private static final String MAPPING = """
            {
              "settings": {
                "index.max_ngram_diff": 1,
                "analysis": {
                  "tokenizer": {
                    "han_ng": { "type": "ngram", "min_gram": 2, "max_gram": 3, "token_chars": ["letter"] }
                  },
                  "analyzer": {
                    "han_ngram": { "type": "custom", "tokenizer": "han_ng" }
                  }
                }
              },
              "mappings": {
                "properties": {
                  "material_id": { "type": "keyword" },
                  "user_id": { "type": "keyword" },
                  "library_id": { "type": "keyword" },
                  "material_kind": { "type": "keyword" },
                  "view_angle": { "type": "keyword" },
                  "material_name": { "type": "text", "analyzer": "han_ngram" },
                  "caption": { "type": "text", "analyzer": "han_ngram" },
                  "storage_key": { "type": "keyword" },
                  "mime_type": { "type": "keyword" }
                }
              }
            }
            """;

    @Autowired
    private ElasticsearchClientHolder elasticsearch;

    private volatile boolean indexReady;

    public void upsert(ShortLookMaterial row) {
        if (row == null || row.getMaterialId() == null || row.getStorageKey() == null) {
            return;
        }
        ElasticsearchClient client = client();
        if (client == null) {
            return;
        }
        try {
            ensureIndex(client);
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("material_id", row.getMaterialId());
            doc.put("user_id", row.getUserId());
            doc.put("library_id", row.getLibraryId());
            doc.put("material_kind", row.getMaterialKind());
            doc.put("view_angle", row.getViewAngle());
            doc.put("material_name", row.getMaterialName());
            doc.put("caption", row.getCaption() == null ? "" : row.getCaption());
            doc.put("storage_key", row.getStorageKey());
            doc.put("mime_type", row.getMimeType());
            client.index(request -> request.index(indexName()).id(row.getMaterialId()).document(doc));
        } catch (Exception ex) {
            log.warn("look rag index failed material={} type={}", row.getMaterialId(), ex.getClass().getSimpleName());
        }
    }

    public void remove(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            return;
        }
        ElasticsearchClient client = client();
        if (client == null) {
            return;
        }
        try {
            client.delete(request -> request.index(indexName()).id(materialId));
        } catch (Exception ex) {
            log.warn("look rag delete failed material={} type={}", materialId, ex.getClass().getSimpleName());
        }
    }

    public LookHit search(String userId, Set<String> libraryIds, String kind, String prompt) {
        if (userId == null || userId.isBlank() || libraryIds == null || libraryIds.isEmpty()
                || prompt == null || prompt.isBlank()) {
            return null;
        }
        ElasticsearchClient client = client();
        if (client == null) {
            return null;
        }
        try {
            ensureIndex(client);
            SearchResponse<Map> response = client.search(request -> request.index(indexName()).size(20)
                    .query(query -> query.bool(bool -> bool
                            .filter(filter -> filter.term(term -> term.field("user_id").value(userId)))
                            .filter(filter -> filter.terms(terms -> terms.field("library_id")
                                    .terms(values -> values.value(libraryIds.stream()
                                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                            .toList()))))
                            .filter(filter -> filter.term(term -> term.field("material_kind").value(kind)))
                            .must(must -> must.multiMatch(match -> match.query(prompt)
                                    .fields("material_name^3", "caption")))
                            .should(should -> should.term(term -> term.field("view_angle")
                                    .value("FRONT_FACE").boost(3.0f))))),
                    Map.class);
            return choose(prompt, response.hits().hits());
        } catch (Exception ex) {
            log.warn("look rag search failed type={}", ex.getClass().getSimpleName());
            return null;
        }
    }

    static LookHit choose(String prompt, List<Hit<Map>> hits) {
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        LookHit best = null;
        int bestScore = 0;
        for (Hit<Map> hit : hits) {
            Map source = hit.source();
            if (source == null) {
                continue;
            }
            String name = text(source.get("material_name"));
            String caption = text(source.get("caption"));
            int score = LookKnowledgeService.rankScore(
                    LookKnowledgeService.matchScore(prompt, name, caption), text(source.get("view_angle")));
            if (score < 3 || score <= bestScore) {
                continue;
            }
            String key = text(source.get("storage_key"));
            if (key.isEmpty()) {
                continue;
            }
            bestScore = score;
            best = new LookHit(text(source.get("material_id")), key, text(source.get("mime_type")), caption);
        }
        return best;
    }

    private ElasticsearchClient client() {
        return elasticsearch == null ? null : elasticsearch.client();
    }

    private String indexName() {
        return elasticsearch.properties().lookIndex();
    }

    private void ensureIndex(ElasticsearchClient client) throws Exception {
        if (indexReady) {
            return;
        }
        String name = indexName();
        boolean exists = client.indices().exists(request -> request.index(name)).value();
        if (!exists) {
            client.indices().create(request -> request.index(name).withJson(new StringReader(MAPPING)));
        }
        indexReady = true;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
