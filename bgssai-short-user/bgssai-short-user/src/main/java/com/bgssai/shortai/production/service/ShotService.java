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
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortScriptMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;
import com.bgssai.shortai.model.ShortScript;
import com.bgssai.shortai.model.ShortScriptExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.ProductionDtos.ShotReq;
import com.bgssai.shortai.production.support.GenerationTaskStatus;
import com.bgssai.shortai.production.support.PromptComposer;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.production.support.ShotConsistency;
import com.github.pagehelper.PageHelper;

@Service
public class ShotService {

    @Autowired
    private AssetService assetService;
    @Autowired
    private FrameLinkService frameLinkService;

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private WorkStatusService workStatusService;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortScriptMapper shortScriptMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortSceneCardMapper shortSceneCardMapper;
    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired(required = false)
    private OfficialVideoQueue officialVideoQueue;

    @Transactional
    public ApiResponse split(ShotReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        if (!"READY".equals(ep.getScriptStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "请先在「剧本与定稿」页完成本集定稿，再拆解分镜");
        }
        ShortScriptExample sx = new ShortScriptExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId());
        ShortScript script = shortScriptMapper.selectOneByExample(sx);
        if (script == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "未找到本集剧本，请先生成本集剧本");
        }
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(ep.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        String style = work == null ? "MODERN_URBAN" : work.getVisualStyle();
        ShortShotExample keep = new ShortShotExample();
        keep.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andShotStatusNotEqualTo("LOCKED").andDeletedFlagEqualTo(0);
        List<ShortShot> unlocked = shortShotMapper.selectByExample(keep);
        for (ShortShot shot : unlocked) {
            ShortShot patch = new ShortShot();
            patch.setDeletedFlag(1);
            patch.setUpdatedTime(new Date());
            ShortShotExample up = new ShortShotExample();
            up.createCriteria().andIdEqualTo(shot.getId());
            shortShotMapper.updateByExampleSelective(patch, up);
        }
        int shotNo = maxLockedNo(ep.getEpisodeId());
        List<Map<String, Object>> scenes = ShortJson.list(script.getSceneJson());
        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andWorkIdEqualTo(ep.getWorkId()).andDeletedFlagEqualTo(0);
        List<ShortCharacter> characters = shortCharacterMapper.selectByExample(cx);
        ShortSceneCardExample scx = new ShortSceneCardExample();
        scx.createCriteria().andWorkIdEqualTo(ep.getWorkId()).andDeletedFlagEqualTo(0);
        List<ShortSceneCard> sceneCards = shortSceneCardMapper.selectByExample(scx);
        for (Map<String, Object> scene : scenes) {
            int sceneNo = ShortJson.integer(scene.get("scene_no"), 1);
            Object raw = scene.get("line_list");
            if (!(raw instanceof List<?>)) {
                continue;
            }
            for (Object item : (List<?>) raw) {
                if (!(item instanceof Map<?, ?>)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> line = (Map<String, Object>) item;
                shotNo++;
                insertShot(ep, shotNo, sceneNo, scene, line, style, characters, sceneCards);
            }
        }
        patchStoryboard(ep, "DRAFT");
        workStatusService.refresh(ep.getWorkId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shot_count", shotNo);
        return Responses.ok(result);
    }

    public PageResponse list(String episodeId, int pageNum, int pageSize, String shotStatus) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 50 : pageSize;
        ShortShotExample ex = new ShortShotExample();
        ShortShotExample.Criteria c = ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        if (shotStatus != null && !shotStatus.isBlank()) {
            c.andShotStatusEqualTo(shotStatus.trim());
        }
        ex.setOrderByClause("shot_no asc");
        PageHelper.startPage(safePageNum, safePageSize);
        List<ShortShot> rows = shortShotMapper.selectByExample(ex);
        long total = rows instanceof com.github.pagehelper.Page<?> page ? page.getTotal() : rows.size();
        List<ShortCharacter> characters = loadWorkCharacters(ep.getWorkId());
        List<ShortSceneCard> sceneCards = loadWorkSceneCards(ep.getWorkId());
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortShot row : rows) {
            items.add(view(row, characters, sceneCards));
        }
        PageResponse response = new PageResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setMessage("OK");
        response.setResult(items);
        response.setPageNum(safePageNum);
        response.setPageSize(safePageSize);
        response.setTotalSize(total);
        return response;
    }

    @Transactional
    public ApiResponse update(ShotReq req) {
        ShortShot shot = requireShot(req == null ? null : req.getShotId());
        if ("LOCKED".equals(shot.getShotStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "locked shot cannot be edited");
        }
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(shot.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        String style = work == null ? "MODERN_URBAN" : work.getVisualStyle();
        ShortShot patch = new ShortShot();
        if (req.getShotDesc() != null) {
            patch.setShotDesc(req.getShotDesc());
        }
        if (req.getDialogueText() != null) {
            patch.setDialogueText(req.getDialogueText());
        }
        if (req.getShotSize() != null) {
            patch.setShotSize(req.getShotSize());
        }
        if (req.getCameraMove() != null) {
            patch.setCameraMove(req.getCameraMove());
        }
        if (req.getShotDurationSec() != null) {
            patch.setShotDurationSec(req.getShotDurationSec());
        }
        if (req.getExtraPrompt() != null) {
            patch.setExtraPrompt(req.getExtraPrompt());
        }
        String desc = req.getShotDesc() == null ? shot.getShotDesc() : req.getShotDesc();
        String dialogue = req.getDialogueText() == null ? shot.getDialogueText() : req.getDialogueText();
        String extra = req.getExtraPrompt() == null ? shot.getExtraPrompt() : req.getExtraPrompt();
        patch.setShotDesc(desc);
        patch.setDialogueText(dialogue);
        patch.setExtraPrompt(extra);
        ShortShot merged = new ShortShot();
        merged.setShotDesc(desc);
        merged.setDialogueText(dialogue);
        merged.setExtraPrompt(extra);
        merged.setShotSize(req.getShotSize() == null ? shot.getShotSize() : req.getShotSize());
        merged.setCameraMove(req.getCameraMove() == null ? shot.getCameraMove() : req.getCameraMove());
        merged.setEmotion(shot.getEmotion());
        merged.setLighting(shot.getLighting());
        patch.setImagePrompt(PromptComposer.compose(merged, style));
        patch.setPrevizText(PromptComposer.previzText(merged, sceneNameOf(shot.getSceneCardId())));
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse bindAssets(ShotReq req) {
        ShortShot shot = requireShot(req == null ? null : req.getShotId());
        if ("LOCKED".equals(shot.getShotStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "定稿镜头不能改绑定");
        }
        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andWorkIdEqualTo(shot.getWorkId()).andDeletedFlagEqualTo(0);
        List<ShortCharacter> characters = shortCharacterMapper.selectByExample(cx);
        ShortSceneCardExample scx = new ShortSceneCardExample();
        scx.createCriteria().andWorkIdEqualTo(shot.getWorkId()).andDeletedFlagEqualTo(0);
        List<ShortSceneCard> sceneCards = shortSceneCardMapper.selectByExample(scx);
        ShortShot patch = new ShortShot();
        if (req.getCharacterIdList() != null) {
            List<String> allowed = new ArrayList<>();
            for (String id : req.getCharacterIdList()) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                boolean found = false;
                for (ShortCharacter c : characters) {
                    if (id.equals(c.getCharacterId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new BizException(CommonErrorCode.INVALID_PARAM, "character not in this work");
                }
                allowed.add(id.trim());
            }
            patch.setCharacterRefJson(ShotConsistency.refsFromIds(allowed));
            shot.setCharacterRefJson(patch.getCharacterRefJson());
        }
        if (req.getSceneCardId() != null) {
            if (req.getSceneCardId().isBlank()) {
                patch.setSceneCardId("");
                shot.setSceneCardId(null);
            } else {
                boolean found = false;
                for (ShortSceneCard sc : sceneCards) {
                    if (req.getSceneCardId().equals(sc.getSceneCardId()) && ShotConsistency.isType(sc, "SCENE")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new BizException(CommonErrorCode.INVALID_PARAM, "scene card not in this work");
                }
                patch.setSceneCardId(req.getSceneCardId().trim());
                shot.setSceneCardId(patch.getSceneCardId());
            }
        }
        if (req.getPropIdList() != null) {
            List<String> allowed = new ArrayList<>();
            for (String id : req.getPropIdList()) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                boolean found = false;
                for (ShortSceneCard sc : sceneCards) {
                    if (id.equals(sc.getSceneCardId()) && ShotConsistency.isType(sc, "PROP")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new BizException(CommonErrorCode.INVALID_PARAM, "prop card not in this work");
                }
                allowed.add(id.trim());
            }
            patch.setPropRefJson(ShotConsistency.refsFromIds(allowed));
            shot.setPropRefJson(patch.getPropRefJson());
        }
        boolean blocked = hasUnlockedRefs(shot, characters, sceneCards);
        patch.setBlockedFlag(blocked || ShotConsistency.shotNeedsPrompt(shot) ? 1 : 0);
        patch.setStaleFlag(0);
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
        ShortShot fresh = requireShot(shot.getShotId());
        return Responses.ok(view(fresh));
    }

    @Transactional
    public ApiResponse lock(ShotReq req, boolean locked) {
        ShortShot shot = requireShot(req == null ? null : req.getShotId());
        ShortShot patch = new ShortShot();
        patch.setShotStatus(locked ? "LOCKED" : (shot.getVideoAssetId() == null ? "IMAGE_READY" : "VIDEO_READY"));
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
        refreshStoryboardReady(shot.getEpisodeId());
        return Responses.ok("ok");
    }

    private String sceneNameOf(String sceneCardId) {
        if (sceneCardId == null || sceneCardId.isBlank()) {
            return "";
        }
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andSceneCardIdEqualTo(sceneCardId).andDeletedFlagEqualTo(0);
        ShortSceneCard card = shortSceneCardMapper.selectOneByExample(ex);
        return card == null || card.getCardName() == null ? "" : card.getCardName();
    }

    private static String cardName(List<ShortSceneCard> cards, String sceneCardId) {
        if (cards == null || sceneCardId == null || sceneCardId.isBlank()) {
            return "";
        }
        for (ShortSceneCard card : cards) {
            if (sceneCardId.equals(card.getSceneCardId())) {
                return card.getCardName() == null ? "" : card.getCardName();
            }
        }
        return "";
    }

    public ShortShot requireShot(String shotId) {
        String userId = workAccessService.requireUser();
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(shotId).andDeletedFlagEqualTo(0);
        ShortShot row = shortShotMapper.selectOneByExample(ex);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new ForbiddenException("shot not found");
        }
        return row;
    }

    private void insertShot(ShortEpisode ep, int shotNo, int sceneNo, Map<String, Object> scene,
            Map<String, Object> line, String style, List<ShortCharacter> characters,
            List<ShortSceneCard> sceneCards) {
        String desc = ShortJson.text(scene.get("scene_summary")) + " / " + ShortJson.text(line.get("line_text"));
        String dialogue = ShortJson.text(line.get("line_text"));
        String characterName = ShortJson.text(line.get("character_name"));
        String haystack = characterName + " " + desc + " " + dialogue;
        ShortShot shot = new ShortShot();
        shot.setShotId(ShortIds.id("sh-"));
        shot.setEpisodeId(ep.getEpisodeId());
        shot.setWorkId(ep.getWorkId());
        shot.setUserId(ep.getUserId());
        shot.setShotNo(shotNo);
        shot.setSceneNo(sceneNo);
        shot.setShotSize("MEDIUM");
        shot.setCameraMove("FIXED");
        shot.setShotDurationSec(ShortJson.integer(line.get("estimated_sec"), 3));
        shot.setShotDesc(desc);
        shot.setDialogueText(dialogue);
        shot.setEmotion(ShortJson.text(line.get("emotion")));
        shot.setCharacterRefJson(ShotConsistency.bindCharacterRefs(characters, haystack, characterName));
        shot.setSceneCardId(ShotConsistency.bindSceneCardId(sceneCards,
                ShortJson.text(scene.get("scene_location")) + " " + ShortJson.text(scene.get("scene_summary"))));
        shot.setPropRefJson(ShotConsistency.bindPropRefs(sceneCards, haystack + " "
                + ShortJson.text(scene.get("scene_location")) + " " + ShortJson.text(scene.get("scene_summary"))));
        shot.setPrevizText(PromptComposer.previzText(shot, cardName(sceneCards, shot.getSceneCardId())));
        shot.setImagePrompt(PromptComposer.compose(shot, style));
        shot.setPrevizLevel("TEXT");
        shot.setShotStatus("PENDING");
        boolean refsUnlocked = hasUnlockedRefs(shot, characters, sceneCards);
        shot.setBlockedFlag(refsUnlocked || ShotConsistency.shotNeedsPrompt(shot) ? 1 : 0);
        shot.setRiskLevel("PASS");
        shot.setRetryCount(0);
        shot.setCostSpentCent(0);
        shot.setShotVersion(1);
        shot.setDeletedFlag(0);
        shot.setCreatedTime(new Date());
        shortShotMapper.insertSelective(shot);
    }

    private boolean hasUnlockedRefs(ShortShot shot, List<ShortCharacter> characters,
            List<ShortSceneCard> sceneCards) {
        return ShotConsistency.hasUnlockedRefs(shot, characters, sceneCards);
    }

    private int maxLockedNo(String episodeId) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andShotStatusEqualTo("LOCKED").andDeletedFlagEqualTo(0);
        ex.setOrderByClause("shot_no desc");
        ShortShot row = shortShotMapper.selectOneByExample(ex);
        return row == null || row.getShotNo() == null ? 0 : row.getShotNo();
    }

    private void patchStoryboard(ShortEpisode ep, String status) {
        ShortEpisode patch = new ShortEpisode();
        patch.setStoryboardStatus(status);
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
    }

    private void refreshStoryboardReady(String episodeId) {
        ShortShotExample all = new ShortShotExample();
        all.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        long total = shortShotMapper.countByExample(all);
        ShortShotExample locked = new ShortShotExample();
        locked.createCriteria().andEpisodeIdEqualTo(episodeId).andShotStatusEqualTo("LOCKED").andDeletedFlagEqualTo(0);
        long lockedCount = shortShotMapper.countByExample(locked);
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ShortEpisode ep = shortEpisodeMapper.selectOneByExample(ex);
        if (ep == null) {
            return;
        }
        patchStoryboard(ep, total > 0 && total == lockedCount ? "READY" : "DRAFT");
    }

    private List<ShortCharacter> loadWorkCharacters(String workId) {
        if (shortCharacterMapper == null || workId == null || workId.isBlank()) {
            return List.of();
        }
        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        List<ShortCharacter> rows = shortCharacterMapper.selectByExample(cx);
        return rows == null ? List.of() : rows;
    }

    private List<ShortSceneCard> loadWorkSceneCards(String workId) {
        if (shortSceneCardMapper == null || workId == null || workId.isBlank()) {
            return List.of();
        }
        ShortSceneCardExample scx = new ShortSceneCardExample();
        scx.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        List<ShortSceneCard> rows = shortSceneCardMapper.selectByExample(scx);
        return rows == null ? List.of() : rows;
    }

    private Map<String, Object> view(ShortShot row) {
        return view(row, loadWorkCharacters(row.getWorkId()), loadWorkSceneCards(row.getWorkId()));
    }

    private Map<String, Object> view(ShortShot row, List<ShortCharacter> characters, List<ShortSceneCard> sceneCards) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("shot_id", row.getShotId());
        item.put("shot_no", row.getShotNo());
        item.put("scene_no", row.getSceneNo());
        item.put("shot_status", row.getShotStatus());
        item.put("shot_desc", row.getShotDesc());
        item.put("dialogue_text", row.getDialogueText());
        item.put("shot_size", row.getShotSize());
        item.put("camera_move", row.getCameraMove());
        item.put("shot_duration_sec", row.getShotDurationSec());
        String anchors = ShotConsistency.anchorFragmentForShot(row, characters, sceneCards);
        item.put("image_prompt", ShotConsistency.leadWithAnchors(row.getImagePrompt(), anchors));
        List<Map<String, String>> segments = new ArrayList<>(PromptComposer.segments(row));
        if (!anchors.isBlank()) {
            Map<String, String> anchor = new LinkedHashMap<>();
            anchor.put("source", "ANCHOR");
            anchor.put("label", "定妆");
            anchor.put("text", anchors);
            segments.add(anchor);
        }
        item.put("prompt_segments", segments);
        item.put("previz_text", row.getPrevizText());
        item.put("look_plain", PromptComposer.lookPlain(row));
        boolean framePlaceholder = assetService != null && assetService.placeholderImage(row.getFirstFrameAssetId());
        boolean videoPlaceholder = assetService != null && assetService.placeholderVideo(row.getVideoAssetId());
        item.put("first_frame_placeholder_flag", framePlaceholder ? 1 : 0);
        item.put("video_placeholder_flag", videoPlaceholder ? 1 : 0);
        item.put("reference_note", framePlaceholder
                ? "首帧还是占位图，不会当作参考。公司视频按定妆外貌、服装和场景文字生成，有真实定妆照片时会作为参考图提交。"
                : null);
        item.put("previz_level", row.getPrevizLevel() == null ? "NONE" : row.getPrevizLevel());
        item.put("emotion", row.getEmotion());
        item.put("lighting", row.getLighting());
        item.put("extra_prompt", row.getExtraPrompt());
        Map<String, Object> check = PromptComposer.check(row, row.getImagePrompt());
        item.put("prompt_score", check.get("prompt_score"));
        item.put("prompt_pass_flag", check.get("pass_flag"));
        item.put("first_frame_asset_id", row.getFirstFrameAssetId());
        item.put("video_asset_id", row.getVideoAssetId());
        item.put("first_frame_url", assetService == null ? null : assetService.resolveUrl(row.getFirstFrameAssetId()));
        item.put("video_url", assetService == null ? null : assetService.resolveUrl(row.getVideoAssetId()));
        item.put("last_frame_asset_id", row.getLastFrameAssetId());
        item.put("last_frame_url", assetService == null ? null : assetService.resolveUrl(row.getLastFrameAssetId()));
        item.put("last_frame_prompt", row.getLastFramePrompt());
        item.put("frame_link_mode", row.getFrameLinkMode() == null ? "NONE" : row.getFrameLinkMode());
        if (frameLinkService != null) {
            item.putAll(frameLinkService.effectiveState(row));
        }
        item.put("character_ref_json", ShotConsistency.characterIds(row.getCharacterRefJson()));
        item.put("scene_card_id", row.getSceneCardId());
        item.put("prop_ref_json", ShotConsistency.propIds(row.getPropRefJson()));
        item.put("stale_flag", row.getStaleFlag() == null ? 0 : row.getStaleFlag());
        boolean blocked = hasUnlockedRefs(row, characters, sceneCards) || ShotConsistency.shotNeedsPrompt(row);
        item.put("blocked_flag", blocked ? 1 : 0);
        item.put("block_reason", blocked
                ? ShotConsistency.describeShotBlockReason(row, characters, sceneCards)
                : "");
        item.put("risk_level", row.getRiskLevel());
        item.put("latest_task", latestTask(row.getShotId()));
        return item;
    }

    private Map<String, Object> latestTask(String shotId) {
        if (shortAiTaskMapper == null || shotId == null || shotId.isBlank()) {
            return null;
        }
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTargetIdEqualTo(shotId);
        ex.setOrderByClause("id desc");
        java.util.List<ShortAiTask> tasks = shortAiTaskMapper.selectByExample(ex);
        ShortAiTask task = tasks == null || tasks.isEmpty() ? null : tasks.get(0);
        if (task == null) {
            return null;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("task_id", task.getTaskId());
        item.put("task_type", task.getTaskType());
        item.put("task_status", task.getTaskStatus());
        item.put("progress_percent", task.getProgressPercent());
        item.putAll(GenerationTaskStatus.flags(task.getTaskStatus(), task.getTaskType()));
        if (officialVideoQueue != null) {
            officialVideoQueue.enrich(item, task);
        }
        return item;
    }

    private String anchorText(ShortShot shot) {
        if (shot == null) {
            return "";
        }
        List<ShortCharacter> characters = new ArrayList<>();
        for (String characterId : ShotConsistency.characterIds(shot.getCharacterRefJson())) {
            ShortCharacterExample cx = new ShortCharacterExample();
            cx.createCriteria().andCharacterIdEqualTo(characterId).andDeletedFlagEqualTo(0);
            ShortCharacter row = shortCharacterMapper.selectOneByExample(cx);
            if (row != null && "LOCKED".equals(row.getCardStatus())) {
                characters.add(row);
            }
        }
        ShortSceneCard scene = null;
        if (shot.getSceneCardId() != null && !shot.getSceneCardId().isBlank()) {
            ShortSceneCardExample sx = new ShortSceneCardExample();
            sx.createCriteria().andSceneCardIdEqualTo(shot.getSceneCardId()).andDeletedFlagEqualTo(0);
            ShortSceneCard row = shortSceneCardMapper.selectOneByExample(sx);
            if (row != null && "LOCKED".equals(row.getCardStatus())) {
                scene = row;
            }
        }
        List<ShortSceneCard> props = new ArrayList<>();
        for (String propId : ShotConsistency.propIds(shot.getPropRefJson())) {
            ShortSceneCardExample px = new ShortSceneCardExample();
            px.createCriteria().andSceneCardIdEqualTo(propId).andDeletedFlagEqualTo(0);
            ShortSceneCard row = shortSceneCardMapper.selectOneByExample(px);
            if (row != null && "LOCKED".equals(row.getCardStatus())) {
                props.add(row);
            }
        }
        return ShotConsistency.appearanceFragment(characters, scene, props);
    }
}
