package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortScriptMapper;
import com.bgssai.shortai.mapper.ShortScriptVersionMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;
import com.bgssai.shortai.model.ShortScript;
import com.bgssai.shortai.model.ShortScriptExample;
import com.bgssai.shortai.model.ShortScriptVersion;
import com.bgssai.shortai.model.ShortScriptVersionExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.ProductionDtos.OutlineSaveReq;
import com.bgssai.shortai.production.dto.ProductionDtos.ScriptGenerateReq;
import com.bgssai.shortai.production.dto.ProductionDtos.ScriptSaveReq;
import com.bgssai.shortai.production.support.ComplianceLexicon;
import com.bgssai.shortai.production.support.RhythmChecker;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class ScriptService {

    private static final Logger log = LoggerFactory.getLogger(ScriptService.class);

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private WorkStatusService workStatusService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortScriptMapper shortScriptMapper;
    @Autowired
    private ShortScriptVersionMapper shortScriptVersionMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortSceneCardMapper shortSceneCardMapper;

    public ApiResponse generateOutline(ScriptGenerateReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("work_id", work.getWorkId());
        ShortAiTask task = aiTaskQueue.enqueue(work.getUserId(), work.getWorkId(), null, "TEXT", "OUTLINE",
                work.getWorkId(), request, null, 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    public ApiResponse getOutline(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("work_id", work.getWorkId());
        result.put("outline", ShortJson.list(work.getOutlineJson()));
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse saveOutline(OutlineSaveReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        List<Map<String, Object>> list = req.getEpisodeList() == null ? new ArrayList<>() : req.getEpisodeList();
        ShortWork patch = new ShortWork();
        patch.setOutlineJson(ShortJson.toJson(list));
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        applyOutline(work.getWorkId(), list);
        return Responses.ok("ok");
    }

    public ApiResponse generateScript(ScriptGenerateReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        ShortWork work = workAccessService.requireWork(ep.getWorkId());
        if (work.getOutlineJson() == null || work.getOutlineJson().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "outline is required before generating a script");
        }
        patchEpisodeScriptStatus(ep, "GENERATING");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("episode_id", ep.getEpisodeId());
        request.put("extra_prompt", req.getExtraPrompt());
        ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "TEXT", "SCRIPT",
                ep.getEpisodeId(), request, null, 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    public ApiResponse detail(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortScript script = findScript(ep.getEpisodeId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("script_status", ep.getScriptStatus());
        if (script == null) {
            result.put("script_version", 0);
            result.put("scene_list", new ArrayList<>());
            result.put("rhythm_check", new LinkedHashMap<>());
            result.put("total_score", 0);
            return Responses.ok(result);
        }
        result.put("script_version", script.getScriptVersion());
        result.put("scene_list", ShortJson.list(script.getSceneJson()));
        result.put("rhythm_check", ShortJson.map(script.getRhythmJson()));
        result.put("total_score", script.getTotalScore());
        result.put("estimated_total_sec", script.getEstimatedTotalSec());
        result.put("force_ready_flag", script.getForceReadyFlag());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse save(ScriptSaveReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        ShortScript script = requireScript(ep);
        if (req.getScriptVersion() != null && !req.getScriptVersion().equals(script.getScriptVersion())) {
            throw new BizException(CommonErrorCode.VERSION_CONFLICT, "script_version mismatch");
        }
        List<Map<String, Object>> scenes = req.getSceneList() == null ? new ArrayList<>() : req.getSceneList();
        persistScript(ep, script, scenes, "save", false);
        if ("READY".equals(ep.getScriptStatus()) && !typoOnly(script, scenes)) {
            patchEpisode(ep, "DRAFT", "DRAFT");
        }
        workStatusService.refresh(ep.getWorkId());
        return Responses.ok("ok");
    }

    public ApiResponse rewrite(ScriptGenerateReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("episode_id", ep.getEpisodeId());
        request.put("scene_no", req.getSceneNo());
        request.put("line_no", req.getLineNo());
        request.put("instruction", req.getInstruction());
        ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "TEXT", "REWRITE",
                ep.getEpisodeId(), request, null, 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse ready(ScriptGenerateReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        ShortScript script = requireScript(ep);
        List<Map<String, Object>> scenes = ShortJson.list(script.getSceneJson());
        Map<String, Object> precheck = scanScript(scenes);
        if ("BLOCK".equals(precheck.get("risk_level"))) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "compliance BLOCK, cannot ready");
        }
        int score = script.getTotalScore() == null ? 0 : script.getTotalScore();
        int force = req.getForceReadyFlag() == null ? 0 : req.getForceReadyFlag();
        if (score < 60 && force != 1) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "rhythm total_score < 60, set force_ready_flag=1");
        }
        ShortScript patch = new ShortScript();
        patch.setScriptStatus("READY");
        patch.setForceReadyFlag(force);
        patch.setUpdatedTime(new Date());
        ShortScriptExample up = new ShortScriptExample();
        up.createCriteria().andIdEqualTo(script.getId());
        shortScriptMapper.updateByExampleSelective(patch, up);
        patchEpisodeScriptStatus(ep, "READY");
        extractCards(ep, scenes);
        workStatusService.refresh(ep.getWorkId());
        log.info("script ready episodeId={} force={}", ep.getEpisodeId(), force);
        return Responses.ok("ok");
    }

    public ApiResponse versions(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortScript script = findScript(ep.getEpisodeId());
        List<Map<String, Object>> list = new ArrayList<>();
        if (script != null) {
            ShortScriptVersionExample ex = new ShortScriptVersionExample();
            ex.createCriteria().andScriptIdEqualTo(script.getScriptId());
            ex.setOrderByClause("script_version desc");
            List<ShortScriptVersion> rows = shortScriptVersionMapper.selectByExample(ex);
            for (ShortScriptVersion row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("script_version", row.getScriptVersion());
                item.put("change_summary", row.getChangeSummary());
                item.put("total_score", row.getTotalScore());
                item.put("created_time", row.getCreatedTime());
                list.add(item);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse rollback(ScriptGenerateReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        ShortScript script = requireScript(ep);
        if (req.getScriptVersion() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "script_version required");
        }
        ShortScriptVersionExample ex = new ShortScriptVersionExample();
        ex.createCriteria().andScriptIdEqualTo(script.getScriptId()).andScriptVersionEqualTo(req.getScriptVersion());
        ShortScriptVersion ver = shortScriptVersionMapper.selectOneByExample(ex);
        if (ver == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "version not found");
        }
        persistScript(ep, script, ShortJson.list(ver.getSceneJson()), "rollback", false);
        return Responses.ok("ok");
    }

    public void applyOutline(String workId, List<Map<String, Object>> list) {
        for (Map<String, Object> item : list) {
            int no = ShortJson.integer(item.get("episode_no"), 0);
            ShortEpisodeExample ex = new ShortEpisodeExample();
            ex.createCriteria().andWorkIdEqualTo(workId).andEpisodeNoEqualTo(no).andDeletedFlagEqualTo(0);
            ShortEpisode ep = shortEpisodeMapper.selectOneByExample(ex);
            if (ep == null) {
                continue;
            }
            ShortEpisode patch = new ShortEpisode();
            patch.setEpisodeTitle(ShortJson.text(item.get("episode_title")));
            patch.setEpisodeSynopsis(ShortJson.text(item.get("episode_synopsis")));
            patch.setEndingHook(ShortJson.text(item.get("ending_hook")));
            patch.setUpdatedTime(new Date());
            ShortEpisodeExample up = new ShortEpisodeExample();
            up.createCriteria().andIdEqualTo(ep.getId());
            shortEpisodeMapper.updateByExampleSelective(patch, up);
        }
    }

    public void persistGeneratedScript(ShortEpisode ep, List<Map<String, Object>> scenes) {
        ShortScript script = findScript(ep.getEpisodeId());
        if (script == null) {
            script = new ShortScript();
            script.setScriptId(ShortIds.id("s-"));
            script.setEpisodeId(ep.getEpisodeId());
            script.setWorkId(ep.getWorkId());
            script.setUserId(ep.getUserId());
            script.setScriptVersion(0);
            script.setScriptStatus("DRAFT");
            script.setForceReadyFlag(0);
            script.setCreatedTime(new Date());
            shortScriptMapper.insertSelective(script);
            script = findScript(ep.getEpisodeId());
        }
        persistScript(ep, script, scenes, "generate", true);
        patchEpisodeScriptStatus(ep, "DRAFT");
        workStatusService.refresh(ep.getWorkId());
    }

    public void persistRewrite(ShortEpisode ep, Integer sceneNo, Integer lineNo, String instruction) {
        persistRewrite(ep, sceneNo, lineNo, instruction, null);
    }

    public void persistRewrite(ShortEpisode ep, Integer sceneNo, Integer lineNo, String instruction,
            java.util.function.BiFunction<String, String, String> rewriter) {
        ShortScript script = requireScript(ep);
        List<Map<String, Object>> scenes = ShortJson.list(script.getSceneJson());
        for (Map<String, Object> scene : scenes) {
            if (sceneNo != null && !sceneNo.equals(ShortJson.integer(scene.get("scene_no"), -1))) {
                continue;
            }
            Object raw = scene.get("line_list");
            if (!(raw instanceof List<?>)) {
                continue;
            }
            List<Map<String, Object>> lines = new ArrayList<>();
            for (Object item : (List<?>) raw) {
                if (item instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> line = (Map<String, Object>) item;
                    if (lineNo == null || lineNo.equals(ShortJson.integer(line.get("line_no"), -1))) {
                        String original = ShortJson.text(line.get("line_text"));
                        if (rewriter != null) {
                            line.put("line_text", rewriter.apply(original, instruction));
                        } else {
                            line.put("line_text", com.bgssai.shortai.production.support.MockAiProvider.rewriteLine(
                                    original, instruction));
                        }
                    }
                    lines.add(line);
                }
            }
            scene.put("line_list", lines);
        }
        persistScript(ep, script, scenes, "rewrite", true);
    }

    private void persistScript(ShortEpisode ep, ShortScript script, List<Map<String, Object>> scenes,
            String summary, boolean bump) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(ep.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        int duration = work == null || work.getEpisodeDurationSec() == null ? 90 : work.getEpisodeDurationSec();
        Map<String, Object> rhythm = RhythmChecker.check(scenes, duration);
        int next = (script.getScriptVersion() == null ? 0 : script.getScriptVersion()) + (bump ? 1 : 0);
        if (!bump) {
            next = script.getScriptVersion() == null ? 1 : script.getScriptVersion() + 1;
        }
        ShortScript patch = new ShortScript();
        patch.setSceneJson(ShortJson.toJson(scenes));
        patch.setRhythmJson(ShortJson.toJson(rhythm));
        patch.setTotalScore(ShortJson.integer(rhythm.get("total_score"), 0));
        patch.setEstimatedTotalSec(ShortJson.integer(rhythm.get("estimated_total_sec"), 0));
        patch.setScriptVersion(next);
        patch.setScriptStatus("DRAFT");
        patch.setUpdatedTime(new Date());
        ShortScriptExample up = new ShortScriptExample();
        up.createCriteria().andIdEqualTo(script.getId());
        shortScriptMapper.updateByExampleSelective(patch, up);
        snapshot(script.getScriptId(), next, scenes, patch.getTotalScore(), summary);
        trimVersions(script.getScriptId());
        script.setScriptVersion(next);
    }

    private void snapshot(String scriptId, int version, List<Map<String, Object>> scenes, Integer score, String summary) {
        ShortScriptVersion row = new ShortScriptVersion();
        row.setVersionId(ShortIds.id("sv-"));
        row.setScriptId(scriptId);
        row.setScriptVersion(version);
        row.setSceneJson(ShortJson.toJson(scenes));
        row.setTotalScore(score);
        row.setChangeSummary(summary);
        row.setCreatedTime(new Date());
        shortScriptVersionMapper.insertSelective(row);
    }

    private void trimVersions(String scriptId) {
        ShortScriptVersionExample ex = new ShortScriptVersionExample();
        ex.createCriteria().andScriptIdEqualTo(scriptId);
        ex.setOrderByClause("script_version desc");
        List<ShortScriptVersion> rows = shortScriptVersionMapper.selectByExample(ex);
        for (int i = 20; i < rows.size(); i++) {
            ShortScriptVersionExample del = new ShortScriptVersionExample();
            del.createCriteria().andIdEqualTo(rows.get(i).getId());
            ShortScriptVersion patch = new ShortScriptVersion();
            patch.setChangeSummary("trimmed");
            shortScriptVersionMapper.updateByExampleSelective(patch, del);
        }
    }

    private void extractCards(ShortEpisode ep, List<Map<String, Object>> scenes) {
        for (Map<String, Object> scene : scenes) {
            String location = ShortJson.text(scene.get("scene_location"));
            if (!location.isBlank()) {
                ensureScene(ep, location);
            }
            Object raw = scene.get("line_list");
            if (!(raw instanceof List<?>)) {
                continue;
            }
            for (Object item : (List<?>) raw) {
                if (item instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> line = (Map<String, Object>) item;
                    String name = ShortJson.text(line.get("character_name"));
                    if (!name.isBlank()) {
                        ensureCharacter(ep, name);
                    }
                }
            }
        }
    }

    private void ensureCharacter(ShortEpisode ep, String name) {
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andWorkIdEqualTo(ep.getWorkId()).andCharacterNameEqualTo(name).andDeletedFlagEqualTo(0);
        if (shortCharacterMapper.selectOneByExample(ex) != null) {
            return;
        }
        ShortCharacter row = new ShortCharacter();
        row.setCharacterId(ShortIds.id("c-"));
        row.setWorkId(ep.getWorkId());
        row.setUserId(ep.getUserId());
        row.setCharacterName(name);
        row.setCardStatus("CANDIDATE");
        row.setAppearancePrompt(name + "，短剧角色，都市写实");
        row.setConsistencySeed(System.nanoTime() % 100000000L);
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        shortCharacterMapper.insertSelective(row);
    }

    private void ensureScene(ShortEpisode ep, String name) {
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andWorkIdEqualTo(ep.getWorkId()).andCardNameEqualTo(name).andDeletedFlagEqualTo(0);
        if (shortSceneCardMapper.selectOneByExample(ex) != null) {
            return;
        }
        ShortSceneCard row = new ShortSceneCard();
        row.setSceneCardId(ShortIds.id("sc-"));
        row.setWorkId(ep.getWorkId());
        row.setUserId(ep.getUserId());
        row.setCardType("SCENE");
        row.setCardName(name);
        row.setCardStatus("CANDIDATE");
        row.setAppearancePrompt(name);
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        shortSceneCardMapper.insertSelective(row);
    }

    private Map<String, Object> scanScript(List<Map<String, Object>> scenes) {
        List<Map<String, Object>> parts = new ArrayList<>();
        for (Map<String, Object> scene : scenes) {
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
        return ComplianceLexicon.merge(parts);
    }

    private boolean typoOnly(ShortScript oldScript, List<Map<String, Object>> newScenes) {
        List<Map<String, Object>> oldScenes = ShortJson.list(oldScript.getSceneJson());
        if (oldScenes.size() != newScenes.size()) {
            return false;
        }
        for (int i = 0; i < oldScenes.size(); i++) {
            Object oldLines = oldScenes.get(i).get("line_list");
            Object newLines = newScenes.get(i).get("line_list");
            if (!(oldLines instanceof List<?>) || !(newLines instanceof List<?>)) {
                return false;
            }
            if (((List<?>) oldLines).size() != ((List<?>) newLines).size()) {
                return false;
            }
        }
        return true;
    }

    private ShortScript findScript(String episodeId) {
        ShortScriptExample ex = new ShortScriptExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId);
        return shortScriptMapper.selectOneByExample(ex);
    }

    private ShortScript requireScript(ShortEpisode ep) {
        ShortScript script = findScript(ep.getEpisodeId());
        if (script == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "script not found");
        }
        return script;
    }

    private void patchEpisodeScriptStatus(ShortEpisode ep, String status) {
        ShortEpisode patch = new ShortEpisode();
        patch.setScriptStatus(status);
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
    }

    private void patchEpisode(ShortEpisode ep, String scriptStatus, String storyboardStatus) {
        ShortEpisode patch = new ShortEpisode();
        patch.setScriptStatus(scriptStatus);
        patch.setStoryboardStatus(storyboardStatus);
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
    }
}
