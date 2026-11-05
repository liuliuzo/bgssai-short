package com.bgssai.shortai.aimodel;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.bgssai.shortai.production.support.ShortJson;

/**
 * User-facing model names. Callers see a BGSSAI product alias; the gateway id
 * stays on the model row and is resolved only on the server.
 * <p>
 * {@code config_json.public_alias} / {@code public_name} can add another alias.
 * The value must start with {@code bgssai-} and must not name an upstream vendor.
 * Rows without an alias fold into the ability default below.
 */
public final class ProductModelAlias {

    public static final String ALIAS_KEY = "public_alias";
    public static final String NAME_KEY = "public_name";

    public record Face(String code, String name) {
    }

    private static final Map<String, Face> BY_ABILITY = Map.of(
            "TEXT", new Face("bgssai-chat", "BGSSAI Chat"),
            "IMAGE", new Face("bgssai-image", "BGSSAI Image"),
            "VIDEO", new Face("bgssai-video", "BGSSAI Video"),
            "VOICE", new Face("bgssai-tts", "BGSSAI TTS"),
            "ASR", new Face("bgssai-asr", "BGSSAI ASR"),
            "MUSIC", new Face("bgssai-music", "BGSSAI Music"));

    private static final Map<String, Face> BY_CODE = Map.of(
            "bgssai-chat", BY_ABILITY.get("TEXT"),
            "bgssai-image", BY_ABILITY.get("IMAGE"),
            "bgssai-video", BY_ABILITY.get("VIDEO"),
            "bgssai-tts", BY_ABILITY.get("VOICE"),
            "bgssai-asr", BY_ABILITY.get("ASR"),
            "bgssai-music", BY_ABILITY.get("MUSIC"));

    /**
     * Upstream brands and ids that must not reach a user, a guest page, or a client payload.
     * Local tools such as ffmpeg are not in this list.
     */
    private static final Pattern VENDOR = Pattern.compile("(?i)(?<![a-z0-9])(?:"
            + "stable[\\s-]?diffusion|sd-turbo|sdxl"
            + "|cogvideox[a-z0-9._-]*|cogvideo"
            + "|qwen[a-z0-9._-]*"
            + "|deepseek[a-z0-9._-]*"
            + "|minimax[a-z0-9._-]*|海螺"
            + "|kling[a-z0-9._-]*|可灵"
            + "|pixverse[a-z0-9._-]*"
            + "|hunyuan|混元"
            + "|openai|chatgpt|gpt-[a-z0-9._-]+|claude[a-z0-9._-]*|anthropic|gemini[a-z0-9._-]*"
            + "|midjourney|whisper|piper|edge-tts|cosyvoice[a-z0-9._-]*"
            + "|dashscope|moonshot|kimi(?:[a-z0-9._-]*)?|vidu[a-z0-9._-]*|seedance[a-z0-9._-]*"
            + "|token\\s*-?\\s*hub|tencentmaas"
            + "|flux|sora|runway|luma|pika|suno|udio"
            + "|jimeng|即梦|doubao|豆包|zhipu|chatglm|glm-[a-z0-9._-]+"
            + "|hailuo|wanx[a-z0-9._-]*|kolors|通义|文心"
            + "|hy-asr[a-z0-9._-]*|hy-vision[a-z0-9._-]*|hy-image[a-z0-9._-]*|hy-video[a-z0-9._-]*|hy-mt[a-z0-9._-]*"
            + "|hy[0-9][a-z0-9._-]*"
            + "|speech-[0-9][a-z0-9._-]*|image-01"
            + ")(?![a-z0-9])");

    private ProductModelAlias() {
    }

    public static Face canonical(String ability) {
        if (ability == null) {
            return null;
        }
        return BY_ABILITY.get(ability.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isCanonical(String ability, String code) {
        Face face = canonical(ability);
        return face != null && code != null && face.code().equalsIgnoreCase(code.trim());
    }

    public static boolean isSafeAlias(String code) {
        if (code == null) {
            return false;
        }
        String value = code.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("bgssai-") && !leaks(value);
    }

    public static boolean leaks(String text) {
        return text != null && !text.isBlank() && VENDOR.matcher(text).find();
    }

    public static String explicitAlias(String configJson) {
        String alias = ShortJson.text(ShortJson.map(configJson).get(ALIAS_KEY)).trim();
        return isSafeAlias(alias) ? alias : "";
    }

    /**
     * Picker identity for one configured row. Vendor rows with no explicit alias
     * share the ability product name, so the client list stays one BGSSAI model.
     */
    public static Face faceOf(String ability, String modelCode, String configJson) {
        String alias = explicitAlias(configJson);
        if (!alias.isEmpty()) {
            return new Face(alias, safeName(configJson, alias));
        }
        Face known = byCode(modelCode);
        if (known != null) {
            return known;
        }
        return canonical(ability);
    }

    /** Writes the client alias into a response map. The stored gateway id is not copied. */
    public static void hideStoredModel(Map<String, Object> item, String ability, String storedCode) {
        if (item == null) {
            return;
        }
        Face face = forStored(ability, storedCode);
        item.put("model_code", face.code());
        if (face.name() != null && !face.name().isBlank()) {
            item.put("display_name", face.name());
            item.put("model_name", face.name());
        }
    }

    /** What a stored gateway id should look like in a client payload. */
    public static Face forStored(String ability, String modelCode) {
        Face known = byCode(modelCode);
        if (known != null) {
            return known;
        }
        if (isLocalTool(modelCode)) {
            return new Face(modelCode.trim(), modelCode.trim());
        }
        Face abilityFace = canonical(ability);
        if (abilityFace != null) {
            return abilityFace;
        }
        if (modelCode == null || modelCode.isBlank()) {
            return new Face("", "");
        }
        if (isSafeAlias(modelCode)) {
            return new Face(modelCode.trim(), "BGSSAI");
        }
        if (leaks(modelCode)) {
            return new Face("bgssai", "BGSSAI");
        }
        return new Face(modelCode.trim(), modelCode.trim());
    }

    /** Drop upstream brand tokens from a sentence that may reach the user. */
    public static String scrub(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String out = VENDOR.matcher(text).replaceAll("");
        out = out.replaceAll("：\\s*，", "，");
        out = out.replaceAll("：\\s*。", "。");
        out = out.replaceAll("（\\s*）", "");
        out = out.replaceAll("[ \\t]{2,}", " ").trim();
        return out;
    }

    private static Face byCode(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return null;
        }
        return BY_CODE.get(modelCode.trim().toLowerCase(Locale.ROOT));
    }

    private static String safeName(String configJson, String alias) {
        String name = ShortJson.text(ShortJson.map(configJson).get(NAME_KEY)).trim();
        if (!name.isEmpty() && !leaks(name)) {
            return name;
        }
        Face known = byCode(alias);
        return known == null ? "BGSSAI" : known.name();
    }

    private static boolean isLocalTool(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return false;
        }
        String value = modelCode.trim().toLowerCase(Locale.ROOT);
        return "ffmpeg".equals(value) || value.startsWith("ffmpeg-");
    }
}
