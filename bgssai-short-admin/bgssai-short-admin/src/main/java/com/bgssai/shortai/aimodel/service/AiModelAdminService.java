package com.bgssai.shortai.aimodel.service;

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

import com.bgssai.shortai.aimodel.dto.ReqAiModel;
import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.production.support.ShortIds;

@Service
public class AiModelAdminService {

    private static final Logger log = LoggerFactory.getLogger(AiModelAdminService.class);

    @Autowired
    private AiModelConfigMapper aiModelConfigMapper;

    public ApiResponse list(String abilityType) {
        requireAdmin();
        AiModelConfigExample ex = new AiModelConfigExample();
        AiModelConfigExample.Criteria c = ex.createCriteria().andDeletedFlagEqualTo(0);
        if (abilityType != null && !abilityType.isBlank()) {
            c.andAbilityTypeEqualTo(abilityType.trim());
        }
        ex.setOrderByClause("ability_type asc, default_flag desc, id asc");
        List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
        List<Map<String, Object>> list = new ArrayList<>();
        for (AiModelConfig row : rows) {
            list.add(view(row));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse create(ReqAiModel req) {
        requireAdmin();
        validateWrite(req, true);
        Date now = new Date();
        AiModelConfig row = new AiModelConfig();
        row.setModelId(ShortIds.id("m-"));
        apply(row, req);
        if (Integer.valueOf(1).equals(row.getDefaultFlag())) {
            clearDefault(row.getAbilityType());
        }
        row.setDeletedFlag(0);
        row.setCreatedTime(now);
        row.setUpdatedTime(now);
        aiModelConfigMapper.insertSelective(row);
        log.info("ai model created modelId={} code={}", row.getModelId(), row.getModelCode());
        return Responses.ok(view(row));
    }

    @Transactional
    public ApiResponse update(ReqAiModel req) {
        requireAdmin();
        if (req == null || req.getModelId() == null || req.getModelId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model_id is required");
        }
        AiModelConfig existing = requireModel(req.getModelId().trim());
        validateWrite(req, false);
        AiModelConfig patch = new AiModelConfig();
        apply(patch, req);
        if (patch.getAbilityType() == null) {
            patch.setAbilityType(existing.getAbilityType());
        }
        if (Integer.valueOf(1).equals(patch.getDefaultFlag())) {
            clearDefault(patch.getAbilityType());
        }
        patch.setUpdatedTime(new Date());
        AiModelConfigExample up = new AiModelConfigExample();
        up.createCriteria().andIdEqualTo(existing.getId()).andDeletedFlagEqualTo(0);
        aiModelConfigMapper.updateByExampleSelective(patch, up);
        log.info("ai model updated modelId={}", existing.getModelId());
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse setDefault(ReqAiModel req) {
        requireAdmin();
        if (req == null || req.getModelId() == null || req.getModelId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model_id is required");
        }
        AiModelConfig existing = requireModel(req.getModelId().trim());
        if (!Integer.valueOf(1).equals(existing.getEnabledFlag())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "disabled model cannot be default");
        }
        clearDefault(existing.getAbilityType());
        AiModelConfig patch = new AiModelConfig();
        patch.setDefaultFlag(1);
        patch.setUpdatedTime(new Date());
        AiModelConfigExample up = new AiModelConfigExample();
        up.createCriteria().andIdEqualTo(existing.getId()).andDeletedFlagEqualTo(0);
        aiModelConfigMapper.updateByExampleSelective(patch, up);
        log.info("ai model set default modelId={}", existing.getModelId());
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse toggleEnabled(ReqAiModel req) {
        requireAdmin();
        if (req == null || req.getModelId() == null || req.getModelId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model_id is required");
        }
        if (!Integer.valueOf(0).equals(req.getEnabledFlag()) && !Integer.valueOf(1).equals(req.getEnabledFlag())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "enabled_flag must be 0 or 1");
        }
        AiModelConfig existing = requireModel(req.getModelId().trim());
        AiModelConfig patch = new AiModelConfig();
        patch.setEnabledFlag(req.getEnabledFlag());
        if (Integer.valueOf(0).equals(req.getEnabledFlag())) {
            patch.setDefaultFlag(0);
        }
        patch.setUpdatedTime(new Date());
        AiModelConfigExample up = new AiModelConfigExample();
        up.createCriteria().andIdEqualTo(existing.getId()).andDeletedFlagEqualTo(0);
        aiModelConfigMapper.updateByExampleSelective(patch, up);
        log.info("ai model toggle enabled modelId={} flag={}", existing.getModelId(), req.getEnabledFlag());
        return Responses.ok("ok");
    }

    private void clearDefault(String abilityType) {
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria().andAbilityTypeEqualTo(abilityType).andDeletedFlagEqualTo(0);
        List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
        for (AiModelConfig row : rows) {
            if (Integer.valueOf(1).equals(row.getDefaultFlag())) {
                AiModelConfig patch = new AiModelConfig();
                patch.setDefaultFlag(0);
                patch.setUpdatedTime(new Date());
                AiModelConfigExample up = new AiModelConfigExample();
                up.createCriteria().andIdEqualTo(row.getId());
                aiModelConfigMapper.updateByExampleSelective(patch, up);
            }
        }
    }

    private AiModelConfig requireModel(String modelId) {
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria().andModelIdEqualTo(modelId).andDeletedFlagEqualTo(0);
        AiModelConfig row = aiModelConfigMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "model not found");
        }
        return row;
    }

    private void validateWrite(ReqAiModel req, boolean create) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "body required");
        }
        if (create) {
            if (blank(req.getAbilityType()) || blank(req.getProviderCode()) || blank(req.getModelCode())
                    || blank(req.getModelName())) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "ability/provider/model_code/name required");
            }
        }
    }

    private void apply(AiModelConfig row, ReqAiModel req) {
        if (!blank(req.getAbilityType())) {
            row.setAbilityType(req.getAbilityType().trim());
        }
        if (!blank(req.getProviderCode())) {
            row.setProviderCode(req.getProviderCode().trim());
        }
        if (!blank(req.getModelCode())) {
            row.setModelCode(req.getModelCode().trim());
        }
        if (!blank(req.getModelName())) {
            row.setModelName(req.getModelName().trim());
        }
        if (!blank(req.getQualityLevel())) {
            row.setQualityLevel(req.getQualityLevel().trim());
        } else if (row.getQualityLevel() == null) {
            row.setQualityLevel("STANDARD");
        }
        if (!blank(req.getPriceUnit())) {
            row.setPriceUnit(req.getPriceUnit().trim());
        } else if (row.getPriceUnit() == null) {
            row.setPriceUnit("PER_CALL");
        }
        if (req.getUnitPriceCent() != null) {
            row.setUnitPriceCent(req.getUnitPriceCent());
        } else if (row.getUnitPriceCent() == null) {
            row.setUnitPriceCent(0);
        }
        if (req.getDefaultFlag() != null) {
            row.setDefaultFlag(req.getDefaultFlag());
        } else if (row.getDefaultFlag() == null) {
            row.setDefaultFlag(0);
        }
        if (req.getEnabledFlag() != null) {
            row.setEnabledFlag(req.getEnabledFlag());
        } else if (row.getEnabledFlag() == null) {
            row.setEnabledFlag(1);
        }
        if (req.getTimeoutSec() != null) {
            row.setTimeoutSec(req.getTimeoutSec());
        } else if (row.getTimeoutSec() == null) {
            row.setTimeoutSec(60);
        }
        if (req.getMaxRetry() != null) {
            row.setMaxRetry(req.getMaxRetry());
        } else if (row.getMaxRetry() == null) {
            row.setMaxRetry(1);
        }
        if (req.getConfigJson() != null) {
            row.setConfigJson(req.getConfigJson());
        } else if (row.getConfigJson() == null) {
            row.setConfigJson("{}");
        }
    }

    private Map<String, Object> view(AiModelConfig row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("model_id", row.getModelId());
        item.put("ability_type", row.getAbilityType());
        item.put("ability_type_name", abilityName(row.getAbilityType()));
        item.put("provider_code", row.getProviderCode());
        item.put("model_code", row.getModelCode());
        item.put("model_name", row.getModelName());
        item.put("quality_level", row.getQualityLevel());
        item.put("price_unit", row.getPriceUnit());
        item.put("unit_price_cent", row.getUnitPriceCent());
        item.put("default_flag", row.getDefaultFlag());
        item.put("enabled_flag", row.getEnabledFlag());
        item.put("timeout_sec", row.getTimeoutSec());
        item.put("max_retry", row.getMaxRetry());
        item.put("config_json", row.getConfigJson());
        item.put("updated_time", row.getUpdatedTime());
        return item;
    }

    private static String abilityName(String ability) {
        if ("TEXT".equals(ability)) {
            return "文本";
        }
        if ("IMAGE".equals(ability)) {
            return "图片";
        }
        if ("VIDEO".equals(ability)) {
            return "视频";
        }
        if ("VOICE".equals(ability)) {
            return "配音";
        }
        if ("ASR".equals(ability)) {
            return "语音识别";
        }
        if ("MUSIC".equals(ability)) {
            return "音乐生成";
        }
        if ("COMPOSE".equals(ability)) {
            return "合成";
        }
        return ability;
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private void requireAdmin() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can manage ai models");
        }
    }
}
