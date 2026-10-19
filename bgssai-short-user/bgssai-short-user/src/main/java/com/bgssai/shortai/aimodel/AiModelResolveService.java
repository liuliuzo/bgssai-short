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
    @Autowired(required = false)
    private TokenHubModelCatalog tokenHubModelCatalog;

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
            AiModelConfig hubRow = firstHub(rows);
            if (hubRow != null) {
                return hubRow;
            }
            AiModelConfig hubModel = syntheticHubModel(ability, modelCode.trim());
            if (hubModel != null) {
                return hubModel;
            }
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    TokenHubClientSupport.userFacingMessage("model unavailable: " + modelCode));
        }
        AiModelConfig row = resolveDefault(ability);
        if (row == null) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE);
        }
        return alignDefault(ability, row);
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

    private static AiModelConfig firstHub(List<AiModelConfig> rows) {
        if (rows == null) {
            return null;
        }
        for (AiModelConfig row : rows) {
            if (isHubConfig(row)) {
                return row;
            }
        }
        return null;
    }

    private static boolean isHubConfig(AiModelConfig row) {
        if (row == null) {
            return false;
        }
        String provider = row.getProviderCode() == null ? "" : row.getProviderCode().trim();
        if ("TENCENT_TOKENHUB".equalsIgnoreCase(provider)) {
            return true;
        }
        String base = ShortJson.text(ShortJson.map(row.getConfigJson()).get("base_url")).toLowerCase(java.util.Locale.ROOT);
        return com.bgssai.shortai.aiprovider.TokenHubClientSupport.isOfficialHub(base);
    }

    private AiModelConfig syntheticHubModel(String ability, String modelCode) {
        if (!safeModelId(modelCode)) {
            return null;
        }
        AiModelConfig model = new AiModelConfig();
        model.setAbilityType(ability);
        model.setProviderCode("TENCENT_TOKENHUB");
        model.setModelCode(modelCode);
        model.setModelName(modelCode);
        model.setPriceUnit("PER_CALL");
        model.setUnitPriceCent(0);
        model.setTimeoutSec("VIDEO".equals(ability) ? 900 : 180);
        model.setEnabledFlag(1);
        model.setConfigJson("{\"hub\":\"cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\"}");
        return model;
    }

    private static boolean safeModelId(String modelCode) {
        if (modelCode == null || modelCode.isBlank() || modelCode.length() > 128) {
            return false;
        }
        for (int i = 0; i < modelCode.length(); i++) {
            char c = modelCode.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == ':';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * Uses the configured default only when the gateway catalog lists it for this ability.
     * Kling image and MiniMax voice may move to the registered id of the same vendor.
     * An empty catalog leaves the configured row unchanged.
     */
    private AiModelConfig alignDefault(String ability, AiModelConfig row) {
        if (row == null || tokenHubModelCatalog == null || row.getModelCode() == null) {
            return row;
        }
        List<TokenHubModelCatalog.HubModel> catalog = tokenHubModelCatalog.snapshot();
        if (catalog.isEmpty()) {
            return row;
        }
        String chosen = CatalogModelAlign.choose(ability, row.getModelCode(), catalog);
        if (chosen == null || chosen.isBlank() || chosen.equals(row.getModelCode())) {
            return row;
        }
        AiModelConfig copy = new AiModelConfig();
        copy.setModelId(row.getModelId());
        copy.setAbilityType(ability);
        copy.setProviderCode("TENCENT_TOKENHUB");
        copy.setModelCode(chosen);
        copy.setModelName(chosen);
        copy.setQualityLevel(row.getQualityLevel());
        copy.setPriceUnit(row.getPriceUnit() == null ? "PER_CALL" : row.getPriceUnit());
        copy.setUnitPriceCent(row.getUnitPriceCent() == null ? 0 : row.getUnitPriceCent());
        copy.setDefaultFlag(1);
        copy.setEnabledFlag(1);
        copy.setTimeoutSec(row.getTimeoutSec());
        copy.setMaxRetry(row.getMaxRetry());
        copy.setConfigJson("{\"hub\":\"cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\"}");
        return copy;
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
        String ability = abilityType == null ? "" : abilityType.trim();
        List<Map<String, Object>> list = new ArrayList<>();
        if (tokenHubModelCatalog != null && !ability.isBlank()) {
            list.addAll(hubChoices(ability,tokenHubModelCatalog.modelsFor(ability),rows));
        }
        for (AiModelConfig row : rows) {
            if (!aiProviderRegistry.resolvable(row) || list.stream().anyMatch(item -> row.getModelCode().equals(item.get("model_code")))) {
                continue;
            }
            Map<String, Object> cfg = ShortJson.map(row.getConfigJson());
            String hub = TokenHubClientSupport.resolveHub(cfg, row.getModelCode());
            String region = TokenHubClientSupport.HUB_GLOBAL.equals(hub)
                    ? ModelUsageMeta.INTL
                    : ModelUsageMeta.regionCode(row.getProviderCode(), row.getModelCode());

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
            item.put("commercial_price_unit", item.get("price_unit"));
            item.putAll(tokenHubModelCatalog==null?com.bgssai.shortai.billing.ModelPricing.from(Map.of()):tokenHubModelCatalog.pricingFor(tokenHubModelCatalog.endpointFor(cfg,row.getModelCode()),row.getModelCode()));
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model_list", list);
        return Responses.ok(result);
    }

    private List<Map<String, Object>> hubChoices(String ability,
            List<TokenHubModelCatalog.HubModel> hubModels, List<AiModelConfig> priced) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (TokenHubModelCatalog.HubModel hub : hubModels) {
            AiModelConfig pricedRow = null;
            if (priced != null) {
                for (AiModelConfig row : priced) {
                    if (hub.id().equals(row.getModelCode()) && isHubConfig(row)) {
                        pricedRow = row;
                        break;
                    }
                }
            }

            String region = ModelUsageMeta.CN;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("model_id", "hub-" + ability.toLowerCase(java.util.Locale.ROOT) + "-" + hub.id());
            item.put("ability_type", ability);
            item.put("model_code", hub.id());
            item.put("display_name", hub.id());
            item.put("model_name", hub.id());
            item.put("quality_level", pricedRow == null || pricedRow.getQualityLevel() == null
                    ? "STANDARD" : pricedRow.getQualityLevel());
            item.put("price_unit", pricedRow == null || pricedRow.getPriceUnit() == null
                    ? "PER_CALL" : pricedRow.getPriceUnit());
            item.put("unit_price_cent", pricedRow == null || pricedRow.getUnitPriceCent() == null
                    ? 0 : pricedRow.getUnitPriceCent());
            int defaultFlag = pricedRow != null && Integer.valueOf(1).equals(pricedRow.getDefaultFlag()) ? 1 : 0;
            item.put("default_flag", defaultFlag);
            item.put("region", region);
            item.put("region_label", ModelUsageMeta.regionLabel(region));
            item.put("commercial_price_unit", item.get("price_unit"));
            if (pricedRow != null) {
                var cfg=ShortJson.map(pricedRow.getConfigJson());
                item.putAll(tokenHubModelCatalog.pricingFor(tokenHubModelCatalog.endpointFor(cfg,hub.id()),hub.id()));
            } else item.putAll(hub.pricing());
            list.add(item);
        }
        boolean anyDefault = list.stream().anyMatch(item -> Integer.valueOf(1).equals(item.get("default_flag")));
        if (!anyDefault && !list.isEmpty()) {
            list.get(0).put("default_flag", 1);
        }
        return list;
    }
}
