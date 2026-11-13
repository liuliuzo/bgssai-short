package com.bgssai.shortai.aiprovider;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * Company-hosted OpenAI-compatible gateway. Direct calls are allowed only for
 * this host and the known company model ids. Third-party hosts stay on the CN hub.
 */
public final class CompanyCompatGateway {

    public static final String HOST = "chat.bgssai.com";
    public static final String DEFAULT_BASE = "https://chat.bgssai.com/v1";

    private CompanyCompatGateway() {
    }

    public static boolean allows(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(baseUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
                return false;
            }
            if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return false;
            }
            if (uri.getPort() != -1 && uri.getPort() != 443) {
                return false;
            }
            String host = uri.getHost();
            return host != null && HOST.equalsIgnoreCase(host);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean directModel(String ability, String modelCode) {
        if (ability == null || modelCode == null) {
            return false;
        }
        String kind = ability.trim().toUpperCase(Locale.ROOT);
        String id = modelCode.trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty()) {
            return false;
        }
        return switch (kind) {
            case "TEXT" -> "qwen".equals(id) || id.startsWith("qwen2") || "bgssai-chat".equals(id);
            case "IMAGE" -> "sd-turbo".equals(id) || "bgssai-image".equals(id);
            case "VIDEO" -> id.startsWith("cogvideox") || "bgssai-video".equals(id);
            case "VOICE" -> "piper".equals(id) || "bgssai-tts".equals(id) || "edge-tts".equals(id);
            case "ASR" -> "whisper-1".equals(id) || id.startsWith("faster-whisper") || "bgssai-asr".equals(id);
            case "MUSIC" -> "musicgen-small".equals(id) || id.startsWith("musicgen") || "bgssai-music".equals(id);
            default -> false;
        };
    }

    public static String defaultModel(String ability) {
        if (ability == null) {
            return "";
        }
        return switch (ability.trim().toUpperCase(Locale.ROOT)) {
            case "TEXT" -> "qwen";
            case "IMAGE" -> "sd-turbo";
            case "VIDEO" -> "cogvideox";
            case "VOICE" -> "piper";
            case "ASR" -> "whisper-1";
            case "MUSIC" -> "musicgen-small";
            default -> "";
        };
    }

    public static AiModelConfig synthetic(String ability, String modelCode) {
        if (ability == null || !directModel(ability, modelCode)) {
            return null;
        }
        String code = modelCode.trim();
        String kind = ability.trim().toUpperCase(Locale.ROOT);
        AiModelConfig model = new AiModelConfig();
        model.setAbilityType(kind);
        model.setProviderCode(OpenAiCompatibleAiProvider.CODE);
        model.setModelCode(code);
        model.setModelName(displayName(kind));
        model.setPriceUnit("PER_CALL");
        model.setUnitPriceCent(0);
        model.setDefaultFlag(0);
        model.setEnabledFlag(1);
        model.setTimeoutSec("MUSIC".equals(kind) ? 240 : ("VIDEO".equals(kind) ? 900 : 180));
        model.setConfigJson(ShortJson.toJson(config()));
        return model;
    }

    private static Map<String, Object> config() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("base_url", DEFAULT_BASE);
        cfg.put("secret_ref", SysSecretReader.AI_OPENAI_COMPAT_API_KEY);
        cfg.put("chat_path", "/chat/completions");
        cfg.put("image_path", "/images/generations");
        cfg.put("video_path", "/videos/generations");
        cfg.put("speech_path", "/audio/speech");
        cfg.put("music_path", "/audio/music");
        cfg.put("transcription_path", "/audio/transcriptions");
        cfg.put("language", "zh");
        cfg.put("size", "512x768");
        cfg.put("default_voice_id", "zh");
        cfg.put("poll_interval_ms", 3000);
        return cfg;
    }

    private static String displayName(String ability) {
        return switch (ability) {
            case "TEXT" -> "剧本文本";
            case "IMAGE" -> "分镜画面";
            case "VIDEO" -> "镜头视频";
            case "VOICE" -> "台词配音";
            case "ASR" -> "台词转写";
            case "MUSIC" -> "背景配乐";
            default -> "";
        };
    }
}
