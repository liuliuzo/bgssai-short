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
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ProductionDtos.CharacterReq;

@Service
public class CharacterService {

    @Autowired
    private AssetService assetService;

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortSceneCardMapper shortSceneCardMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortAssetMapper shortAssetMapper;

    public ApiResponse listCharacters(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id asc");
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortCharacter row : shortCharacterMapper.selectByExample(ex)) {
            list.add(characterView(row));
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
        if (req.getCharacterName() != null) {
            patch.setCharacterName(req.getCharacterName());
        }
        if (req.getAppearancePrompt() != null) {
            patch.setAppearancePrompt(req.getAppearancePrompt());
        }
        if (req.getCharacterRole() != null) {
            patch.setCharacterRole(req.getCharacterRole());
        }
        patch.setUpdatedTime(new Date());
        ShortCharacterExample up = new ShortCharacterExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortCharacterMapper.updateByExampleSelective(patch, up);
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
        return Responses.ok("ok");
    }

    public ApiResponse listScenes(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortSceneCard row : shortSceneCardMapper.selectByExample(ex)) {
            list.add(sceneView(row));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_card_list", list);
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

    public ApiResponse portraitScene(CharacterReq req) {
        ShortSceneCard row = requireScene(req == null ? null : req.getSceneCardId());
        int count = req.getCandidateCount() == null ? 4 : req.getCandidateCount();
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
        return Responses.ok("ok");
    }

    private Map<String, Object> characterView(ShortCharacter row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("character_id", row.getCharacterId());
        item.put("work_id", row.getWorkId());
        item.put("character_name", row.getCharacterName());
        item.put("character_role", row.getCharacterRole());
        item.put("card_status", row.getCardStatus());
        item.put("appearance_prompt", row.getAppearancePrompt());
        item.put("ref_image_asset_id", row.getRefImageAssetId());
        item.put("ref_image_url", assetUrl(row.getRefImageAssetId()));
        item.put("candidate_list", candidates(row.getCharacterId()));
        return item;
    }

    private Map<String, Object> sceneView(ShortSceneCard row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("scene_card_id", row.getSceneCardId());
        item.put("card_name", row.getCardName());
        item.put("card_type", row.getCardType());
        item.put("card_status", row.getCardStatus());
        item.put("appearance_prompt", row.getAppearancePrompt());
        item.put("ref_image_asset_id", row.getRefImageAssetId());
        item.put("ref_image_url", assetUrl(row.getRefImageAssetId()));
        item.put("candidate_list", candidates(row.getSceneCardId()));
        return item;
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
