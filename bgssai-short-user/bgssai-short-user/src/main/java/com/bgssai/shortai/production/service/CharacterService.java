package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortContinuityIssueMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortContinuityIssue;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ProductionDtos.CharacterReq;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShotConsistency;

@Service
public class CharacterService {

    @Autowired
    private AssetService assetService;

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortSceneCardMapper shortSceneCardMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortAssetMapper shortAssetMapper;
    @Autowired(required = false)
    private ShortContinuityIssueMapper shortContinuityIssueMapper;

    public ApiResponse listCharacters(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id asc");
        List<Map<String, Object>> list = new ArrayList<>();
        List<com.bgssai.shortai.model.ShortShot> shots = workShots(work.getWorkId());
        for (ShortCharacter row : shortCharacterMapper.selectByExample(ex)) {
            list.add(characterView(row, shots));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("character_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse createCharacter(CharacterReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        if (req.getCharacterName() == null || req.getCharacterName().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "character_name required");
        }
        ShortCharacter row = new ShortCharacter();
        row.setCharacterId(com.bgssai.shortai.production.support.ShortIds.id("c-"));
        row.setWorkId(work.getWorkId());
        row.setUserId(work.getUserId());
        row.setCharacterName(req.getCharacterName().trim());
        row.setCharacterRole(req.getCharacterRole());
        row.setAppearancePrompt(req.getAppearancePrompt());
        row.setCardStatus("DRAFT");
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        shortCharacterMapper.insertSelective(row);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("character_id", row.getCharacterId());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse updateCharacter(CharacterReq req) {
        ShortCharacter row = requireCharacter(req == null ? null : req.getCharacterId());
        ShortCharacter patch = new ShortCharacter();
        boolean settingChanged = false;
        if (req.getCharacterName() != null && !Objects.equals(req.getCharacterName(), row.getCharacterName())) {
            patch.setCharacterName(req.getCharacterName());
            row.setCharacterName(req.getCharacterName());
            settingChanged = true;
        }
        if (req.getAppearancePrompt() != null && !Objects.equals(req.getAppearancePrompt(), row.getAppearancePrompt())) {
            patch.setAppearancePrompt(req.getAppearancePrompt());
            row.setAppearancePrompt(req.getAppearancePrompt());
            settingChanged = true;
        }
        if (req.getCharacterRole() != null && !Objects.equals(req.getCharacterRole(), row.getCharacterRole())) {
            patch.setCharacterRole(req.getCharacterRole());
            row.setCharacterRole(req.getCharacterRole());
            settingChanged = true;
        }
        if (!settingChanged) return Responses.ok("ok");
        patch.setCardStatus("DRAFT");
        row.setCardStatus("DRAFT");
        patch.setUpdatedTime(new Date());
        ShortCharacterExample up = new ShortCharacterExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortCharacterMapper.updateByExampleSelective(patch, up);
        if (settingChanged) {
            markShotsStaleForCharacter(row);
        }
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse deleteCharacter(CharacterReq req) {
        ShortCharacter row = requireCharacter(req == null ? null : req.getCharacterId());
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andWorkIdEqualTo(row.getWorkId()).andDeletedFlagEqualTo(0);
        if (shortShotMapper.countByExample(sx) > 0 && "LOCKED".equals(row.getCardStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "character is referenced by shots");
        }
        ShortCharacter patch = new ShortCharacter();
        patch.setDeletedFlag(1);
        patch.setUpdatedTime(new Date());
        ShortCharacterExample up = new ShortCharacterExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortCharacterMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    public ApiResponse portrait(CharacterReq req) {
        ShortCharacter row = requireCharacter(req == null ? null : req.getCharacterId());
        int count = req.getCandidateCount() == null ? 4 : req.getCandidateCount();
        if (count != 2 && count != 4 && count != 8) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "candidate_count must be 2/4/8");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("character_id", row.getCharacterId());
        request.put("candidate_count", count);
        ShortAiTask task = aiTaskQueue.enqueue(row.getUserId(), row.getWorkId(), null, "IMAGE", "PORTRAIT",
                row.getCharacterId(), request, null, 0, req.getModelCode());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse lockCharacter(CharacterReq req) {
        ShortCharacter row = requireCharacter(req == null ? null : req.getCharacterId());
        if (req.getAssetId() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "asset_id required");
        }
        ShortCharacter patch = new ShortCharacter();
        patch.setRefImageAssetId(req.getAssetId());
        patch.setCardStatus("LOCKED");
        patch.setUpdatedTime(new Date());
        ShortCharacterExample up = new ShortCharacterExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortCharacterMapper.updateByExampleSelective(patch, up);
        row.setRefImageAssetId(req.getAssetId());
        row.setCardStatus("LOCKED");
        markShotsStaleForCharacter(row);
        return Responses.ok("ok");
    }

    public ApiResponse listScenes(String workId) {
        return listScenes(workId, null);
    }

    public ApiResponse listScenes(String workId, String cardType) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        List<com.bgssai.shortai.model.ShortShot> shots = workShots(work.getWorkId());
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortSceneCard row : shortSceneCardMapper.selectByExample(ex)) {
            if (cardType != null && !cardType.isBlank() && !cardType.equalsIgnoreCase(
                    row.getCardType() == null ? "SCENE" : row.getCardType())) {
                continue;
            }
            list.add(sceneView(row, shots));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_card_list", list);
        return Responses.ok(result);
    }

    public ApiResponse consistencyPanel(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        List<com.bgssai.shortai.model.ShortShot> shots = workShots(work.getWorkId());
        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        cx.setOrderByClause("id asc");
        List<ShortCharacter> characterRows = shortCharacterMapper.selectByExample(cx);
        List<Map<String, Object>> characters = new ArrayList<>();
        for (ShortCharacter row : characterRows) {
            characters.add(characterView(row, shots));
        }
        ShortSceneCardExample sx = new ShortSceneCardExample();
        sx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        List<ShortSceneCard> sceneRows = shortSceneCardMapper.selectByExample(sx);
        List<Map<String, Object>> scenes = new ArrayList<>();
        List<Map<String, Object>> props = new ArrayList<>();
        for (ShortSceneCard row : sceneRows) {
            Map<String, Object> item = sceneView(row, shots);
            if (ShotConsistency.isType(row, "PROP")) {
                props.add(item);
            } else {
                scenes.add(item);
            }
        }
        int unbound = 0;
        int unlocked = 0;
        for (com.bgssai.shortai.model.ShortShot shot : shots) {
            boolean hasRef = !ShotConsistency.characterIds(shot.getCharacterRefJson()).isEmpty()
                    || (shot.getSceneCardId() != null && !shot.getSceneCardId().isBlank())
                    || !ShotConsistency.propIds(shot.getPropRefJson()).isEmpty();
            if (!hasRef) {
                unbound++;
            }
            if (ShotConsistency.hasUnlockedRefs(shot, characterRows, sceneRows)) {
                unlocked++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("character_list", characters);
        result.put("scene_list", scenes);
        result.put("prop_list", props);
        result.put("unbound_shot_count", unbound);
        result.put("unlocked_ref_count", unlocked);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse createScene(CharacterReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        ShortSceneCard row = new ShortSceneCard();
        row.setSceneCardId(com.bgssai.shortai.production.support.ShortIds.id("sc-"));
        row.setWorkId(work.getWorkId());
        row.setUserId(work.getUserId());
        row.setCardType(req.getCardType() == null ? "SCENE" : req.getCardType());
        row.setCardName(req.getCardName());
        row.setAppearancePrompt(req.getAppearancePrompt());
        row.setCardStatus("DRAFT");
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        shortSceneCardMapper.insertSelective(row);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_card_id", row.getSceneCardId());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse updateScene(CharacterReq req) {
        ShortSceneCard row = requireScene(req == null ? null : req.getSceneCardId());
        ShortSceneCard patch = new ShortSceneCard();
        boolean changed = false;
        if (req.getCardName() != null && !Objects.equals(req.getCardName(), row.getCardName())) {
            if (req.getCardName().isBlank()) throw new BizException(CommonErrorCode.INVALID_PARAM, "card_name required");
            patch.setCardName(req.getCardName());
            row.setCardName(req.getCardName());
            changed = true;
        }
        if (req.getAppearancePrompt() != null && !Objects.equals(req.getAppearancePrompt(), row.getAppearancePrompt())) {
            patch.setAppearancePrompt(req.getAppearancePrompt());
            row.setAppearancePrompt(req.getAppearancePrompt());
            changed = true;
        }
        if (!changed) return Responses.ok("ok");
        patch.setCardStatus("DRAFT");
        row.setCardStatus("DRAFT");
        patch.setUpdatedTime(new Date());
        ShortSceneCardExample up = new ShortSceneCardExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortSceneCardMapper.updateByExampleSelective(patch, up);
        row.setCardStatus(patch.getCardStatus());
        if (patch.getRefImageAssetId() != null) row.setRefImageAssetId(patch.getRefImageAssetId());
        markShotsStaleForScene(row);
        return Responses.ok("ok");
    }

    public ApiResponse portraitScene(CharacterReq req) {
        ShortSceneCard row = requireScene(req == null ? null : req.getSceneCardId());
        int count = req.getCandidateCount() == null ? 4 : req.getCandidateCount();
        if (count != 2 && count != 4 && count != 8) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "candidate_count must be 2/4/8");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scene_card_id", row.getSceneCardId());
        request.put("candidate_count", count);
        ShortAiTask task = aiTaskQueue.enqueue(row.getUserId(), row.getWorkId(), null, "IMAGE", "SCENE_PORTRAIT",
                row.getSceneCardId(), request, null, 0, req.getModelCode());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse lockScene(CharacterReq req) {
        ShortSceneCard row = requireScene(req == null ? null : req.getSceneCardId());
        ShortSceneCard patch = new ShortSceneCard();
        patch.setRefImageAssetId(req.getAssetId());
        patch.setCardStatus("LOCKED");
        patch.setUpdatedTime(new Date());
        ShortSceneCardExample up = new ShortSceneCardExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortSceneCardMapper.updateByExampleSelective(patch, up);
        row.setCardStatus(patch.getCardStatus());
        if (patch.getRefImageAssetId() != null) row.setRefImageAssetId(patch.getRefImageAssetId());
        markShotsStaleForScene(row);
        return Responses.ok("ok");
    }

    private void markShotsStaleForCharacter(ShortCharacter character) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andWorkIdEqualTo(character.getWorkId()).andDeletedFlagEqualTo(0);
        List<ShortCharacter> characters = workCharacters(character.getWorkId());
        List<ShortSceneCard> scenes = workScenes(character.getWorkId());
        Date now = new Date();
        String label = character.getCharacterName() == null || character.getCharacterName().isBlank()
                ? character.getCharacterId()
                : character.getCharacterName();
        for (com.bgssai.shortai.model.ShortShot shot : shortShotMapper.selectByExample(ex)) {
            if ("LOCKED".equals(shot.getShotStatus())) {
                continue;
            }
            if (!ShotConsistency.containsRef(shot.getCharacterRefJson(), character.getCharacterId())) {
                continue;
            }
            com.bgssai.shortai.model.ShortShot patch = new com.bgssai.shortai.model.ShortShot();
            patch.setStaleFlag(1);
            patch.setBlockedFlag(ShotConsistency.hasUnlockedRefs(shot, characters, scenes)
                    || ShotConsistency.shotNeedsPrompt(shot) ? 1 : 0);
            patch.setUpdatedTime(now);
            ShortShotExample up = new ShortShotExample();
            up.createCriteria().andIdEqualTo(shot.getId());
            shortShotMapper.updateByExampleSelective(patch, up);
            if (shortContinuityIssueMapper != null) {
                ShortContinuityIssue issue = new ShortContinuityIssue();
                issue.setIssueId(ShortIds.id("ci-"));
                issue.setEpisodeId(shot.getEpisodeId());
                issue.setWorkId(character.getWorkId());
                issue.setIssueType("CHARACTER");
                issue.setSeverity("MAJOR");
                issue.setIssueDesc("角色设定已变更（" + label + "），需重新对齐镜头一致性");
                issue.setShotId(shot.getShotId());
                issue.setRelatedShotId(character.getCharacterId());
                issue.setHandleStatus("OPEN");
                issue.setDeletedFlag(0);
                issue.setCreatedTime(now);
                issue.setUpdatedTime(now);
                shortContinuityIssueMapper.insertSelective(issue);
            }
        }
    }

    private void markShotsStaleForScene(ShortSceneCard scene) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andWorkIdEqualTo(scene.getWorkId()).andDeletedFlagEqualTo(0);
        List<ShortCharacter> characters = workCharacters(scene.getWorkId());
        List<ShortSceneCard> scenes = workScenes(scene.getWorkId());
        Date now = new Date();
        for (com.bgssai.shortai.model.ShortShot shot : shortShotMapper.selectByExample(ex)) {
            if ("LOCKED".equals(shot.getShotStatus())) {
                continue;
            }
            boolean hit = scene.getSceneCardId() != null && scene.getSceneCardId().equals(shot.getSceneCardId());
            if (!hit && ShotConsistency.isType(scene, "PROP")) {
                hit = ShotConsistency.containsRef(shot.getPropRefJson(), scene.getSceneCardId());
            }
            if (!hit) {
                continue;
            }
            com.bgssai.shortai.model.ShortShot patch = new com.bgssai.shortai.model.ShortShot();
            patch.setStaleFlag(1);
            patch.setBlockedFlag(ShotConsistency.hasUnlockedRefs(shot, characters, scenes)
                    || ShotConsistency.shotNeedsPrompt(shot) ? 1 : 0);
            patch.setUpdatedTime(now);
            ShortShotExample up = new ShortShotExample();
            up.createCriteria().andIdEqualTo(shot.getId());
            shortShotMapper.updateByExampleSelective(patch, up);
        }
    }

    private List<ShortCharacter> workCharacters(String workId) {
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        return shortCharacterMapper.selectByExample(ex);
    }

    private List<ShortSceneCard> workScenes(String workId) {
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        return shortSceneCardMapper.selectByExample(ex);
    }

    private String cardStatus(String persisted, String targetId) {
        if ("LOCKED".equals(persisted)) return persisted;
        ShortAiTaskExample tasks = new ShortAiTaskExample();
        tasks.createCriteria().andTargetIdEqualTo(targetId).andTaskTypeEqualTo("IMAGE")
                .andTargetTypeIn(List.of("PORTRAIT", "SCENE_PORTRAIT"))
                .andTaskStatusIn(List.of("QUEUED", "RUNNING"));
        if (shortAiTaskMapper.countByExample(tasks) > 0) return "GENERATING";
        return "GENERATING".equals(persisted) ? "DRAFT" : persisted;
    }

    private List<com.bgssai.shortai.model.ShortShot> workShots(String workId) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        return shortShotMapper.selectByExample(ex);
    }

    private Map<String, Object> characterView(ShortCharacter row) {
        return characterView(row, workShots(row.getWorkId()));
    }

    private Map<String, Object> characterView(ShortCharacter row, List<com.bgssai.shortai.model.ShortShot> shots) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("character_id", row.getCharacterId());
        item.put("work_id", row.getWorkId());
        item.put("character_name", row.getCharacterName());
        item.put("character_role", row.getCharacterRole());
        item.put("card_status", cardStatus(row.getCardStatus(), row.getCharacterId()));
        item.put("appearance_prompt", row.getAppearancePrompt());
        item.put("ref_image_asset_id", row.getRefImageAssetId());
        item.put("ref_image_url", assetUrl(row.getRefImageAssetId()));
        item.put("candidate_list", candidates(row.getCharacterId()));
        item.put("used_shot_count", countCharacterShots(shots, row.getCharacterId()));
        return item;
    }

    private Map<String, Object> sceneView(ShortSceneCard row) {
        return sceneView(row, workShots(row.getWorkId()));
    }

    private Map<String, Object> sceneView(ShortSceneCard row, List<com.bgssai.shortai.model.ShortShot> shots) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("scene_card_id", row.getSceneCardId());
        item.put("card_name", row.getCardName());
        item.put("card_type", row.getCardType() == null ? "SCENE" : row.getCardType());
        item.put("card_status", cardStatus(row.getCardStatus(), row.getSceneCardId()));
        item.put("appearance_prompt", row.getAppearancePrompt());
        item.put("ref_image_asset_id", row.getRefImageAssetId());
        item.put("ref_image_url", assetUrl(row.getRefImageAssetId()));
        item.put("candidate_list", candidates(row.getSceneCardId()));
        item.put("used_shot_count", countSceneShots(shots, row));
        return item;
    }

    private int countCharacterShots(List<com.bgssai.shortai.model.ShortShot> shots, String characterId) {
        int count = 0;
        for (com.bgssai.shortai.model.ShortShot shot : shots) {
            if (ShotConsistency.containsRef(shot.getCharacterRefJson(), characterId)) {
                count++;
            }
        }
        return count;
    }

    private int countSceneShots(List<com.bgssai.shortai.model.ShortShot> shots, ShortSceneCard card) {
        int count = 0;
        for (com.bgssai.shortai.model.ShortShot shot : shots) {
            if (card.getSceneCardId() != null && card.getSceneCardId().equals(shot.getSceneCardId())) {
                count++;
            } else if (ShotConsistency.isType(card, "PROP")
                    && ShotConsistency.containsRef(shot.getPropRefJson(), card.getSceneCardId())) {
                count++;
            }
        }
        return count;
    }

    private List<Map<String, Object>> candidates(String targetId) {
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andShotIdEqualTo(targetId).andDeletedFlagEqualTo(0);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortAsset asset : shortAssetMapper.selectByExample(ex)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("asset_id", asset.getAssetId());
            item.put("file_url", assetService.resolveUrl(asset.getAssetId()));
            item.put("selected_flag", asset.getSelectedFlag());
            list.add(item);
        }
        return list;
    }

    private String assetUrl(String assetId) {
        return assetService.resolveUrl(assetId);
    }

    private ShortCharacter requireCharacter(String characterId) {
        String userId = workAccessService.requireUser();
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andCharacterIdEqualTo(characterId).andDeletedFlagEqualTo(0);
        ShortCharacter row = shortCharacterMapper.selectOneByExample(ex);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new com.bgssai.shortai.common.exception.ForbiddenException("character not found");
        }
        return row;
    }

    private ShortSceneCard requireScene(String sceneCardId) {
        String userId = workAccessService.requireUser();
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andSceneCardIdEqualTo(sceneCardId).andDeletedFlagEqualTo(0);
        ShortSceneCard row = shortSceneCardMapper.selectOneByExample(ex);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new com.bgssai.shortai.common.exception.ForbiddenException("scene card not found");
        }
        return row;
    }
}
