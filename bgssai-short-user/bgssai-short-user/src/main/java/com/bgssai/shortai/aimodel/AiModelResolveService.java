package com.bgssai.shortai.aimodel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.CompanyCompatGateway;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProvider;
import com.bgssai.shortai.aiprovider.OpenAiCompatibleAiProvider;
import com.bgssai.shortai.aiprovider.TokenHubClientSupport;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.model.UserAiModelKey;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderCatalog;
import com.bgssai.shortai.useraimodel.service.UserAiProviderCatalog.ProviderSpec;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;

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
    @Autowired(required = false)
    private UserAiModelKeyService userAiModelKeyService;
    @Autowired(required = false)
    private UserAiProviderKeyService userAiProviderKeyService;

    void setUserAiProviderKeyService(UserAiProviderKeyService userAiProviderKeyService) {
        this.userAiProviderKeyService = userAiProviderKeyService;
    }

    public AiModelConfig resolve(String abilityType, String modelCode) {
        return resolveForUser(abilityType, modelCode, effectiveUserId());
    }

    public AiModelConfig resolveForUser(String abilityType, String modelCode, String explicitUserId) {
        if (abilityType == null || abilityType.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "ability_type required");
        }
        String ability = abilityType.trim();
        String userId = effectiveUserId(explicitUserId);
        if (!"COMPOSE".equalsIgnoreCase(ability)) {
            if (UserAiModelSupport.isUserKeyCode(modelCode) && (userId == null || userId.isBlank())) {
                throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "请先登录后使用自有模型密钥");
            }
            if (userAiModelKeyService != null && userId != null && !userId.isBlank()) {
                AiModelConfig userModel = userAiModelKeyService.resolveUserModel(userId, ability, modelCode);
                if (userModel != null) {
                    return userModel;
                }
            }
            if (userAiProviderKeyService != null && userId != null && !userId.isBlank()
                    && modelCode != null
                    && modelCode.trim().toLowerCase(Locale.ROOT).startsWith(UserAiModelSupport.PROVIDER_KEY_PREFIX)) {
                return resolveExplicitProviderByokModel(ability, modelCode.trim(), userId);
            }
        }
        if ("VIDEO".equals(ability) && ProductModelAlias.isCanonical(ability, modelCode)) {
            AiModelConfig company = companyVideo(enabledForAbility(ability));
            if (company == null) {
                throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, closed(ability, modelCode));
            }
            return company;
        }
        modelCode = rewriteAlias(ability, modelCode);
        if (modelCode != null && ImageConditionedVideoProvider.MODEL_CODE.equalsIgnoreCase(modelCode.trim())) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, ImageConditionedVideoProtocol.UNAVAILABLE);
        }
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
            AiModelConfig byokMatch = findProviderByokRow(rows, userId);
            if (byokMatch != null) {
                return byokMatch;
            }
            AiModelConfig localRow = firstLocalTool(rows);
            if (localRow != null) {
                return applyProviderByokIfActive(localRow, userId);
            }
            AiModelConfig hubRow = firstHub(rows);
            if (hubKeyReady()) {
                if (hubRow != null) {
                    return applyProviderByokIfActive(hubRow, userId);
                }
                if ("ASR".equals(ability) && asrCompatKeyReady()) {
                    AiModelConfig compatRow = firstCompanyCompat(rows);
                    if (compatRow != null) {
                        return applyProviderByokIfActive(compatRow, userId);
                    }
                    if (CompanyCompatGateway.directModel(ability, modelCode.trim())) {
                        AiModelConfig compat = CompanyCompatGateway.synthetic(ability, modelCode.trim());
                        if (compat != null) {
                            return applyProviderByokIfActive(compat, userId);
                        }
                    }
                }
                AiModelConfig hubModel = syntheticHubModel(ability, modelCode.trim());
                if (hubModel != null) {
                    return hubModel;
                }
                AiModelConfig inferred = resolveByInferredProviderByok(ability, modelCode.trim(), userId);
                if (inferred != null) {
                    return inferred;
                }
                throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                        closed(ability, modelCode));
            }
            if ("ASR".equals(ability) ? asrCompatKeyReady() : compatKeyReady()) {
                AiModelConfig compatRow = firstCompanyCompat(rows);
                if (compatRow != null) {
                    return applyProviderByokIfActive(compatRow, userId);
                }
                if (CompanyCompatGateway.directModel(ability, modelCode.trim())) {
                    AiModelConfig compat = CompanyCompatGateway.synthetic(ability, modelCode.trim());
                    if (compat != null) {
                        return applyProviderByokIfActive(compat, userId);
                    }
                }
            }
            if (hubRow != null) {
                return applyProviderByokIfActive(hubRow, userId);
            }
            AiModelConfig hubModel = syntheticHubModel(ability, modelCode.trim());
            if (hubModel != null) {
                return hubModel;
            }
            AiModelConfig inferred = resolveByInferredProviderByok(ability, modelCode.trim(), userId);
            if (inferred != null) {
                return inferred;
            }
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, closed(ability, modelCode));
        }
        if ("ASR".equals(ability) ? asrCompatKeyReady() : (!hubKeyReady() && compatKeyReady())) {
            AiModelConfig compat = resolveCompatDefault(ability);
            if (compat != null) {
                return applyProviderByokIfActive(compat, userId);
            }
        }
        AiModelConfig row = resolveDefault(ability);
        if (row == null) {
            AiModelConfig fallbackByok = resolveAnyActiveProviderByok(ability, userId);
            if (fallbackByok != null) {
                return fallbackByok;
            }
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE);
        }
        return applyProviderByokIfActive(alignDefault(ability, row), userId);
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
            if (!aiProviderRegistry.resolvable(row) || !domestic(row) || imageConditioned(row)) {
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

    private boolean asrCompatKeyReady() {
        return compatKeyReady() || (secrets != null && secrets.has(SysSecretReader.AI_IMAGE_TO_VIDEO_API_KEY));
    }

    private AiModelConfig firstLocalTool(List<AiModelConfig> rows) {
        if (rows == null) {
            return null;
        }
        for (AiModelConfig row : rows) {
            if (row == null || !domestic(row) || isHubConfig(row) || imageConditioned(row)) {
                continue;
            }
            String provider = row.getProviderCode() == null ? "" : row.getProviderCode().trim();
            if (OpenAiCompatibleAiProvider.CODE.equalsIgnoreCase(provider)
                    || UserAiProviderCatalog.isRuntimeSupported(provider)
                    || "PIXVERSE".equalsIgnoreCase(provider)
                    || "XAI".equalsIgnoreCase(provider)) {
                continue;
            }
            if (aiProviderRegistry.supports(provider) || aiProviderRegistry.resolvable(row)) {
                return row;
            }
        }
        return null;
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

    private static boolean imageConditioned(AiModelConfig row) {
        return row != null && ImageConditionedVideoProvider.CODE.equalsIgnoreCase(
                row.getProviderCode() == null ? "" : row.getProviderCode().trim());
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

    private List<AiModelConfig> enabledForAbility(String ability) {
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria().andAbilityTypeEqualTo(ability).andEnabledFlagEqualTo(1).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("default_flag desc, id asc");
        return aiModelConfigMapper.selectByExample(ex);
    }

    /** Shared by the public catalog and explicit selection; never invent price or credentials. */
    private AiModelConfig companyVideo(List<AiModelConfig> rows) {
        AiModelConfig legacy = null;
        if (rows == null) {
            return null;
        }
        List<AiModelConfig> ordered = rows.stream().filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt((AiModelConfig row) -> Integer.valueOf(1).equals(row.getDefaultFlag()) ? 0 : 1)
                        .thenComparing(AiModelConfig::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AiModelConfig::getModelId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (AiModelConfig row : ordered) {
            if (!"VIDEO".equals(row.getAbilityType()) || !Integer.valueOf(1).equals(row.getEnabledFlag())
                    || !Integer.valueOf(0).equals(row.getDeletedFlag()) || !isHubConfig(row)
                    || !domestic(row) || !aiProviderRegistry.resolvable(row)) {
                continue;
            }
            if ("bgssai-video".equals(row.getModelCode())) {
                return row;
            }
            if (legacy == null && "m-tokenhub-video-cogvideox".equals(row.getModelId())
                    && "TENCENT_TOKENHUB".equals(row.getProviderCode())
                    && "cogvideox".equals(row.getModelCode())
                    && "cn".equals(ShortJson.text(ShortJson.map(row.getConfigJson()).get("hub")))
                    && "bgssai-video".equals(ProductModelAlias.explicitAlias(row.getConfigJson()))) {
                legacy = row;
            }
        }
        if (legacy == null) {
            return null;
        }
        AiModelConfig normalized = new AiModelConfig();
        BeanUtils.copyProperties(legacy, normalized);
        normalized.setModelCode("bgssai-video");
        return normalized;
    }

    private AiModelConfig syntheticHubModel(String ability, String modelCode) {
        if ("COMPOSE".equalsIgnoreCase(ability)
                || (modelCode != null && modelCode.trim().toLowerCase(java.util.Locale.ROOT).startsWith("ffmpeg"))) {
            return null;
        }
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
        model.setTimeoutSec("MUSIC".equals(ability) ? 240 : ("VIDEO".equals(ability) ? 900 : 180));
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
            if (domestic(row) && !imageConditioned(row) && aiProviderRegistry.supports(row.getProviderCode())) {
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
        String userId = effectiveUserId();
        List<Map<String, Object>> list = new ArrayList<>();
        if (userAiModelKeyService != null && userId != null && !userId.isBlank()) {
            List<UserAiModelKey> userKeys = userAiModelKeyService.listEnabledRows(userId, abilityType);
            for (UserAiModelKey keyRow : userKeys) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("model_id", keyRow.getKeyId());
                item.put("ability_type", keyRow.getAbilityType());
                item.put("model_code", keyRow.getKeyId());
                item.put("upstream_model_code", keyRow.getModelCode());
                item.put("display_name", keyRow.getModelName());
                item.put("model_name", keyRow.getModelName());
                item.put("provider_code", keyRow.getProviderCode());
                item.put("provider_name", UserAiModelSupport.providerName(keyRow.getProviderCode()));
                item.put("quality_level", "STANDARD");
                item.put("price_unit", "PER_CALL");
                item.put("unit_price_cent", 0);
                item.put("default_flag", Integer.valueOf(1).equals(keyRow.getDefaultModel()) ? 1 : 0);
                item.put("credential_source", UserAiModelSupport.CREDENTIAL_SOURCE_USER);
                item.put("user_key_flag", 1);
                item.put("billing_multiplier", null);
                item.put("pricing_status", "USER_KEY_FREE");
                item.put("currency", "CNY");
                list.add(item);
            }
        }
        Map<String, ResolvedProviderKey> activeProviderKeys = (userAiProviderKeyService == null || userId == null
                || userId.isBlank()) ? Map.of() : userAiProviderKeyService.activeKeyMap(userId);
        appendProviderByokListItems(list, abilityType, activeProviderKeys);
        AiModelConfigExample ex = new AiModelConfigExample();
        AiModelConfigExample.Criteria c = ex.createCriteria().andEnabledFlagEqualTo(1).andDeletedFlagEqualTo(0);
        if (abilityType != null && !abilityType.isBlank()) {
            c.andAbilityTypeEqualTo(abilityType.trim());
        }
        ex.setOrderByClause("ability_type asc, default_flag desc, quality_level asc, id asc");
        List<AiModelConfig> rows = aiModelConfigMapper.selectByExample(ex);
        AiModelConfig companyVideo = companyVideo(rows);
        java.util.Map<String, AiModelConfig> chosen = new java.util.LinkedHashMap<>();
        java.util.Map<String, ProductModelAlias.Face> faces = new java.util.LinkedHashMap<>();
        if (companyVideo != null) {
            chosen.put("bgssai-video", companyVideo);
            faces.put("bgssai-video", ProductModelAlias.canonical("VIDEO"));
        }
        if (rows != null) {
            for (AiModelConfig row : rows) {
                boolean hasProviderByok = findActiveProviderKey(activeProviderKeys, row.getProviderCode()) != null;
                if (!hasProviderByok && (!domestic(row) || imageConditioned(row) || !aiProviderRegistry.resolvable(row))) {
                    continue;
                }
                ProductModelAlias.Face face = ProductModelAlias.faceOf(
                        row.getAbilityType(), row.getModelCode(), row.getConfigJson());
                if ("VIDEO".equals(row.getAbilityType()) && face != null
                        && ProductModelAlias.isCanonical("VIDEO", face.code())) {
                    if (companyVideo == null) {
                        continue;
                    }
                    row = companyVideo;
                    face = ProductModelAlias.canonical("VIDEO");
                }
                if (face == null || face.code() == null || face.code().isBlank() || chosen.containsKey(face.code())) {
                    continue;
                }
                chosen.put(face.code(), row);
                faces.put(face.code(), face);
            }
        }
        for (java.util.Map.Entry<String, AiModelConfig> entry : chosen.entrySet()) {
            AiModelConfig row = entry.getValue();
            ProductModelAlias.Face face = faces.get(entry.getKey());
            Map<String, Object> cfg = ShortJson.map(row.getConfigJson());
            ResolvedProviderKey matchedByok = findActiveProviderKey(activeProviderKeys, row.getProviderCode());
            String region = ModelUsageMeta.regionCode(row.getProviderCode(), row.getModelCode());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("model_id", face.code());
            item.put("ability_type", row.getAbilityType());
            item.put("model_code", face.code());
            item.put("display_name", face.name());
            item.put("model_name", face.name());
            item.put("quality_level", row.getQualityLevel());
            item.put("price_unit", row.getPriceUnit());
            item.put("unit_price_cent", matchedByok != null ? 0 : row.getUnitPriceCent());
            item.put("default_flag", row.getDefaultFlag());
            item.put("credential_source", matchedByok != null
                    ? UserAiModelSupport.CREDENTIAL_SOURCE_USER : "PLATFORM");
            item.put("user_key_flag", matchedByok != null ? 1 : 0);
            item.put("region", region);
            item.put("region_label", ModelUsageMeta.regionLabel(region));
            item.put("commercial_price_unit", item.get("price_unit"));
            if (matchedByok != null) {
                item.put("pricing_status", "USER_KEY_FREE");
                item.put("provider_code", matchedByok.providerCode());
            } else {
                item.putAll(tokenHubModelCatalog == null
                        ? com.bgssai.shortai.billing.ModelPricing.from(Map.of())
                        : tokenHubModelCatalog.pricingFor(
                                tokenHubModelCatalog.endpointFor(cfg, row.getModelCode()), row.getModelCode()));
            }
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model_list", list);
        result.put("only_user_key", 0);
        return Responses.ok(result);
    }

    private String effectiveUserId() {
        return effectiveUserId(null);
    }

    private String effectiveUserId(String explicitUserId) {
        if (explicitUserId != null && !explicitUserId.isBlank()) {
            return explicitUserId.trim();
        }
        if (userAiModelKeyService != null) {
            String fromService = userAiModelKeyService.currentUserOrScopeUserId();
            if (fromService != null && !fromService.isBlank()) {
                return fromService.trim();
            }
        }
        String ctx = com.bgssai.shortai.common.aop.UserContext.getUserId();
        if (ctx != null && !ctx.isBlank()) {
            return ctx.trim();
        }
        com.bgssai.shortai.billing.UsageScope scope = com.bgssai.shortai.billing.UsageScope.current();
        return scope != null && scope.userId() != null ? scope.userId().trim() : null;
    }

    private ResolvedProviderKey findActiveProviderKey(Map<String, ResolvedProviderKey> activeMap,
            String rawProviderCode) {
        if (activeMap == null || activeMap.isEmpty() || rawProviderCode == null || rawProviderCode.isBlank()) {
            return null;
        }
        String canonical = UserAiProviderCatalog.normalizeRuntimeProviderCode(rawProviderCode);
        return activeMap.get(canonical);
    }

    private AiModelConfig findProviderByokRow(List<AiModelConfig> rows, String userId) {
        if (userAiProviderKeyService == null || rows == null || rows.isEmpty()
                || userId == null || userId.isBlank()) {
            return null;
        }
        for (AiModelConfig row : rows) {
            if (row == null || !UserAiProviderCatalog.isRuntimeSupported(row.getProviderCode())) {
                continue;
            }
            String canonical = UserAiProviderCatalog.normalizeRuntimeProviderCode(row.getProviderCode());
            Optional<ResolvedProviderKey> byok = userAiProviderKeyService.findActiveKey(userId, canonical);
            if (byok.isPresent()) {
                ResolvedProviderKey key = byok.get();
                return UserAiModelSupport.withProviderByokContext(row, userId, key.providerCode(), key.baseUrl());
            }
        }
        return null;
    }

    private AiModelConfig applyProviderByokIfActive(AiModelConfig row, String userId) {
        if (row == null || userAiProviderKeyService == null || userId == null || userId.isBlank()) {
            return row;
        }
        if (UserAiModelSupport.isUserKeyModel(row)
                || !UserAiProviderCatalog.isRuntimeSupported(row.getProviderCode())) {
            return row;
        }
        String canonical = UserAiProviderCatalog.normalizeRuntimeProviderCode(row.getProviderCode());
        Optional<ResolvedProviderKey> byok = userAiProviderKeyService.findActiveKey(userId, canonical);
        if (byok.isEmpty()) {
            return row;
        }
        ResolvedProviderKey key = byok.get();
        return UserAiModelSupport.withProviderByokContext(row, userId, key.providerCode(), key.baseUrl());
    }

    private AiModelConfig resolveExplicitProviderByokModel(String ability, String code, String userId) {
        String rawSuffix = code.substring(UserAiModelSupport.PROVIDER_KEY_PREFIX.length()).trim();
        String providerCode = UserAiProviderCatalog.normalizeProviderCode(rawSuffix);
        Optional<ResolvedProviderKey> byok = providerCode.isBlank()
                ? Optional.empty()
                : userAiProviderKeyService.findActiveKey(userId, providerCode);
        if (byok.isEmpty()) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "请先启用对应的自有厂商 API Key");
        }
        ResolvedProviderKey key = byok.get();
        ProviderSpec spec = UserAiProviderCatalog.get(key.providerCode());
        String defaultModel = defaultModelForAbility(spec, ability);
        return buildSyntheticByokModel(ability, defaultModel, key, userId);
    }

    private AiModelConfig resolveByInferredProviderByok(String ability, String modelCode, String userId) {
        if (userAiProviderKeyService == null || userId == null || userId.isBlank()
                || modelCode == null || modelCode.isBlank()) {
            return null;
        }
        String inferred = inferProviderFromModelCode(modelCode);
        if (inferred.isBlank()) {
            return null;
        }
        Optional<ResolvedProviderKey> byok = userAiProviderKeyService.findActiveKey(userId, inferred);
        if (byok.isEmpty()) {
            return null;
        }
        return buildSyntheticByokModel(ability, modelCode, byok.get(), userId);
    }

    private AiModelConfig resolveAnyActiveProviderByok(String ability, String userId) {
        if (userAiProviderKeyService == null || userId == null || userId.isBlank()) {
            return null;
        }
        for (ResolvedProviderKey key : userAiProviderKeyService.listActiveKeysForUser(userId)) {
            ProviderSpec spec = UserAiProviderCatalog.get(key.providerCode());
            if (spec == null) {
                continue;
            }
            if ("VIDEO".equalsIgnoreCase(ability) && !spec.defaultVideoModel().isBlank()) {
                return buildSyntheticByokModel(ability, spec.defaultVideoModel(), key, userId);
            }
            if ("TEXT".equalsIgnoreCase(ability) && !spec.defaultChatModel().isBlank()) {
                return buildSyntheticByokModel(ability, spec.defaultChatModel(), key, userId);
            }
        }
        return null;
    }

    private AiModelConfig buildSyntheticByokModel(String ability, String modelCode, ResolvedProviderKey key,
            String userId) {
        AiModelConfig m = new AiModelConfig();
        String providerCode = key.providerCode();
        m.setModelId(UserAiModelSupport.PROVIDER_KEY_PREFIX + providerCode.toLowerCase(Locale.ROOT));
        m.setAbilityType(ability);
        m.setProviderCode(providerCode);
        m.setModelCode(modelCode);
        ProviderSpec spec = UserAiProviderCatalog.get(providerCode);
        m.setModelName((spec != null ? spec.providerName() : providerCode) + " " + modelCode);
        m.setEnabledFlag(1);
        m.setDeletedFlag(0);
        m.setDefaultFlag(0);
        m.setUnitPriceCent(0);
        m.setPriceUnit("PER_CALL");
        m.setTimeoutSec("VIDEO".equalsIgnoreCase(ability) ? 600 : 180);
        return UserAiModelSupport.withProviderByokContext(m, userId, providerCode, key.baseUrl());
    }

    private void appendProviderByokListItems(List<Map<String, Object>> list, String abilityType,
            Map<String, ResolvedProviderKey> activeProviderKeys) {
        if (activeProviderKeys == null || activeProviderKeys.isEmpty()) {
            return;
        }
        String normalizedAbility = abilityType == null || abilityType.isBlank()
                ? "TEXT"
                : abilityType.trim().toUpperCase(Locale.ROOT);
        for (ResolvedProviderKey key : activeProviderKeys.values()) {
            ProviderSpec spec = UserAiProviderCatalog.get(key.providerCode());
            if (spec == null) {
                continue;
            }
            String defaultModel = defaultModelForAbility(spec, normalizedAbility);
            if (defaultModel.isBlank()) {
                continue;
            }
            String bpkCode = UserAiModelSupport.PROVIDER_KEY_PREFIX + spec.providerCode().toLowerCase(Locale.ROOT);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("model_id", bpkCode);
            item.put("ability_type", normalizedAbility);
            item.put("model_code", bpkCode);
            item.put("upstream_model_code", defaultModel);
            item.put("display_name", spec.providerName() + " (" + defaultModel + ")");
            item.put("model_name", spec.providerName() + " (" + defaultModel + ")");
            item.put("provider_code", spec.providerCode());
            item.put("provider_name", spec.providerName());
            item.put("quality_level", "STANDARD");
            item.put("price_unit", "PER_CALL");
            item.put("unit_price_cent", 0);
            item.put("default_flag", 0);
            item.put("credential_source", UserAiModelSupport.CREDENTIAL_SOURCE_USER);
            item.put("user_key_flag", 1);
            item.put("pricing_status", "USER_KEY_FREE");
            item.put("currency", "CNY");
            list.add(item);
        }
    }

    private static String defaultModelForAbility(ProviderSpec spec, String ability) {
        if (spec == null) {
            return "";
        }
        String ab = ability == null ? "" : ability.trim().toUpperCase(Locale.ROOT);
        if ("VIDEO".equals(ab)) {
            return spec.defaultVideoModel();
        }
        if ("TEXT".equals(ab)) {
            return spec.defaultChatModel();
        }
        if ("ASR".equals(ab) && "OPENAI".equals(spec.providerCode())) {
            return "gpt-transcribe";
        }
        if ("IMAGE".equals(ab)) {
            return switch (spec.providerCode()) {
                case "KLING" -> "kling-v2";
                case "MINIMAX" -> "image-01";
                case "OPENAI" -> "gpt-image-2.6-sunburst";
                case "DASHSCOPE" -> "wanx2.1-t2i-turbo";
                case "ZHIPU" -> "cogview-4";
                default -> "";
            };
        }
        return "";
    }

    private static String inferProviderFromModelCode(String modelCode) {
        String lower = modelCode.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("deepseek")) return "DEEPSEEK";
        if (lower.startsWith("minimax") || lower.startsWith("abab") || lower.startsWith("hailuo") || "image-01".equals(lower)) return "MINIMAX";
        if (lower.startsWith("kling")) return "KLING";
        if (lower.startsWith("qwen") || lower.startsWith("wanx")) return "DASHSCOPE";
        if (lower.startsWith("glm") || lower.startsWith("cogvideo") || lower.startsWith("cogview")) return "ZHIPU";
        if (lower.startsWith("moonshot") || lower.startsWith("kimi")) return "MOONSHOT";
        if (lower.startsWith("doubao") || lower.startsWith("seedance")) return "VOLCENGINE";
        if (lower.startsWith("hunyuan") || lower.startsWith("hy-")) return "HUNYUAN";
        if (lower.startsWith("gpt-") || lower.startsWith("o1") || lower.startsWith("o3") || lower.startsWith("o4") || lower.startsWith("sora") || lower.startsWith("dall-e")) return "OPENAI";
        if (lower.startsWith("claude")) return "ANTHROPIC";
        if (lower.startsWith("gemini") || lower.startsWith("veo") || lower.startsWith("imagen")) return "GOOGLE";
        return "";
    }
}
