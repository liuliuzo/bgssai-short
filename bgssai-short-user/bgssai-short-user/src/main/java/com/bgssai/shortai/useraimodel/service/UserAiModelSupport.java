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
    public static final String PROVIDER_KEY_PREFIX = "bpk-";
    public static final String CREDENTIAL_SOURCE_USER = "USER_KEY";

    public static final String PROVIDER_MINIMAX = "MINIMAX";
    public static final String PROVIDER_MINIMAX_NAME = "MiniMax (海螺)";
    public static final String DEFAULT_MINIMAX_BASE_URL = "https://api.minimaxi.com";

    public static final String PROVIDER_KLING = "KLING";
    public static final String PROVIDER_KLING_NAME = "可灵 (Kling AI)";
    public static final String DEFAULT_KLING_BASE_URL = "https://api-beijing.klingai.com";

    public static final String PROVIDER_PIXVERSE = "PIXVERSE";
    public static final String PROVIDER_PIXVERSE_NAME = "PixVerse";
    public static final String DEFAULT_PIXVERSE_BASE_URL = "https://app-api.pixverse.ai";

    public static final String PROVIDER_DEEPSEEK = "DEEPSEEK";
    public static final String PROVIDER_DEEPSEEK_NAME = "DeepSeek";
    public static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com";

    public static final String PROVIDER_XAI = "XAI";
    public static final String PROVIDER_XAI_NAME = "Grok (xAI)";
    public static final String DEFAULT_XAI_BASE_URL = "https://api.x.ai/v1";

    public static final String PROVIDER_OPENAI = "OPENAI";
    public static final String PROVIDER_OPENAI_NAME = "OpenAI";
    public static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1";

    public static final String PROVIDER_ANTHROPIC = "ANTHROPIC";
    public static final String PROVIDER_ANTHROPIC_NAME = "Claude";
    public static final String DEFAULT_ANTHROPIC_BASE_URL = "https://api.anthropic.com";

    public static final String PROVIDER_TOKENHUB = "TENCENT_TOKENHUB";
    public static final String PROVIDER_TOKENHUB_NAME = "TokenHub";
    public static final String DEFAULT_TOKENHUB_BASE_URL = "https://www.bgssai-tokenhub.cn";

    public static final String AUTH_BEARER = "BEARER";
    public static final String AUTH_AK_SK = "AK_SK";
    public static final String AUTH_API_KEY_HEADER = "API_KEY_HEADER";

    public static final Set<String> SUPPORTED_PROVIDERS = Set.of(
            PROVIDER_MINIMAX, PROVIDER_KLING, PROVIDER_PIXVERSE, PROVIDER_DEEPSEEK, PROVIDER_XAI,
            PROVIDER_OPENAI, PROVIDER_ANTHROPIC, PROVIDER_TOKENHUB);
    public static final Set<String> MODALITIES = Set.of("TEXT", "IMAGE", "VIDEO", "AUDIO");
    public static final Set<String> ABILITIES = Set.of("TEXT", "IMAGE", "VIDEO", "VOICE", "ASR", "MUSIC");

    private UserAiModelSupport() {
    }

    public static boolean isUserKeyCode(String code) {
        if (code == null) {
            return false;
        }
        String lower = code.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith(USER_KEY_PREFIX) || lower.startsWith(PROVIDER_KEY_PREFIX);
    }

    public static AiModelConfig withProviderByokContext(AiModelConfig source, String userId,
            String providerCode, String customBaseUrl) {
        if (source == null || userId == null || userId.isBlank()) {
            return source;
        }
        AiModelConfig copy = new AiModelConfig();
        copy.setId(source.getId());
        copy.setModelId(source.getModelId());
        copy.setAbilityType(source.getAbilityType());
        copy.setProviderCode(source.getProviderCode());
        copy.setModelCode(source.getModelCode());
        copy.setModelName(source.getModelName());
        copy.setEnabledFlag(source.getEnabledFlag());
        copy.setDefaultFlag(source.getDefaultFlag());
        copy.setUnitPriceCent(0);
        copy.setPriceUnit(source.getPriceUnit());
        copy.setDeletedFlag(source.getDeletedFlag());
        copy.setCreatedTime(source.getCreatedTime());
        copy.setUpdatedTime(source.getUpdatedTime());

        Map<String, Object> cfg = new java.util.LinkedHashMap<>(ShortJson.map(source.getConfigJson()));
        String canonicalProvider = UserAiProviderCatalog.normalizeRuntimeProviderCode(providerCode);
        cfg.put("credential_source", CREDENTIAL_SOURCE_USER);
        cfg.put("byok_source", "PROVIDER_KEY");
        cfg.put("byok_user_id", userId.trim());
        cfg.put("user_id", userId.trim());
        cfg.put("byok_provider_code", canonicalProvider);
        cfg.put("user_key_id", PROVIDER_KEY_PREFIX + canonicalProvider.toLowerCase(Locale.ROOT));
        String resolvedUrl = UserAiProviderCatalog.resolveEffectiveBaseUrl(customBaseUrl, canonicalProvider);
        if (!resolvedUrl.isBlank()) {
            cfg.put("base_url", resolvedUrl);
        }
        copy.setConfigJson(ShortJson.toJson(cfg));
        return copy;
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
        if ("GROK".equals(code) || "X.AI".equals(code)) {
            return PROVIDER_XAI;
        }
        if ("KLINGAI".equals(code)) {
            return PROVIDER_KLING;
        }
        if ("HAILUO".equals(code) || "MINIMAXI".equals(code)) {
            return PROVIDER_MINIMAX;
        }
        if ("CLAUDE".equals(code)) {
            return PROVIDER_ANTHROPIC;
        }
        if ("TOKENHUB".equals(code) || "TENCENT-TOKENHUB".equals(code)
                || "BGSSAI-TOKENHUB".equals(code) || "BGSSAI_TOKENHUB".equals(code)) {
            return PROVIDER_TOKENHUB;
        }
        if (!SUPPORTED_PROVIDERS.contains(code)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "unsupported provider_code: " + code);
        }
        return code;
    }

    public static String providerName(String providerCode) {
        if (providerCode == null) {
            return "";
        }
        return switch (providerCode.trim().toUpperCase(Locale.ROOT)) {
            case PROVIDER_MINIMAX -> PROVIDER_MINIMAX_NAME;
            case PROVIDER_KLING -> PROVIDER_KLING_NAME;
            case PROVIDER_PIXVERSE -> PROVIDER_PIXVERSE_NAME;
            case PROVIDER_DEEPSEEK -> PROVIDER_DEEPSEEK_NAME;
            case PROVIDER_XAI -> PROVIDER_XAI_NAME;
            case PROVIDER_OPENAI -> PROVIDER_OPENAI_NAME;
            case PROVIDER_ANTHROPIC -> PROVIDER_ANTHROPIC_NAME;
            case PROVIDER_TOKENHUB -> PROVIDER_TOKENHUB_NAME;
            default -> providerCode;
        };
    }

    public static String authType(String providerCode) {
        if (PROVIDER_KLING.equalsIgnoreCase(providerCode)) {
            return AUTH_AK_SK;
        }
        if (PROVIDER_PIXVERSE.equalsIgnoreCase(providerCode)) {
            return AUTH_API_KEY_HEADER;
        }
        return AUTH_BEARER;
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
        if (providerCode == null) {
            return DEFAULT_XAI_BASE_URL;
        }
        return switch (providerCode.trim().toUpperCase(Locale.ROOT)) {
            case PROVIDER_MINIMAX -> DEFAULT_MINIMAX_BASE_URL;
            case PROVIDER_KLING -> DEFAULT_KLING_BASE_URL;
            case PROVIDER_PIXVERSE -> DEFAULT_PIXVERSE_BASE_URL;
            case PROVIDER_DEEPSEEK -> DEFAULT_DEEPSEEK_BASE_URL;
            case PROVIDER_OPENAI -> DEFAULT_OPENAI_BASE_URL;
            case PROVIDER_ANTHROPIC -> DEFAULT_ANTHROPIC_BASE_URL;
            case PROVIDER_TOKENHUB -> DEFAULT_TOKENHUB_BASE_URL;
            default -> DEFAULT_XAI_BASE_URL;
        };
    }

    /**
     * Zero-cost connectivity URL. OpenAI and xAI list models under {@code /v1/models}.
     * Anthropic and TokenHub document {@code GET /v1/models} on a host with no version prefix.
     */
    public static String connectivityProbeUrl(String providerCode, String baseUrl) {
        String provider = providerCode == null ? "" : providerCode.trim().toUpperCase(Locale.ROOT);
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (PROVIDER_ANTHROPIC.equals(provider) || PROVIDER_TOKENHUB.equals(provider)) {
            return stripTrailingPath(base, "/v1") + "/v1/models";
        }
        if (PROVIDER_XAI.equals(provider) || PROVIDER_OPENAI.equals(provider)) {
            String path = URI.create(base).getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return base + "/v1/models";
            }
        }
        return base + "/models";
    }

    public static String stripTrailingPath(String baseUrl, String suffix) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (suffix != null && !suffix.isBlank() && trimmed.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
            return trimmed.substring(0, trimmed.length() - suffix.length());
        }
        return trimmed;
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
        ProviderPresetItem minimax = new ProviderPresetItem(
                PROVIDER_MINIMAX,
                PROVIDER_MINIMAX_NAME,
                AUTH_BEARER,
                DEFAULT_MINIMAX_BASE_URL,
                List.of(
                        new ProviderAbilityPreset("TEXT", "TEXT", "文本模型", List.of(
                                new ModelPresetOption("MiniMax-M2.5", "MiniMax M2.5", true),
                                new ModelPresetOption("MiniMax-M2.5-highspeed", "MiniMax M2.5 Highspeed", false),
                                new ModelPresetOption("abab6.5s-chat", "MiniMax abab6.5s", false))),
                        new ProviderAbilityPreset("IMAGE", "IMAGE", "图片模型", List.of(
                                new ModelPresetOption("image-01", "MiniMax Image-01", true),
                                new ModelPresetOption("image-01-live", "MiniMax Image-01 Live", false))),
                        new ProviderAbilityPreset("VIDEO", "VIDEO", "视频模型", List.of(
                                new ModelPresetOption("MiniMax-Hailuo-02", "MiniMax Hailuo 02", true),
                                new ModelPresetOption("MiniMax-Hailuo-2.3", "MiniMax Hailuo 2.3", false),
                                new ModelPresetOption("T2V-01-Director", "MiniMax T2V-01 Director", false),
                                new ModelPresetOption("I2V-01-Director", "MiniMax I2V-01 Director", false))),
                        new ProviderAbilityPreset("AUDIO", "VOICE", "音频模型（配音合成）", List.of(
                                new ModelPresetOption("speech-2.8-hd", "MiniMax Speech 2.8 HD", true),
                                new ModelPresetOption("speech-2.8-turbo", "MiniMax Speech 2.8 Turbo", false),
                                new ModelPresetOption("speech-02-hd", "MiniMax Speech 02 HD", false))),
                        new ProviderAbilityPreset("AUDIO", "ASR", "音频模型（字幕识别）", List.of(
                                new ModelPresetOption("asr-1.0", "MiniMax ASR 1.0", true))),
                        new ProviderAbilityPreset("AUDIO", "MUSIC", "音频模型（背景音乐）", List.of(
                                new ModelPresetOption("music-2.5", "MiniMax Music 2.5", true),
                                new ModelPresetOption("music-2.0", "MiniMax Music 2.0", false)))
                ));
        ProviderPresetItem kling = new ProviderPresetItem(
                PROVIDER_KLING,
                PROVIDER_KLING_NAME,
                AUTH_AK_SK,
                DEFAULT_KLING_BASE_URL,
                List.of(
                        new ProviderAbilityPreset("VIDEO", "VIDEO", "视频模型", List.of(
                                new ModelPresetOption("kling-v3", "Kling V3", true),
                                new ModelPresetOption("kling-v2-6", "Kling V2.6", false),
                                new ModelPresetOption("kling-v2-5-turbo", "Kling V2.5 Turbo", false),
                                new ModelPresetOption("kling-v2-1-master", "Kling V2.1 Master", false),
                                new ModelPresetOption("kling-v1-6", "Kling V1.6", false))),
                        new ProviderAbilityPreset("IMAGE", "IMAGE", "图片模型", List.of(
                                new ModelPresetOption("kling-v3", "Kling Image V3", true),
                                new ModelPresetOption("kling-v2-1", "Kling Image V2.1", false),
                                new ModelPresetOption("kling-v1-5", "Kling Image V1.5", false))),
                        new ProviderAbilityPreset("AUDIO", "VOICE", "音频模型（配音合成）", List.of(
                                new ModelPresetOption("kling-tts", "Kling TTS", true)))
                ));
        ProviderPresetItem pixverse = new ProviderPresetItem(
                PROVIDER_PIXVERSE,
                PROVIDER_PIXVERSE_NAME,
                AUTH_API_KEY_HEADER,
                DEFAULT_PIXVERSE_BASE_URL,
                List.of(
                        new ProviderAbilityPreset("VIDEO", "VIDEO", "视频模型", List.of(
                                new ModelPresetOption("v6", "PixVerse v6", true),
                                new ModelPresetOption("v6-max", "PixVerse v6 Max", false),
                                new ModelPresetOption("v5.6", "PixVerse v5.6", false),
                                new ModelPresetOption("v5", "PixVerse v5", false),
                                new ModelPresetOption("v4.5", "PixVerse v4.5", false)))
                ));
        ProviderPresetItem deepseek = new ProviderPresetItem(
                PROVIDER_DEEPSEEK,
                PROVIDER_DEEPSEEK_NAME,
                AUTH_BEARER,
                DEFAULT_DEEPSEEK_BASE_URL,
                List.of(
                        new ProviderAbilityPreset("TEXT", "TEXT", "文本模型", List.of(
                                new ModelPresetOption("deepseek-chat", "DeepSeek Chat", true),
                                new ModelPresetOption("deepseek-reasoner", "DeepSeek Reasoner", false)))
                ));
        ProviderPresetItem xai = new ProviderPresetItem(
                PROVIDER_XAI,
                PROVIDER_XAI_NAME,
                AUTH_BEARER,
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
        ProviderPresetItem openai = new ProviderPresetItem(
                PROVIDER_OPENAI,
                PROVIDER_OPENAI_NAME,
                AUTH_BEARER,
                DEFAULT_OPENAI_BASE_URL,
                List.of(
                        new ProviderAbilityPreset("TEXT", "TEXT", "文本模型", List.of(
                                new ModelPresetOption("gpt-6-luna", "GPT-6 Luna", true),
                                new ModelPresetOption("gpt-5.4", "GPT-5.4", false),
                                new ModelPresetOption("gpt-4o", "GPT-4o", false))),
                        new ProviderAbilityPreset("IMAGE", "IMAGE", "图片模型", List.of(
                                new ModelPresetOption("gpt-image-2.6-sunburst", "GPT Image 2.6 Sunburst", true),
                                new ModelPresetOption("gpt-image-2.6-flare", "GPT Image 2.6 Flare", false))),
                        new ProviderAbilityPreset("AUDIO", "ASR", "音频模型（字幕识别）", List.of(
                                new ModelPresetOption("gpt-transcribe", "GPT Transcribe", true),
                                new ModelPresetOption("whisper-1", "Whisper", false)))
                ));
        ProviderPresetItem claude = new ProviderPresetItem(
                PROVIDER_ANTHROPIC,
                PROVIDER_ANTHROPIC_NAME,
                AUTH_BEARER,
                DEFAULT_ANTHROPIC_BASE_URL,
                List.of(
                        new ProviderAbilityPreset("TEXT", "TEXT", "文本模型", List.of(
                                new ModelPresetOption("claude-opus-5-5", "Claude Opus 5.5", true),
                                new ModelPresetOption("claude-sonnet-5-5", "Claude Sonnet 5.5", false),
                                new ModelPresetOption("claude-haiku-5-5", "Claude Haiku 5.5", false)))
                ));
        ProviderPresetItem tokenhub = new ProviderPresetItem(
                PROVIDER_TOKENHUB,
                PROVIDER_TOKENHUB_NAME,
                AUTH_BEARER,
                DEFAULT_TOKENHUB_BASE_URL,
                List.of(
                        new ProviderAbilityPreset("TEXT", "TEXT", "文本模型", List.of(
                                new ModelPresetOption("deepseek-chat", "DeepSeek Chat", true),
                                new ModelPresetOption("hy4-preview", "Hy4 preview", false),
                                new ModelPresetOption("kimi-k3", "Kimi K3", false))),
                        new ProviderAbilityPreset("IMAGE", "IMAGE", "图片模型", List.of(
                                new ModelPresetOption("kling-v3", "可灵文生图", true))),
                        new ProviderAbilityPreset("VIDEO", "VIDEO", "视频模型", List.of(
                                new ModelPresetOption("minimax-video-h3", "MiniMax Video H3", true),
                                new ModelPresetOption("kling-video-v3", "Kling Video V3", false),
                                new ModelPresetOption("pixverse-video-v6.0", "PixVerse Video v6", false))),
                        new ProviderAbilityPreset("AUDIO", "VOICE", "音频模型（配音合成）", List.of(
                                new ModelPresetOption("minimax-speech-2.8-hd", "MiniMax Speech 2.8 HD", true))),
                        new ProviderAbilityPreset("AUDIO", "ASR", "音频模型（字幕识别）", List.of(
                                new ModelPresetOption("hy-asr-3.0-preview", "Hy ASR 3.0 Preview", true))),
                        new ProviderAbilityPreset("AUDIO", "MUSIC", "音频模型（背景音乐）", List.of(
                                new ModelPresetOption("minimax-music-v2.6", "MiniMax Music v2.6", true)))
                ));
        return List.of(minimax, kling, pixverse, deepseek, xai, openai, claude, tokenhub);
    }
}
