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
import com.bgssai.shortai.mapper.ShortContinuityIssueMapper;
import com.bgssai.shortai.mapper.ShortPromptVersionMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortShotPrevizMapper;
import com.bgssai.shortai.model.ShortContinuityIssue;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortPromptVersion;
import com.bgssai.shortai.model.ShortPromptVersionExample;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortShotPreviz;
import com.bgssai.shortai.model.ShortShotPrevizExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.FeatureDtos.PrevizGenerateReq;
import com.bgssai.shortai.production.dto.FeatureDtos.PrevizPromptReq;
import com.bgssai.shortai.production.dto.FeatureDtos.PrevizSaveReq;
import com.bgssai.shortai.production.support.PromptComposer;
import com.bgssai.shortai.production.support.ShortIds;

@Service
public class PrevizService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortSceneCardMapper shortSceneCardMapper;
    @Autowired
    private ShortShotPrevizMapper shortShotPrevizMapper;
    @Autowired
    private ShortPromptVersionMapper shortPromptVersionMapper;
    @Autowired
    private ShortContinuityIssueMapper shortContinuityIssueMapper;

    public ApiResponse list(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        List<ShortShot> shots = listShots(ep.getEpisodeId());
        ShortShotPrevizExample px = new ShortShotPrevizExample();
        px.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        Map<String, ShortShotPreviz> byShot = new LinkedHashMap<>();
        for (ShortShotPreviz p : shortShotPrevizMapper.selectByExample(px)) {
            byShot.put(p.getShotId(), p);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortShot shot : shots) {
            ShortShotPreviz p = byShot.get(shot.getShotId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("shot_id", shot.getShotId());
            item.put("shot_no", shot.getShotNo());
            item.put("previz_level", shot.getPrevizLevel() == null ? "NONE" : shot.getPrevizLevel());
            item.put("previz_text", p == null ? shot.getPrevizText() : p.getPrevizText());
            item.put("look_plain", PromptComposer.lookPlain(shot));
            item.put("draft_asset_id", p == null ? null : p.getDraftAssetId());
            item.put("image_prompt", shot.getImagePrompt());
            item.put("prompt_version", shot.getPromptVersion());
            item.put("stale_flag", shot.getStaleFlag());
            Map<String, Object> check = PromptComposer.check(shot, shot.getImagePrompt());
            item.put("prompt_score", check.get("prompt_score"));
            item.put("prompt_pass_flag", check.get("pass_flag"));
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("previz_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse generateText(PrevizGenerateReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        List<ShortShot> shots = filterShots(ep.getEpisodeId(), req.getShotIdList());
        Map<String, String> sceneNames = sceneNamesByWork(ep.getWorkId());
        List<Map<String, Object>> updated = new ArrayList<>();
        ShortShot prev = null;
        for (ShortShot shot : shots) {
            String text = PromptComposer.previzText(shot, sceneName(sceneNames, shot));
            String prompt = PromptComposer.compose(shot, null);
            upsertPreviz(shot, "TEXT", text, null);
            patchShotLevel(shot, "TEXT", text, prompt);
            if (prev != null && obviousMismatch(prev, shot)) {
                insertContinuity(ep, shot, prev, "PROP", "MAJOR",
                        "相邻镜头道具/场景描述可能不一致");
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("shot_id", shot.getShotId());
            item.put("previz_level", "TEXT");
            item.put("previz_text", text);
            updated.add(item);
            prev = shot;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated_list", updated);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse generateDraft(PrevizGenerateReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        ShortWork work = workAccessService.requireWork(ep.getWorkId());
        List<ShortShot> shots = filterShots(ep.getEpisodeId(), req.getShotIdList());
        Map<String, String> sceneNames = sceneNamesByWork(ep.getWorkId());
        boolean skip = Integer.valueOf(1).equals(req.getSkipLevelCheckFlag())
                || Integer.valueOf(1).equals(work.getSkipLevelCheckFlag());
        List<Map<String, Object>> updated = new ArrayList<>();
        for (ShortShot shot : shots) {
            String level = shot.getPrevizLevel() == null ? "NONE" : shot.getPrevizLevel();
            if (!skip && "NONE".equals(level)) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "need TEXT previz before DRAFT_IMAGE");
            }
            String text = shot.getPrevizText();
            if (text == null || text.isBlank()) {
                text = PromptComposer.previzText(shot, sceneName(sceneNames, shot));
            }
            String draftAssetId = ShortIds.id("a-draft-");
            upsertPreviz(shot, "DRAFT_IMAGE", text, draftAssetId);
            patchShotLevel(shot, "DRAFT_IMAGE", text, null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("shot_id", shot.getShotId());
            item.put("previz_level", "DRAFT_IMAGE");
            item.put("draft_asset_id", draftAssetId);
            item.put("previz_text", text);
            updated.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated_list", updated);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse save(PrevizSaveReq req) {
        if (req == null || req.getShotId() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id required");
        }
        ShortShot shot = requireShot(req.getShotId());
        String text = req.getPrevizText() == null ? shot.getPrevizText() : req.getPrevizText();
        if (text != null && !text.isBlank()) {
            shot.setShotDesc(stripPrevizLabels(text));
        }
        String prompt = req.getImagePrompt();
        if (prompt == null || prompt.isBlank()) {
            prompt = PromptComposer.compose(shot, null);
        }
        upsertPreviz(shot, shot.getPrevizLevel() == null || "NONE".equals(shot.getPrevizLevel()) ? "TEXT"
                : shot.getPrevizLevel(), text, null);
        Integer nextVer = (shot.getPromptVersion() == null ? 0 : shot.getPromptVersion()) + 1;
        ShortPromptVersion ver = new ShortPromptVersion();
        ver.setVersionId(ShortIds.id("pv-"));
        ver.setShotId(shot.getShotId());
        ver.setPromptVersion(nextVer);
        ver.setImagePrompt(prompt);
        ver.setChangeSummary("previz save");
        ver.setCreatedTime(new Date());
        shortPromptVersionMapper.insertSelective(ver);
        patchShotLevel(shot, shot.getPrevizLevel() == null || "NONE".equals(shot.getPrevizLevel()) ? "TEXT"
                : shot.getPrevizLevel(), text, prompt);
        ShortShot patch = new ShortShot();
        patch.setPromptVersion(nextVer);
        patch.setImagePrompt(prompt);
        patch.setStaleFlag(0);
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shot_id", shot.getShotId());
        result.put("previz_text", text);
        result.put("look_plain", PromptComposer.lookPlain(shot));
        result.put("image_prompt", prompt);
        result.put("prompt_version", nextVer);
        result.putAll(PromptComposer.check(shot, prompt));
        return Responses.ok(result);
    }

    public ApiResponse promptCheck(PrevizPromptReq req) {
        if (req == null || req.getShotId() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id required");
        }
        ShortShot shot = requireShot(req.getShotId());
        String prompt = req.getImagePrompt() == null ? shot.getImagePrompt() : req.getImagePrompt();
        Map<String, Object> result = PromptComposer.check(shot, prompt);
        result.put("shot_id", shot.getShotId());
        return Responses.ok(result);
    }

    public ApiResponse promptDiff(PrevizPromptReq req) {
        if (req == null || req.getShotId() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id required");
        }
        ShortShot shot = requireShot(req.getShotId());
        ShortPromptVersionExample ex = new ShortPromptVersionExample();
        ex.createCriteria().andShotIdEqualTo(shot.getShotId());
        ex.setOrderByClause("prompt_version asc");
        List<ShortPromptVersion> versions = shortPromptVersionMapper.selectByExample(ex);
        String from = "";
        String to = shot.getImagePrompt() == null ? "" : shot.getImagePrompt();
        int fromVer = req.getFromVersion() == null ? 0 : req.getFromVersion();
        int toVer = req.getToVersion() == null ? (shot.getPromptVersion() == null ? 0 : shot.getPromptVersion())
                : req.getToVersion();
        for (ShortPromptVersion v : versions) {
            if (v.getPromptVersion() != null && v.getPromptVersion() == fromVer) {
                from = v.getImagePrompt();
            }
            if (v.getPromptVersion() != null && v.getPromptVersion() == toVer) {
                to = v.getImagePrompt();
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shot_id", shot.getShotId());
        result.put("from_version", fromVer);
        result.put("to_version", toVer);
        result.put("from_prompt", from);
        result.put("to_prompt", to);
        boolean changed = from == null ? to != null && !to.isEmpty() : !from.equals(to);
        result.put("changed_flag", changed ? 1 : 0);
        return Responses.ok(result);
    }

    private Map<String, String> sceneNamesByWork(String workId) {
        Map<String, String> names = new LinkedHashMap<>();
        if (workId == null || workId.isBlank()) {
            return names;
        }
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        for (ShortSceneCard card : shortSceneCardMapper.selectByExample(ex)) {
            if (card.getSceneCardId() != null) {
                names.put(card.getSceneCardId(), card.getCardName() == null ? "" : card.getCardName());
            }
        }
        return names;
    }

    private static String sceneName(Map<String, String> names, ShortShot shot) {
        if (names == null || shot == null || shot.getSceneCardId() == null) {
            return "";
        }
        String name = names.get(shot.getSceneCardId());
        return name == null ? "" : name;
    }

    private void upsertPreviz(ShortShot shot, String level, String text, String draftAssetId) {
        ShortShotPrevizExample ex = new ShortShotPrevizExample();
        ex.createCriteria().andShotIdEqualTo(shot.getShotId()).andDeletedFlagEqualTo(0);
        ShortShotPreviz row = shortShotPrevizMapper.selectOneByExample(ex);
        if (row == null) {
            row = new ShortShotPreviz();
            row.setPrevizId(ShortIds.id("pz-"));
            row.setShotId(shot.getShotId());
            row.setEpisodeId(shot.getEpisodeId());
            row.setWorkId(shot.getWorkId());
            row.setUserId(shot.getUserId());
            row.setPrevizLevel(level);
            row.setPrevizText(text);
            row.setDraftAssetId(draftAssetId);
            row.setDeletedFlag(0);
            row.setCreatedTime(new Date());
            row.setUpdatedTime(new Date());
            shortShotPrevizMapper.insertSelective(row);
            return;
        }
        ShortShotPreviz patch = new ShortShotPreviz();
        patch.setPrevizLevel(level);
        patch.setPrevizText(text);
        if (draftAssetId != null) {
            patch.setDraftAssetId(draftAssetId);
        }
        patch.setUpdatedTime(new Date());
        ShortShotPrevizExample up = new ShortShotPrevizExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortShotPrevizMapper.updateByExampleSelective(patch, up);
    }

    private void patchShotLevel(ShortShot shot, String level, String text, String prompt) {
        ShortShot patch = new ShortShot();
        patch.setPrevizLevel(level);
        if (text != null) {
            patch.setPrevizText(text);
        }
        if (prompt != null) {
            patch.setImagePrompt(prompt);
        }
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
    }

    private void insertContinuity(ShortEpisode ep, ShortShot shot, ShortShot related, String type, String severity,
            String desc) {
        ShortContinuityIssue issue = new ShortContinuityIssue();
        issue.setIssueId(ShortIds.id("ci-"));
        issue.setEpisodeId(ep.getEpisodeId());
        issue.setWorkId(ep.getWorkId());
        issue.setIssueType(type);
        issue.setSeverity(severity);
        issue.setIssueDesc(desc);
        issue.setShotId(shot.getShotId());
        issue.setRelatedShotId(related.getShotId());
        issue.setHandleStatus("OPEN");
        issue.setDeletedFlag(0);
        issue.setCreatedTime(new Date());
        issue.setUpdatedTime(new Date());
        shortContinuityIssueMapper.insertSelective(issue);
    }

    private List<ShortShot> filterShots(String episodeId, List<String> shotIdList) {
        List<ShortShot> shots = listShots(episodeId);
        if (shotIdList == null || shotIdList.isEmpty()) {
            return shots;
        }
        return shots.stream().filter(s -> shotIdList.contains(s.getShotId())).toList();
    }

    private List<ShortShot> listShots(String episodeId) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("shot_no asc");
        return shortShotMapper.selectByExample(ex);
    }

    private ShortShot requireShot(String shotId) {
        String userId = workAccessService.requireUser();
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(shotId).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null || !userId.equals(shot.getUserId())) {
            throw new ForbiddenException("shot not found");
        }
        return shot;
    }

    private static String stripPrevizLabels(String text) {
        String t = text == null ? "" : text.trim();
        t = t.replace("谁：", "").replace("在哪：", " ").replace("做什么：", " ").replace("怎么拍：", " ")
                .replace("什么感觉：", " ").replace("台词：", " ");
        return t.replaceAll("\\s+", " ").trim();
    }

    private static boolean obviousMismatch(ShortShot prev, ShortShot cur) {
        String a = prev.getShotDesc() == null ? "" : prev.getShotDesc();
        String b = cur.getShotDesc() == null ? "" : cur.getShotDesc();
        return a.contains("室内") && b.contains("室外") || a.contains("室外") && b.contains("室内");
    }

}
