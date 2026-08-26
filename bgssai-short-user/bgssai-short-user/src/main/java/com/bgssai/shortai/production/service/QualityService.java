package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortQualityGateMapper;
import com.bgssai.shortai.mapper.ShortQualityIssueMapper;
import com.bgssai.shortai.mapper.ShortQualityThresholdLogMapper;
import com.bgssai.shortai.mapper.ShortReferenceTemplateMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortVariantMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortDubbing;
import com.bgssai.shortai.model.ShortDubbingExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortQualityGate;
import com.bgssai.shortai.model.ShortQualityGateExample;
import com.bgssai.shortai.model.ShortQualityIssue;
import com.bgssai.shortai.model.ShortQualityIssueExample;
import com.bgssai.shortai.model.ShortQualityThresholdLog;
import com.bgssai.shortai.model.ShortQualityThresholdLogExample;
import com.bgssai.shortai.model.ShortReferenceTemplate;
import com.bgssai.shortai.model.ShortReferenceTemplateExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortVariant;
import com.bgssai.shortai.model.ShortVariantExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityForcePassReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityGateRunReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityIssueHandleReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityThresholdAdjustReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityVariantGenerateReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityVariantSelectReq;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class QualityService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ShortQualityGateMapper shortQualityGateMapper;
    @Autowired
    private ShortQualityIssueMapper shortQualityIssueMapper;
    @Autowired
    private ShortQualityThresholdLogMapper shortQualityThresholdLogMapper;
    @Autowired
    private ShortReferenceTemplateMapper shortReferenceTemplateMapper;
    @Autowired
    private ShortVariantMapper shortVariantMapper;
    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortDubbingMapper shortDubbingMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;

    public ApiResponse templates(String genre) {
        ShortReferenceTemplateExample ex = new ShortReferenceTemplateExample();
        ShortReferenceTemplateExample.Criteria c = ex.createCriteria().andDeletedFlagEqualTo(0)
                .andEnabledFlagEqualTo(1);
        if (genre != null && !genre.isBlank()) {
            c.andGenreEqualTo(genre);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortReferenceTemplate t : shortReferenceTemplateMapper.selectByExample(ex)) {
            list.add(toTemplate(t));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("template_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse gateRun(QualityGateRunReq req) {
        if (req == null || req.getGateCode() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "gate_code required");
        }
        ShortWork work;
        ShortEpisode ep = null;
        if (req.getEpisodeId() != null && !req.getEpisodeId().isBlank()) {
            ep = workAccessService.requireEpisode(req.getEpisodeId());
            work = workAccessService.requireWork(ep.getWorkId());
        } else {
            work = workAccessService.requireWork(req.getWorkId());
        }
        int threshold = work.getQualityThresholdScore() == null ? 75 : work.getQualityThresholdScore();
        if (ep != null && Integer.valueOf(1).equals(ep.getEpisodeNo())) {
            threshold = Math.max(threshold, 85);
        }
        List<Map<String, Object>> issues = scoreIssues(req.getGateCode(), work, ep);
        int score = Math.max(0, 100 - issues.size() * 8);
        String status = score >= threshold ? "PASSED" : "FAILED";
        ShortQualityGate gate = new ShortQualityGate();
        gate.setGateId(ShortIds.id("qg-"));
        gate.setWorkId(work.getWorkId());
        gate.setEpisodeId(ep == null ? null : ep.getEpisodeId());
        gate.setUserId(work.getUserId());
        gate.setGateCode(req.getGateCode());
        gate.setGateStatus(status);
        gate.setScore(score);
        gate.setThresholdScore(threshold);
        gate.setForcePassFlag(0);
        Map<String, Object> resultJson = new LinkedHashMap<>();
        resultJson.put("score", score);
        resultJson.put("threshold", threshold);
        resultJson.put("issue_count", issues.size());
        gate.setResultJson(ShortJson.toJson(resultJson));
        gate.setDeletedFlag(0);
        gate.setCreatedTime(new Date());
        gate.setUpdatedTime(new Date());
        shortQualityGateMapper.insertSelective(gate);
        List<Map<String, Object>> issueOut = new ArrayList<>();
        for (Map<String, Object> issue : issues) {
            ShortQualityIssue row = new ShortQualityIssue();
            row.setIssueId(ShortIds.id("qi-"));
            row.setGateId(gate.getGateId());
            row.setEpisodeId(ep == null ? null : ep.getEpisodeId());
            row.setDimension(ShortJson.text(issue.get("dimension")));
            row.setIssueDesc(ShortJson.text(issue.get("issue_desc")));
            row.setFixHint(ShortJson.text(issue.get("fix_hint")));
            row.setTargetType(ShortJson.text(issue.get("target_type")));
            row.setTargetId(ShortJson.text(issue.get("target_id")));
            row.setHandleStatus("OPEN");
            row.setDeletedFlag(0);
            row.setCreatedTime(new Date());
            row.setUpdatedTime(new Date());
            shortQualityIssueMapper.insertSelective(row);
            Map<String, Object> out = new LinkedHashMap<>(issue);
            out.put("issue_id", row.getIssueId());
            out.put("handle_status", "OPEN");
            issueOut.add(out);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gate_id", gate.getGateId());
        result.put("gate_code", gate.getGateCode());
        result.put("gate_status", status);
        result.put("score", score);
        result.put("threshold_score", threshold);
        result.put("issue_list", issueOut);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse forcePass(QualityForcePassReq req) {
        ShortQualityGate gate = requireGate(req == null ? null : req.getGateId());
        if (req.getForcePassReason() == null || req.getForcePassReason().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "force_pass_reason required");
        }
        ShortQualityGate patch = new ShortQualityGate();
        patch.setForcePassFlag(1);
        patch.setForcePassReason(req.getForcePassReason());
        patch.setGateStatus("FORCE_PASSED");
        patch.setUpdatedTime(new Date());
        ShortQualityGateExample up = new ShortQualityGateExample();
        up.createCriteria().andIdEqualTo(gate.getId());
        shortQualityGateMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse issueHandle(QualityIssueHandleReq req) {
        if (req == null || req.getIssueId() == null || req.getHandleStatus() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "issue_id and handle_status required");
        }
        String userId = workAccessService.requireUser();
        ShortQualityIssueExample ex = new ShortQualityIssueExample();
        ex.createCriteria().andIssueIdEqualTo(req.getIssueId()).andDeletedFlagEqualTo(0);
        ShortQualityIssue row = shortQualityIssueMapper.selectOneByExample(ex);
        if (row == null) {
            throw new ForbiddenException("issue not found");
        }
        ShortQualityGate gate = requireGate(row.getGateId());
        if (!userId.equals(gate.getUserId())) {
            throw new ForbiddenException("issue not found");
        }
        ShortQualityIssue patch = new ShortQualityIssue();
        patch.setHandleStatus(req.getHandleStatus());
        patch.setUpdatedTime(new Date());
        ShortQualityIssueExample up = new ShortQualityIssueExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortQualityIssueMapper.updateByExampleSelective(patch, up);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("issue_id", row.getIssueId());
        result.put("handle_status", req.getHandleStatus());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse thresholdAdjust(QualityThresholdAdjustReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        if (req.getNewThresholdScore() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "new_threshold_score required");
        }
        int old = work.getQualityThresholdScore() == null ? 75 : work.getQualityThresholdScore();
        ShortWork patch = new ShortWork();
        patch.setQualityThresholdScore(req.getNewThresholdScore());
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        ShortQualityThresholdLog log = new ShortQualityThresholdLog();
        log.setLogId(ShortIds.id("ql-"));
        log.setWorkId(work.getWorkId());
        log.setUserId(work.getUserId());
        log.setOldThresholdScore(old);
        log.setNewThresholdScore(req.getNewThresholdScore());
        log.setAdjustReason(req.getAdjustReason() == null ? "manual" : req.getAdjustReason());
        log.setCreatedTime(new Date());
        shortQualityThresholdLogMapper.insertSelective(log);
        return Responses.ok(Map.of("new_threshold_score", req.getNewThresholdScore()));
    }

    @Transactional
    public ApiResponse variantsGenerate(QualityVariantGenerateReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        int count = req.getVariantCount() == null ? 3 : Math.min(5, Math.max(1, req.getVariantCount()));
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ShortVariant v = new ShortVariant();
            v.setVariantId(ShortIds.id("vv-"));
            v.setWorkId(work.getWorkId());
            v.setEpisodeId(req.getEpisodeId());
            v.setVariantNode(req.getVariantNode() == null ? "OPENING" : req.getVariantNode());
            v.setTargetId(req.getTargetId());
            v.setVariantNo(i);
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("title", "方案 " + i);
            content.put("summary", "自动生成变体 " + i);
            v.setContentJson(ShortJson.toJson(content));
            v.setScore(80 - i * 3);
            v.setSelectedFlag(0);
            v.setDeletedFlag(0);
            v.setCreatedTime(new Date());
            v.setUpdatedTime(new Date());
            shortVariantMapper.insertSelective(v);
            list.add(toVariant(v));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("variant_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse variantSelect(QualityVariantSelectReq req) {
        if (req == null || req.getVariantId() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "variant_id required");
        }
        String userId = workAccessService.requireUser();
        ShortVariantExample ex = new ShortVariantExample();
        ex.createCriteria().andVariantIdEqualTo(req.getVariantId()).andDeletedFlagEqualTo(0);
        ShortVariant row = shortVariantMapper.selectOneByExample(ex);
        if (row == null) {
            throw new ForbiddenException("variant not found");
        }
        ShortWork work = workAccessService.requireWork(row.getWorkId());
        if (!userId.equals(work.getUserId())) {
            throw new ForbiddenException("variant not found");
        }
        ShortVariantExample clearEx = new ShortVariantExample();
        clearEx.createCriteria().andWorkIdEqualTo(row.getWorkId()).andVariantNodeEqualTo(row.getVariantNode())
                .andDeletedFlagEqualTo(0);
        ShortVariant clear = new ShortVariant();
        clear.setSelectedFlag(0);
        clear.setUpdatedTime(new Date());
        shortVariantMapper.updateByExampleSelective(clear, clearEx);
        ShortVariant patch = new ShortVariant();
        patch.setSelectedFlag(1);
        patch.setUpdatedTime(new Date());
        ShortVariantExample up = new ShortVariantExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortVariantMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    public ApiResponse gateResult(String gateCode, String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortQualityGateExample ex = new ShortQualityGateExample();
        ShortQualityGateExample.Criteria c = ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId())
                .andDeletedFlagEqualTo(0);
        if (gateCode != null && !gateCode.isBlank()) {
            c.andGateCodeEqualTo(gateCode);
        }
        ex.setOrderByClause("id desc");
        ShortQualityGate gate = shortQualityGateMapper.selectOneByExample(ex);
        if (gate == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("gate_result", null);
            return Responses.ok(empty);
        }
        ShortQualityIssueExample ix = new ShortQualityIssueExample();
        ix.createCriteria().andGateIdEqualTo(gate.getGateId()).andDeletedFlagEqualTo(0);
        List<Map<String, Object>> issues = new ArrayList<>();
        for (ShortQualityIssue i : shortQualityIssueMapper.selectByExample(ix)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("issue_id", i.getIssueId());
            item.put("dimension", i.getDimension());
            item.put("issue_desc", i.getIssueDesc());
            item.put("fix_hint", i.getFixHint());
            item.put("target_type", i.getTargetType());
            item.put("target_id", i.getTargetId());
            item.put("handle_status", i.getHandleStatus());
            issues.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gate_id", gate.getGateId());
        result.put("gate_code", gate.getGateCode());
        result.put("gate_status", gate.getGateStatus());
        result.put("score", gate.getScore());
        result.put("threshold_score", gate.getThresholdScore());
        result.put("force_pass_flag", gate.getForcePassFlag());
        result.put("issue_list", issues);
        return Responses.ok(result);
    }

    public ApiResponse thresholdLogs(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortQualityThresholdLogExample ex = new ShortQualityThresholdLogExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId());
        ex.setOrderByClause("id desc");
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortQualityThresholdLog log : shortQualityThresholdLogMapper.selectByExample(ex)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("log_id", log.getLogId());
            item.put("old_threshold_score", log.getOldThresholdScore());
            item.put("new_threshold_score", log.getNewThresholdScore());
            item.put("adjust_reason", log.getAdjustReason());
            item.put("created_time", log.getCreatedTime());
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("log_list", list);
        return Responses.ok(result);
    }

    public ApiResponse variants(String variantNode, String targetId) {
        workAccessService.requireUser();
        ShortVariantExample ex = new ShortVariantExample();
        ShortVariantExample.Criteria c = ex.createCriteria().andDeletedFlagEqualTo(0);
        if (variantNode != null && !variantNode.isBlank()) {
            c.andVariantNodeEqualTo(variantNode);
        }
        if (targetId != null && !targetId.isBlank()) {
            c.andTargetIdEqualTo(targetId);
        }
        ex.setOrderByClause("variant_no asc");
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortVariant v : shortVariantMapper.selectByExample(ex)) {
            ShortWork work = workAccessService.requireWork(v.getWorkId());
            if (work != null) {
                list.add(toVariant(v));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("variant_list", list);
        return Responses.ok(result);
    }

    public ApiResponse profile(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortQualityGateExample ex = new ShortQualityGateExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        List<Map<String, Object>> gates = new ArrayList<>();
        for (ShortQualityGate g : shortQualityGateMapper.selectByExample(ex)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gate_id", g.getGateId());
            item.put("episode_id", g.getEpisodeId());
            item.put("gate_code", g.getGateCode());
            item.put("gate_status", g.getGateStatus());
            item.put("score", g.getScore());
            item.put("force_pass_flag", g.getForcePassFlag());
            gates.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("work_id", work.getWorkId());
        result.put("overall_quality_score", work.getOverallQualityScore());
        result.put("quality_threshold_score", work.getQualityThresholdScore());
        result.put("template_id", work.getTemplateId());
        result.put("gate_list", gates);
        return Responses.ok(result);
    }

    private List<Map<String, Object>> scoreIssues(String gateCode, ShortWork work, ShortEpisode ep) {
        List<Map<String, Object>> issues = new ArrayList<>();
        if ("G1_SCRIPT".equals(gateCode)) {
            if (ep != null && !"READY".equals(ep.getScriptStatus())) {
                issues.add(issue("RHYTHM", "剧本尚未定稿", "先定稿再过剧本门", "EPISODE", ep.getEpisodeId()));
            }
            if (work.getTemplateId() == null || work.getTemplateId().isBlank()) {
                issues.add(issue("BEAT", "未选用结构模板", "为作品绑定参考模板", "WORK", work.getWorkId()));
            }
        } else if ("G2_STORYBOARD".equals(gateCode)) {
            if (ep != null) {
                ShortShotExample sx = new ShortShotExample();
                sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
                sx.setOrderByClause("shot_no asc");
                List<ShortShot> shots = shortShotMapper.selectByExample(sx);
                String lastSize = null;
                int same = 0;
                for (ShortShot s : shots) {
                    if (s.getShotSize() != null && s.getShotSize().equals(lastSize)) {
                        same++;
                        if (same >= 2) {
                            issues.add(issue("SHOT_SIZE", "连续同景别", "调整景别分布", "SHOT", s.getShotId()));
                            break;
                        }
                    } else {
                        same = 0;
                    }
                    lastSize = s.getShotSize();
                }
            }
        } else if ("G3_VISUAL".equals(gateCode)) {
            if (ep != null) {
                ShortShotExample sx = new ShortShotExample();
                sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
                for (ShortShot s : shortShotMapper.selectByExample(sx)) {
                    if (s.getFirstFrameAssetId() == null || s.getFirstFrameAssetId().isBlank()) {
                        issues.add(issue("FRAME", "缺少首帧", "生成或锁定首帧", "SHOT", s.getShotId()));
                        if (issues.size() >= 5) {
                            break;
                        }
                    }
                }
            }
        } else if ("G4_FILM".equals(gateCode)) {
            if (ep != null) {
                ShortDubbingExample dx = new ShortDubbingExample();
                dx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0)
                        .andAlignStatusEqualTo("MISALIGNED");
                for (ShortDubbing d : shortDubbingMapper.selectByExample(dx)) {
                    issues.add(issue("ALIGN", "配音未对齐", "处理对齐后再过成片门", "DUBBING", d.getDubbingId()));
                }
                if (!"READY".equals(ep.getComposeStatus())) {
                    issues.add(issue("COMPOSE", "成片未就绪", "完成合成", "EPISODE", ep.getEpisodeId()));
                }
            }
        } else {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "unknown gate_code");
        }
        return issues;
    }

    private ShortQualityGate requireGate(String gateId) {
        if (gateId == null || gateId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "gate_id required");
        }
        String userId = workAccessService.requireUser();
        ShortQualityGateExample ex = new ShortQualityGateExample();
        ex.createCriteria().andGateIdEqualTo(gateId).andDeletedFlagEqualTo(0);
        ShortQualityGate gate = shortQualityGateMapper.selectOneByExample(ex);
        if (gate == null || !userId.equals(gate.getUserId())) {
            throw new ForbiddenException("gate not found");
        }
        return gate;
    }

    private static Map<String, Object> issue(String dimension, String desc, String hint, String targetType,
            String targetId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dimension", dimension);
        m.put("issue_desc", desc);
        m.put("fix_hint", hint);
        m.put("target_type", targetType);
        m.put("target_id", targetId);
        m.put("severity", "MAJOR");
        return m;
    }

    private static Map<String, Object> toTemplate(ShortReferenceTemplate t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("template_id", t.getTemplateId());
        m.put("template_name", t.getTemplateName());
        m.put("genre", t.getGenre());
        m.put("hook_type_list", ShortJson.list(t.getHookTypeJson()));
        m.put("beat_sheet_json", ShortJson.map(t.getBeatSheetJson()).isEmpty()
                ? ShortJson.list(t.getBeatSheetJson()) : ShortJson.map(t.getBeatSheetJson()));
        m.put("turning_point_json", ShortJson.map(t.getTurningPointJson()));
        m.put("taboo_list", ShortJson.list(t.getTabooListJson()));
        m.put("enabled_flag", t.getEnabledFlag());
        return m;
    }

    private static Map<String, Object> toVariant(ShortVariant v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("variant_id", v.getVariantId());
        m.put("variant_node", v.getVariantNode());
        m.put("target_id", v.getTargetId());
        m.put("variant_no", v.getVariantNo());
        m.put("content", ShortJson.map(v.getContentJson()));
        m.put("score", v.getScore());
        m.put("selected_flag", v.getSelectedFlag());
        return m;
    }
}
