package com.bgssai.shortai.useraimodel.service;

import java.net.InetAddress;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.bgssai.shortai.aimodel.ProductModelAlias;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.UserAiModelKey;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.dto.ModelPresetOption;
import com.bgssai.shortai.useraimodel.dto.ProviderAbilityPreset;
import com.bgssai.shortai.useraimodel.dto.ProviderPresetItem;

public final class UserAiModelSupport {

    public static final String USER_KEY_PREFIX = "umk-";
    public static final String CREDENTIAL_SOURCE_USER = "USER_KEY";
    public static final String PROVIDER_XAI = "XAI";
    public static final String PROVIDER_XAI_NAME = "Grok (xAI)";
    public static final String DEFAULT_XAI_BASE_URL = "https://api.x.ai/v1";

    public static final Set<String> MODALITIES = Set.of("TEXT", "IMAGE", "VIDEO", "AUDIO");
    public static final Set<String> ABILITIES = Set.of("TEXT", "IMAGE", "VIDEO", "VOICE", "ASR", "MUSIC");

    private UserAiModelSupport() {
    }

    public static boolean isUserKeyCode(String code) {
        return code != null && code.trim().toLowerCase(Locale.ROOT).startsWith(USER_KEY_PREFIX);
    }

    public static boolean isUserKeyModel(AiModelConfig model) {
        if (model == null) {
            return false;
        }
        if (isUserKeyCode(model.getModelId())) {
            return true;
        }
        Map<String, Object> cfg = ShortJson.map(model.getConfigJson());
        return CREDENTIAL_SOURCE_USER.equalsIgnoreCase(ShortJson.text(cfg.get("credential_source")));
    }

    public static String maskApiKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    public static String normalizeProvider(String providerCode) {
        String code = providerCode == null ? "" : providerCode.trim().toUpperCase(Locale.ROOT);
        if (code.isBlank()) {
            return PROVIDER_XAI;
        }
        if (!PROVIDER_XAI.equals(code)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "unsupported provider_code: " + code);
        }
        return code;
    }

    public static String providerName(String providerCode) {
        if (PROVIDER_XAI.equalsIgnoreCase(providerCode)) {
            return PROVIDER_XAI_NAME;
        }
        return providerCode == null ? "" : providerCode;
    }

    public static String normalizeAbility(String modalityType, String abilityType) {
        String ability = abilityType == null ? "" : abilityType.trim().toUpperCase(Locale.ROOT);
        String modality = modalityType == null ? "" : modalityType.trim().toUpperCase(Locale.ROOT);
        if (ability.isBlank()) {
            if ("AUDIO".equals(modality)) {
                ability = "VOICE";
            } else {
                ability = modality;
            }
        }
        if (!ABILITIES.contains(ability)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "invalid ability_type");
        }
        return ability;
    }

    public static String modalityForAbility(String abilityType) {
        if ("VOICE".equals(abilityType) || "ASR".equals(abilityType) || "MUSIC".equals(abilityType)) {
            return "AUDIO";
        }
        if ("IMAGE".equals(abilityType) || "VIDEO".equals(abilityType)) {
            return abilityType;
        }
        return "TEXT";
    }

    public static String abilityLabel(String abilityType) {
        if ("TEXT".equals(abilityType)) {
            return "文本";
        }
        if ("IMAGE".equals(abilityType)) {
            return "图片";
        }
        if ("VIDEO".equals(abilityType)) {
            return "视频";
        }
        if ("VOICE".equals(abilityType)) {
            return "配音";
        }
        if ("ASR".equals(abilityType)) {
            return "语音识别";
        }
        if ("MUSIC".equals(abilityType)) {
            return "背景音乐";
        }
        return abilityType == null ? "" : abilityType;
    }

    public static String normalizeBaseUrl(String rawBaseUrl, String providerCode) {
        String value = rawBaseUrl == null || rawBaseUrl.isBlank() ? defaultBaseUrl(providerCode) : rawBaseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        validatePublicHttpsUrl(value, "base_url");
        return value;
    }

    public static String defaultBaseUrl(String providerCode) {
        if (PROVIDER_XAI.equalsIgnoreCase(providerCode)) {
            return DEFAULT_XAI_BASE_URL;
        }
        return DEFAULT_XAI_BASE_URL;
    }

    public static void validatePublicHttpsUrl(String rawUrl, String fieldName) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, fieldName + " required");
        }
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, fieldName + " invalid");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, fieldName + " must use https");
        }
        if (uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            if ("base_url".equals(fieldName)) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, fieldName + " must not contain userinfo, query or fragment");
            }
        }
        if (uri.getUserInfo() != null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, fieldName + " must not contain userinfo");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, fieldName + " host required");
        }
        String lowerHost = host.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(lowerHost) || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
            throw new BizException(CommonErrorCode.RULE_REJECT, fieldName + " private host rejected");
        }
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            if (addrs == null || addrs.length == 0) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, fieldName + " host unresolvable");
            }
            for (InetAddress addr : addrs) {
                if (isForbiddenAddress(addr)) {
                    throw new BizException(CommonErrorCode.RULE_REJECT, fieldName + " private address rejected");
                }
            }
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, fieldName + " host resolution failed");
        }
    }

    private static boolean isForbiddenAddress(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            return true;
        }
        byte[] raw = addr.getAddress();
        if (raw.length == 4) {
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;
            // 100.64.0.0/10 carrier-grade NAT and 169.254.0.0/16 metadata link-local
            if (b0 == 100 && b1 >= 64 && b1 <= 127) {
                return true;
            }
            if (b0 == 169 && b1 == 254) {
                return true;
            }
            if (b0 == 0) {
                return true;
            }
        }
        if (raw.length == 16) {
            int b0 = raw[0] & 0xFF;
            // fc00::/7 unique local address
            if ((b0 & 0xFE) == 0xFC) {
                return true;
            }
        }
        return false;
    }

    public static AiModelConfig toTransientModelConfig(UserAiModelKey row) {
        if (row == null) {
            return null;
        }
        AiModelConfig model = new AiModelConfig();
        model.setModelId(row.getKeyId());
        model.setAbilityType(row.getAbilityType());
        model.setProviderCode(row.getProviderCode());
        model.setModelCode(row.getModelCode());
        model.setModelName(row.getModelName());
        model.setQualityLevel("STANDARD");
        model.setPriceUnit("PER_CALL");
        model.setUnitPriceCent(0);
        model.setDefaultFlag(Integer.valueOf(1).equals(row.getDefaultModel()) ? 1 : 0);
        model.setEnabledFlag(Integer.valueOf(1).equals(row.getEnabled()) ? 1 : 0);
        String ability = row.getAbilityType();
        int timeout = "VIDEO".equals(ability) ? 900 : ("IMAGE".equals(ability) ? 600 : 180);
        model.setTimeoutSec(timeout);
        model.setMaxRetry(1);
        model.setDeletedFlag(0);

        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("credential_source", CREDENTIAL_SOURCE_USER);
        cfg.put("user_key_id", row.getKeyId());
        cfg.put("user_id", row.getUserId());
        cfg.put("base_url", row.getBaseUrl());
        cfg.put("display_name", row.getModelName());
        model.setConfigJson(ShortJson.toJson(cfg));
        return model;
    }

    public static void presentModel(Map<String, Object> target, String ability, AiModelConfig model) {
        if (target == null || model == null) {
            return;
        }
        if (isUserKeyModel(model)) {
            String keyId = model.getModelId() != null && !model.getModelId().isBlank()
                    ? model.getModelId() : model.getModelCode();
            String displayName = model.getModelName() != null && !model.getModelName().isBlank()
                    ? model.getModelName() : model.getModelCode();
            target.put("model_code", keyId);
            target.put("display_name", displayName);
            target.put("model_name", displayName);
            target.put("credential_source", CREDENTIAL_SOURCE_USER);
            return;
        }
        ProductModelAlias.hideStoredModel(target, ability, model.getModelCode());
    }

    public static String displayNameOf(AiModelConfig model) {
        if (model == null) {
            return "BGSSAI";
        }
        if (isUserKeyModel(model)) {
            if (model.getModelName() != null && !model.getModelName().isBlank()) {
                return model.getModelName();
            }
            return model.getModelCode() == null ? "" : model.getModelCode();
        }
        return ProductModelAlias.forStored(model.getAbilityType(), model.getModelCode()).name();
    }

    public static List<ProviderPresetItem> buildProviderPresets() {
        ProviderPresetItem xai = new ProviderPresetItem(
                PROVIDER_XAI,
                PROVIDER_XAI_NAME,
                DEFAULT_XAI_BASE_URL,
                List.of(
                        new ProviderAbilityPreset("TEXT", "TEXT", "文本模型", List.of(
                                new ModelPresetOption("grok-4.7", "Grok 4.7", true),
                                new ModelPresetOption("grok-4", "Grok 4", false),
                                new ModelPresetOption("grok-3", "Grok 3", false),
                                new ModelPresetOption("grok-3-mini", "Grok 3 Mini", false))),
                        new ProviderAbilityPreset("IMAGE", "IMAGE", "图片模型", List.of(
                                new ModelPresetOption("grok-imagine-image-2.0", "Grok Imagine Image 2.0", true),
                                new ModelPresetOption("grok-2-image", "Grok 2 Image", false))),
                        new ProviderAbilityPreset("VIDEO", "VIDEO", "视频模型", List.of(
                                new ModelPresetOption("grok-imagine-video-1.5", "Grok Imagine Video 1.5", true),
                                new ModelPresetOption("grok-imagine-video-1.5-lite", "Grok Imagine Video 1.5 Lite", false))),
                        new ProviderAbilityPreset("AUDIO", "VOICE", "音频模型（配音合成）", List.of(
                                new ModelPresetOption("grok-tts", "Grok TTS", true))),
                        new ProviderAbilityPreset("AUDIO", "ASR", "音频模型（字幕识别）", List.of(
                                new ModelPresetOption("grok-voice-transcribe-2.0", "Grok Voice Transcribe 2.0", true)))
                ));
        return List.of(xai);
    }
}
