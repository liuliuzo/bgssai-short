package com.bgssai.shortai.aimodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.TokenHubClientSupport;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class AiModelResolveService {

    @Autowired
    private AiModelConfigMapper aiModelConfigMapper;
    @Autowired
    private AiProviderRegistry aiProviderRegistry;

    public AiModelConfig resolve(String abilityType, String modelCode) {
        if (abilityType == null || abilityType.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "ability_type required");
        }
        String ability = abilityType.trim();
        if (modelCode != null && !modelCode.isBlank()) {
            AiModelConfigExample ex = new AiModelConfigExample();
            ex.createCriteria()
                    .andAbilityTypeEqualTo(ability)
                    .andModelCodeEqualTo(modelCode.trim())
                    .andEnabledFlagEqualTo(1)
                    .andDeletedFlagEqualTo(0);
            // Prefer default + registered provider when catalog has duplicate model_code rows.
            ex.setOrderByClause("default_flag desc, id asc");
            List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
            AiModelConfig picked = pickResolvable(rows);
            if (picked != null) {
                return picked;
            }
            // Graceful fallback: requested model unavailable → ability default.
            AiModelConfig fallback = resolveDefault(ability);
            if (fallback != null) {
                return fallback;
            }
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    TokenHubClientSupport.userFacingMessage("model unavailable: " + modelCode));
        }
        AiModelConfig row = resolveDefault(ability);
        if (row == null) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE);
        }
        return row;
    }

    /**
     * Pick another enabled model for the same ability (retry with fallback_model_flag).
     * Prefers non-matching model_code, then default, then any resolvable row.
     */
    public AiModelConfig resolveFallback(String abilityType, String excludeModelCode) {
        if (abilityType == null || abilityType.isBlank()) {
            return null;
        }
        String ability = abilityType.trim();
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria()
                .andAbilityTypeEqualTo(ability)
                .andEnabledFlagEqualTo(1)
                .andDeletedFlagEqualTo(0);
        ex.setOrderByClause("default_flag desc, id asc");
        List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
        String exclude = excludeModelCode == null ? "" : excludeModelCode.trim();
        AiModelConfig alternate = null;
        for (AiModelConfig row : rows) {
            if (!aiProviderRegistry.resolvable(row)) {
                continue;
            }
            if (!exclude.isBlank() && exclude.equals(row.getModelCode())) {
                continue;
            }
            if (row.getDefaultFlag() != null && row.getDefaultFlag() == 1) {
                return row;
            }
            if (alternate == null) {
                alternate = row;
            }
        }
        return alternate;
    }

    private AiModelConfig resolveDefault(String ability) {
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria()
                .andAbilityTypeEqualTo(ability)
                .andDefaultFlagEqualTo(1)
                .andEnabledFlagEqualTo(1)
                .andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id asc");
        List<AiModelConfig> defaults = aiModelConfigMapper.selectByExample(ex);
        AiModelConfig row = pickResolvable(defaults);
        if (row != null) {
            return row;
        }
        // No default row: any enabled resolvable model for the ability.
        AiModelConfigExample any = new AiModelConfigExample();
        any.createCriteria()
                .andAbilityTypeEqualTo(ability)
                .andEnabledFlagEqualTo(1)
                .andDeletedFlagEqualTo(0);
        any.setOrderByClause("id asc");
        return pickResolvable(aiModelConfigMapper.selectByExample(any));
    }

    private AiModelConfig pickResolvable(List<AiModelConfig> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        for (AiModelConfig row : rows) {
            if (aiProviderRegistry.supports(row.getProviderCode())) {
                return row;
            }
        }
        for (AiModelConfig row : rows) {
            if (aiProviderRegistry.resolvable(row)) {
                return row;
            }
        }
        return null;
    }

    public int costCent(AiModelConfig model, int usageAmount) {
        int price = model.getUnitPriceCent() == null ? 0 : model.getUnitPriceCent();
        int amount = usageAmount <= 0 ? 1 : usageAmount;
        return price * amount;
    }

    /**
     * User catalog: display_name + model_code for picker only. No provider_code / hub brand.
     */
    public ApiResponse listEnabled(String abilityType) {
        AiModelConfigExample ex = new AiModelConfigExample();
        AiModelConfigExample.Criteria c = ex.createCriteria().andEnabledFlagEqualTo(1).andDeletedFlagEqualTo(0);
        if (abilityType != null && !abilityType.isBlank()) {
            c.andAbilityTypeEqualTo(abilityType.trim());
        }
        ex.setOrderByClause("ability_type asc, default_flag desc, quality_level asc, id asc");
        List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
        List<Map<String, Object>> list = new ArrayList<>();
        for (AiModelConfig row : rows) {
            if (!aiProviderRegistry.resolvable(row)) {
                continue;
            }
            Map<String, Object> cfg = ShortJson.map(row.getConfigJson());
            String hub = TokenHubClientSupport.resolveHub(cfg, row.getModelCode());
            String region = TokenHubClientSupport.HUB_GLOBAL.equals(hub)
                    ? ModelUsageMeta.INTL
                    : ModelUsageMeta.regionCode(row.getProviderCode(), row.getModelCode());
            int weight = ModelUsageMeta.tokenMultiplier(row.getModelCode(), row.getProviderCode());
            String displayName = row.getModelName() == null || row.getModelName().isBlank()
                    ? row.getModelCode() : row.getModelName();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("model_id", row.getModelId());
            item.put("ability_type", row.getAbilityType());
            item.put("model_code", row.getModelCode());
            item.put("display_name", displayName);
            item.put("model_name", displayName);
            item.put("quality_level", row.getQualityLevel());
            item.put("price_unit", row.getPriceUnit());
            item.put("unit_price_cent", row.getUnitPriceCent());
            item.put("default_flag", row.getDefaultFlag());
            item.put("region", region);
            item.put("region_label", ModelUsageMeta.regionLabel(region));
            item.put("token_multiplier", weight);
            item.put("usage_hint", ModelUsageMeta.usageHint(weight));
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model_list", list);
        return Responses.ok(result);
    }
}
