package com.bgssai.shortai.aimodel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.aiprovider.GatewayErrorText;
import com.bgssai.shortai.aiprovider.TokenHubClientSupport;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * Before a generation is queued, checks the text, image and voice models the
 * pipeline will call. A blocked step is reported immediately and nothing is queued.
 */
@Component
public class PipelineReadiness {

    private static final Logger log = LoggerFactory.getLogger(PipelineReadiness.class);
    private static final Set<String> GATED = Set.of("TEXT", "IMAGE", "VOICE", "VIDEO");
    private static final long READY_TTL_MS = 10 * 60 * 1000L;
    private static final long BLOCKED_TTL_MS = 30 * 1000L;

    private static final List<Step> STEPS = List.of(
            new Step("TEXT", "chat", "大纲、剧本、分镜"),
            new Step("IMAGE", "image_generation", "定妆、图片"),
            new Step("VOICE", "audio_speech", "配音"));

    private final AiModelResolveService models;
    private final SysSecretReader secrets;
    private final String baseUrl;
    private final GatewayClient client;
    private final TokenHubModelCatalog catalogCache;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    @Autowired
    public PipelineReadiness(AiModelResolveService models, SysSecretReader secrets,
            TokenHubModelCatalog catalogCache,
            @Value("${bgssai.llm.tokenhub-cn-base-url:${bgssai.llm.gateway-base-url:https://www.bgssai-tokenhub.cn}}")
                    String baseUrl) {
        this(models, secrets, baseUrl, new JdkGatewayClient(), catalogCache);
    }

    PipelineReadiness(AiModelResolveService models, SysSecretReader secrets, String baseUrl,
            GatewayClient client) {
        this(models, secrets, baseUrl, client, null);
    }

    private PipelineReadiness(AiModelResolveService models, SysSecretReader secrets, String baseUrl,
            GatewayClient client, TokenHubModelCatalog catalogCache) {
        this.models = models;
        this.secrets = secrets;
        this.baseUrl = trim(baseUrl);
        this.client = client;
        this.catalogCache = catalogCache;
    }

    public void assertReady(String taskType, String selectedModel) {
        if (taskType == null || !GATED.contains(taskType.trim().toUpperCase(Locale.ROOT))) {
            return;
        }
        String key = TokenHubClientSupport.resolveApiKey(secrets, TokenHubClientSupport.HUB_CN, null);
        if (key.isBlank()) {
            throw blocked("模型网关未配置，大纲、剧本、分镜、定妆、图片和配音现在都不能开始，请稍后重试");
        }
        Map<String, TokenHubModelCatalog.HubModel> catalog;
        try {
            catalog = loadCatalog(key);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw blocked("模型网关连接失败，大纲、剧本、分镜、定妆、图片和配音现在都不能开始，请稍后重试");
        } catch (Exception ex) {
            log.warn("gateway catalog unavailable");
            throw blocked("模型网关连接失败，大纲、剧本、分镜、定妆、图片和配音现在都不能开始，请稍后重试");
        }
        if (catalogCache != null && !catalog.isEmpty()) {
            catalogCache.remember(List.copyOf(catalog.values()));
        }
        String ability = taskType.trim().toUpperCase(Locale.ROOT);
        List<String> blocks = new ArrayList<>();
        for (Step step : STEPS) {
            String modelCode = modelCode(step, ability, selectedModel, catalog);
            String reason = reasonFor(step, modelCode, catalog, key);
            if (reason != null) {
                blocks.add(step.label + "（" + reason + "）");
            }
        }
        if (!blocks.isEmpty()) {
            throw blocked(format(blocks));
        }
    }

    private String modelCode(Step step, String taskAbility, String selectedModel,
            Map<String, TokenHubModelCatalog.HubModel> catalog) {
        String override = step.ability.equals(taskAbility) && selectedModel != null && !selectedModel.isBlank()
                ? selectedModel.trim() : null;
        String configured = "";
        try {
            AiModelConfig model = models.resolve(step.ability, override);
            configured = model == null || model.getModelCode() == null ? "" : model.getModelCode().trim();
        } catch (RuntimeException ex) {
            configured = override == null ? "" : override;
        }
        if (override != null) {
            return configured;
        }
        return CatalogModelAlign.choose(step.ability, configured, List.copyOf(catalog.values()));
    }

    private String reasonFor(Step step, String modelCode,
            Map<String, TokenHubModelCatalog.HubModel> catalog, String apiKey) {
        if (modelCode.isBlank()) {
            return "没有可用模型";
        }
        String cacheKey = baseUrl + "|" + step.ability + "|" + modelCode;
        Cached cached = cache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && cached.until > now) {
            return cached.reason;
        }
        String reason = probe(step, modelCode, catalog, apiKey);
        cache.put(cacheKey, new Cached(reason, now + (reason == null ? READY_TTL_MS : BLOCKED_TTL_MS)));
        return reason;
    }

    private String probe(Step step, String modelCode, Map<String, TokenHubModelCatalog.HubModel> catalog,
            String apiKey) {
        TokenHubModelCatalog.HubModel listed = catalog.get(modelCode);
        if (listed == null || listed.capabilities() == null
                || !listed.capabilities().contains(step.capability)) {
            return "该模型暂未开放：" + modelCode;
        }
        try {
            GatewayClient.Reply reply = client.post(baseUrl + probePath(step.ability), apiKey, probeBody(step, modelCode));
            if (GatewayErrorText.probeMeansReady(reply.status(), reply.body())) {
                return null;
            }
            GatewayErrorText.note(reply.status(), reply.body());
            String reason = GatewayErrorText.reason(reply.status(), reply.body());
            if ("该模型暂未开放".equals(reason)) {
                return reason + "：" + modelCode;
            }
            return reason;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "模型网关连接失败";
        } catch (Exception ex) {
            log.warn("gateway probe failed ability={} model={}", step.ability, modelCode);
            return "模型网关连接失败";
        }
    }

    private Map<String, TokenHubModelCatalog.HubModel> loadCatalog(String apiKey) throws Exception {
        GatewayClient.Reply reply = client.get(baseUrl + "/v1/models", apiKey);
        if (reply.status() < 200 || reply.status() >= 300) {
            GatewayErrorText.note(reply.status(), reply.body());
            throw new IllegalStateException("catalog status " + reply.status());
        }
        Map<String, TokenHubModelCatalog.HubModel> rows = new LinkedHashMap<>();
        for (TokenHubModelCatalog.HubModel row : TokenHubModelCatalog.parse(reply.body())) {
            rows.put(row.id(), row);
        }
        return rows;
    }

    private static String probePath(String ability) {
        return switch (ability) {
            case "IMAGE" -> "/v1/images/generations";
            case "VOICE" -> "/v1/audio/speech";
            default -> "/v1/chat/completions";
        };
    }

    /**
     * The probe body is intentionally incomplete ({@code n=0} for images, no prompt
     * for text or speech) so a configured provider fails validation instead of
     * starting a billed generation. A missing provider still answers
     * {@code provider_not_configured} before that validation.
     */
    private static String probeBody(Step step, String modelCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelCode);
        if ("IMAGE".equals(step.ability)) {
            body.put("prompt", ".");
            body.put("n", 0);
            body.put("size", "0x0");
        }
        return ShortJson.toJson(body);
    }

    private static String format(List<String> blocks) {
        if (blocks.size() == 1) {
            String one = blocks.get(0);
            int open = one.indexOf('（');
            int close = one.endsWith("）") ? one.length() - 1 : -1;
            if (open > 0 && close > open) {
                return one.substring(0, open) + "现在不能开始：" + one.substring(open + 1, close) + "，请稍后重试";
            }
        }
        return "以下步骤现在不能开始：" + String.join("；", blocks) + "。请稍后重试";
    }

    private static BizException blocked(String message) {
        return new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, message);
    }

    private static String trim(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record Step(String ability, String capability, String label) {
    }

    private record Cached(String reason, long until) {
    }

    interface GatewayClient {
        Reply get(String url, String apiKey) throws Exception;

        Reply post(String url, String apiKey, String json) throws Exception;

        record Reply(int status, String body) {
        }
    }

    private static final class JdkGatewayClient implements GatewayClient {
        private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

        @Override
        public Reply get(String url, String apiKey) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body() == null ? "" : response.body());
        }

        @Override
        public Reply post(String url, String apiKey, String json) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body() == null ? "" : response.body());
        }
    }
}
