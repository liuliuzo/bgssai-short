package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.mapper.ShortComplianceHitMapper;
import com.bgssai.shortai.mapper.ShortScriptMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortComplianceHit;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortScript;
import com.bgssai.shortai.model.ShortScriptExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.production.dto.ProductionDtos.PrecheckReq;
import com.bgssai.shortai.production.support.ComplianceLexicon;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class ComplianceService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ShortScriptMapper shortScriptMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortComplianceHitMapper shortComplianceHitMapper;

    public ApiResponse precheck(PrecheckReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        String scope = req.getCheckScope() == null ? "SCRIPT" : req.getCheckScope();
        List<Map<String, Object>> parts = new ArrayList<>();
        if ("SCRIPT".equals(scope) || "FILM".equals(scope)) {
            ShortScriptExample sx = new ShortScriptExample();
            sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId());
            ShortScript script = shortScriptMapper.selectOneByExample(sx);
            if (script != null) {
                for (Map<String, Object> scene : ShortJson.list(script.getSceneJson())) {
                    int sceneNo = ShortJson.integer(scene.get("scene_no"), 0);
                    Object raw = scene.get("line_list");
                    if (!(raw instanceof List<?>)) {
                        continue;
                    }
                    for (Object item : (List<?>) raw) {
                        if (item instanceof Map<?, ?>) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> line = (Map<String, Object>) item;
                            parts.add(ComplianceLexicon.scan(ShortJson.text(line.get("line_text")), sceneNo,
                                    ShortJson.integer(line.get("line_no"), 0), null));
                        }
                    }
                }
            }
        }
        if ("SHOT".equals(scope) || "FILM".equals(scope)) {
            ShortShotExample ex = new ShortShotExample();
            ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
            for (ShortShot shot : shortShotMapper.selectByExample(ex)) {
                parts.add(ComplianceLexicon.scan(
                        (shot.getDialogueText() == null ? "" : shot.getDialogueText()) + " "
                                + (shot.getImagePrompt() == null ? "" : shot.getImagePrompt()),
                        shot.getSceneNo(), null, shot.getShotNo()));
            }
        }
        Map<String, Object> merged = ComplianceLexicon.merge(parts);
        persistHits(ep, scope, merged);
        return Responses.ok(merged);
    }

    @SuppressWarnings("unchecked")
    private void persistHits(ShortEpisode ep, String scope, Map<String, Object> merged) {
        Object raw = merged.get("hit_list");
        if (!(raw instanceof List<?>)) {
            return;
        }
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> hit = (Map<String, Object>) item;
            ShortComplianceHit row = new ShortComplianceHit();
            row.setHitId(ShortIds.id("h-"));
            row.setWorkId(ep.getWorkId());
            row.setEpisodeId(ep.getEpisodeId());
            row.setCheckScope(scope);
            row.setRiskLevel(ShortJson.text(hit.get("risk_level")));
            row.setRiskCategory(ShortJson.text(hit.get("risk_category")));
            row.setHitText(ShortJson.text(hit.get("hit_text")));
            row.setSuggestion(ShortJson.text(hit.get("suggestion")));
            row.setSceneNo(ShortJson.integer(hit.get("scene_no"), 0));
            row.setLineNo(hit.get("line_no") == null ? null : ShortJson.integer(hit.get("line_no"), 0));
            row.setShotNo(hit.get("shot_no") == null ? null : ShortJson.integer(hit.get("shot_no"), 0));
            row.setCreatedTime(new Date());
            shortComplianceHitMapper.insertSelective(row);
        }
    }
}
