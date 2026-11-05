package com.bgssai.shortai.aimodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.CompanyCompatGateway;
import com.bgssai.shortai.aiprovider.OpenAiCompatibleAiProvider;
import com.bgssai.shortai.aiprovider.TokenHubClientSupport;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

@Service
public class AiModelResolveService {

    @Autowired
    private AiModelConfigMapper aiModelConfigMapper;
    @Autowired
    private AiProviderRegistry aiProviderRegistry;
    @Autowired(required = false)
    private TokenHubModelCatalog tokenHubModelCatalog;
    /** Null in unit tests keeps the hub path. The running app always has this bean. */
    @Autowired(required = false)
    private SysSecretReader secrets;

    public AiModelConfig resolve(String abilityType, String modelCode) {
        if (abilityType == null || abilityType.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "ability_type required");
        }
        String ability = abilityType.trim();
        modelCode = rewriteAlias(ability, modelCode);
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
            if (hubKeyReady()) {
                if (hubRow != null) {
                    return hubRow;
                }
                AiModelConfig hubModel = syntheticHubModel(ability, modelCode.trim());
                if (hubModel != null) {
                    return hubModel;
                }
                throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                        closed(ability, modelCode));
            }
            if (compatKeyReady()) {
                AiModelConfig compatRow = firstCompanyCompat(rows);
                if (compatRow != null) {
                    return compatRow;
                }
                if (CompanyCompatGateway.directModel(ability, modelCode.trim())) {
                    AiModelConfig compat = CompanyCompatGateway.synthetic(ability, modelCode.trim());
                    if (compat != null) {
                        return compat;
                    }
                }
            }
            if (hubRow != null) {
                return hubRow;
            }
            AiModelConfig hubModel = syntheticHubModel(ability, modelCode.trim());
            if (hubModel != null) {
                return hubModel;
            }
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, closed(ability, modelCode));
        }
        if (!hubKeyReady() && compatKeyReady()) {
            AiModelConfig compat = resolveCompatDefault(ability);
            if (compat != null) {
                return compat;
            }
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
            if (!aiProviderRegistry.resolvable(row) || !domestic(row)) {
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

    private boolean hubKeyReady() {
        if (secrets == null) {
            return true;
        }
        return !TokenHubClientSupport.resolveApiKey(secrets, TokenHubClientSupport.HUB_CN, null).isBlank();
    }

    private boolean compatKeyReady() {
        return secrets != null && secrets.has(SysSecretReader.AI_OPENAI_COMPAT_API_KEY);
    }

    private static AiModelConfig firstCompanyCompat(List<AiModelConfig> rows) {
        if (rows == null) {
            return null;
        }
        for (AiModelConfig row : rows) {
            if (row == null || !domestic(row)) {
                continue;
            }
            String provider = row.getProviderCode() == null ? "" : row.getProviderCode().trim();
            if (!OpenAiCompatibleAiProvider.CODE.equalsIgnoreCase(provider)) {
                continue;
            }
            String base = ShortJson.text(ShortJson.map(row.getConfigJson()).get("base_url"));
            if (CompanyCompatGateway.allows(base)) {
                return row;
            }
        }
        return null;
    }

    private AiModelConfig resolveCompatDefault(String ability) {
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria()
                .andAbilityTypeEqualTo(ability)
                .andProviderCodeEqualTo(OpenAiCompatibleAiProvider.CODE)
                .andEnabledFlagEqualTo(1)
                .andDeletedFlagEqualTo(0);
        ex.setOrderByClause("default_flag desc, id asc");
        List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
        AiModelConfig listed = firstCompanyCompat(rows);
        if (listed != null) {
            return listed;
        }
        return CompanyCompatGateway.synthetic(ability, CompanyCompatGateway.defaultModel(ability));
    }

    private static AiModelConfig firstHub(List<AiModelConfig> rows) {
        if (rows == null) {
            return null;
        }
        for (AiModelConfig row : rows) {
            if (isHubConfig(row) && domestic(row)) {
                return row;
            }
        }
        return null;
    }

    private static boolean domestic(AiModelConfig row) {
        if (row == null) {
            return false;
        }
        return !TokenHubClientSupport.wouldSelectGlobal(ShortJson.map(row.getConfigJson()), row.getModelCode());
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
        if (!safeModelId(modelCode) || TokenHubClientSupport.wouldSelectGlobal(Map.of(), modelCode)) {
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
            if (domestic(row) && aiProviderRegistry.supports(row.getProviderCode())) {
                return row;
            }
        }
        for (AiModelConfig row : rows) {
            if (domestic(row) && aiProviderRegistry.resolvable(row)) {
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
     * A product alias with no row of that exact code means "the BGSSAI model for this ability".
     * An explicit {@code public_alias} on a row maps back to that row's gateway id.
     * A real gateway id is left unchanged for server-side callers.
     */
    private static String closed(String ability, String modelCode) {
        ProductModelAlias.Face face = ProductModelAlias.forStored(ability, modelCode);
        String name = face.name() == null || face.name().isBlank() ? "BGSSAI" : face.name();
        return "该模型暂未开放：" + name + "，请稍后重试";
    }

    private String rewriteAlias(String ability, String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return modelCode;
        }
        String submitted = modelCode.trim();
        if (hasExactModelCode(ability, submitted)) {
            return submitted;
        }
        if (ProductModelAlias.isCanonical(ability, submitted)) {
            return null;
        }
        AiModelConfig aliased = findByPublicAlias(ability, submitted);
        if (aliased != null && aliased.getModelCode() != null && !aliased.getModelCode().isBlank()) {
            return aliased.getModelCode();
        }
        return submitted;
    }

    private boolean hasExactModelCode(String ability, String code) {
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria()
                .andAbilityTypeEqualTo(ability)
                .andModelCodeEqualTo(code)
                .andEnabledFlagEqualTo(1)
                .andDeletedFlagEqualTo(0);
        List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
        if (rows == null) {
            return false;
        }
        for (AiModelConfig row : rows) {
            if (row != null && code.equals(row.getModelCode())) {
                return true;
            }
        }
        return false;
    }

    private AiModelConfig findByPublicAlias(String ability, String alias) {
        if (!ProductModelAlias.isSafeAlias(alias)) {
            return null;
        }
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria()
                .andAbilityTypeEqualTo(ability)
                .andEnabledFlagEqualTo(1)
                .andDeletedFlagEqualTo(0);
        ex.setOrderByClause("default_flag desc, id asc");
        List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
        if (rows == null) {
            return null;
        }
        for (AiModelConfig row : rows) {
            if (row != null && domestic(row) && alias.equalsIgnoreCase(ProductModelAlias.explicitAlias(row.getConfigJson()))) {
                return row;
            }
        }
        return null;
    }

    /**
     * User catalog: one BGSSAI alias per product model. No provider, gateway id, or vendor name.
     */
    public ApiResponse listEnabled(String abilityType) {
        AiModelConfigExample ex = new AiModelConfigExample();
        AiModelConfigExample.Criteria c = ex.createCriteria().andEnabledFlagEqualTo(1).andDeletedFlagEqualTo(0);
        if (abilityType != null && !abilityType.isBlank()) {
            c.andAbilityTypeEqualTo(abilityType.trim());
        }
        ex.setOrderByClause("ability_type asc, default_flag desc, quality_level asc, id asc");
        List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
        java.util.Map<String, AiModelConfig> chosen = new java.util.LinkedHashMap<>();
        java.util.Map<String, ProductModelAlias.Face> faces = new java.util.LinkedHashMap<>();
        if (rows != null) {
            for (AiModelConfig row : rows) {
                if (!domestic(row) || !aiProviderRegistry.resolvable(row)) {
                    continue;
                }
                ProductModelAlias.Face face = ProductModelAlias.faceOf(
                        row.getAbilityType(), row.getModelCode(), row.getConfigJson());
                if (face == null || face.code() == null || face.code().isBlank() || chosen.containsKey(face.code())) {
                    continue;
                }
                chosen.put(face.code(), row);
                faces.put(face.code(), face);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (java.util.Map.Entry<String, AiModelConfig> entry : chosen.entrySet()) {
            AiModelConfig row = entry.getValue();
            ProductModelAlias.Face face = faces.get(entry.getKey());
            Map<String, Object> cfg = ShortJson.map(row.getConfigJson());
            String region = ModelUsageMeta.regionCode(row.getProviderCode(), row.getModelCode());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("model_id", face.code());
            item.put("ability_type", row.getAbilityType());
            item.put("model_code", face.code());
            item.put("display_name", face.name());
            item.put("model_name", face.name());
            item.put("quality_level", row.getQualityLevel());
            item.put("price_unit", row.getPriceUnit());
            item.put("unit_price_cent", row.getUnitPriceCent());
            item.put("default_flag", row.getDefaultFlag());
            item.put("region", region);
            item.put("region_label", ModelUsageMeta.regionLabel(region));
            item.put("commercial_price_unit", item.get("price_unit"));
            item.putAll(tokenHubModelCatalog == null
                    ? com.bgssai.shortai.billing.ModelPricing.from(Map.of())
                    : tokenHubModelCatalog.pricingFor(
                            tokenHubModelCatalog.endpointFor(cfg, row.getModelCode()), row.getModelCode()));
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model_list", list);
        return Responses.ok(result);
    }
}
