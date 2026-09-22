package com.bgssai.shortai.aiprovider;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * Org-wide TokenHub client helpers: dual hub (CN + Global), secret refs, and
 * user-facing message scrubbing so TokenHub branding never leaks to creators.
 */
public final class TokenHubClientSupport {

    public static final String HUB_CN = "cn";
    public static final String HUB_GLOBAL = "global";

    public static final String DEFAULT_CN_BASE_URL = "https://www.bgssai-tokenhub.cn";
    public static final String DEFAULT_GLOBAL_BASE_URL = "https://www.bgssai-tokenhub.com";

    private static final Pattern TOKENHUB_WORD = Pattern.compile("(?i)token\\s*-?\\s*hub");
    private static final Pattern SECRET_REF = Pattern.compile("(?i)ai\\.[a-z0-9_.]+");
    private static final Pattern BEARER_LEAK = Pattern.compile("(?i)bearer\\s+[a-z0-9._\\-]{8,}");

    private TokenHubClientSupport() {
    }

    public static String resolveHub(Map<String, Object> cfg, String modelCode) {
        String hub = ShortJson.text(cfg == null ? null : cfg.get("hub")).trim().toLowerCase(Locale.ROOT);
        if ("global".equals(hub) || "intl".equals(hub) || "overseas".equals(hub)) {
            return HUB_GLOBAL;
        }
        if ("cn".equals(hub) || "domestic".equals(hub) || "china".equals(hub)) {
            return HUB_CN;
        }
        String base = ShortJson.text(cfg == null ? null : cfg.get("base_url")).toLowerCase(Locale.ROOT);
        if (base.contains("tokenhub-global") || base.contains("bgssai-tokenhub-global")
                || base.contains("tokenhub-intl") || base.contains("bgssai-tokenhub.com")) {
            return HUB_GLOBAL;
        }
        if (base.contains("tokenhub-cn") || base.contains("bgssai-tokenhub-cn")
                || base.contains("bgssai-tokenhub.cn") || base.contains("tokenhub.tencentmaas.com")) {
            return HUB_CN;
        }
        String model = modelCode == null ? "" : modelCode.trim().toLowerCase(Locale.ROOT);
        if (model.startsWith("gpt-") || model.startsWith("claude") || model.startsWith("gemini")
                || model.startsWith("grok") || model.startsWith("o1") || model.startsWith("o3")
                || model.startsWith("o4")) {
            return HUB_GLOBAL;
        }
        return HUB_CN;
    }

    public static String defaultBaseUrl(String hub, String cnDefault, String globalDefault) {
        if (HUB_GLOBAL.equals(hub)) {
            return ProviderHttp.trimSlash(ProviderHttp.firstNonBlank(globalDefault, DEFAULT_GLOBAL_BASE_URL));
        }
        return ProviderHttp.trimSlash(ProviderHttp.firstNonBlank(cnDefault, DEFAULT_CN_BASE_URL));
    }

    public static String defaultSecretRef(String hub) {
        return HUB_GLOBAL.equals(hub)
                ? SysSecretReader.AI_TOKENHUB_GLOBAL_API_KEY
                : SysSecretReader.AI_TOKENHUB_CN_API_KEY;
    }

    /**
     * Reads hub key; CN also accepts the legacy {@code ai.tencent_tokenhub.api_key} when the
     * new CN key is empty (empty-fill migration).
     */
    public static String resolveApiKey(SysSecretReader secrets, String hub, String configuredRef) {
        String ref = ProviderHttp.firstNonBlank(configuredRef, defaultSecretRef(hub));
        String key = secrets.get(ref);
        if (!key.isBlank()) {
            return key;
        }
        if (HUB_CN.equals(hub) && !SysSecretReader.AI_TENCENT_TOKENHUB_API_KEY.equals(ref)) {
            return secrets.get(SysSecretReader.AI_TENCENT_TOKENHUB_API_KEY);
        }
        if (HUB_CN.equals(hub) && SysSecretReader.AI_TENCENT_TOKENHUB_API_KEY.equals(ref)) {
            String modern = secrets.get(SysSecretReader.AI_TOKENHUB_CN_API_KEY);
            if (!modern.isBlank()) {
                return modern;
            }
        }
        return "";
    }

    public static boolean isTokenHubHost(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        String n = baseUrl.toLowerCase(Locale.ROOT);
        return n.contains("tokenhub") || n.contains("bgssai-tokenhub");
    }

    /** Scrub infra names from messages that may reach the user SPA or fail_reason. */
    public static String userFacingMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "模型暂时不可用，请稍后重试或更换模型";
        }
        String msg = raw.trim();
        if (TOKENHUB_WORD.matcher(msg).find() || msg.toLowerCase(Locale.ROOT).contains("tencentmaas")
                || SECRET_REF.matcher(msg).find() || BEARER_LEAK.matcher(msg).find()
                || msg.toLowerCase(Locale.ROOT).contains("api key missing")
                || msg.toLowerCase(Locale.ROOT).contains("secret_ref")) {
            return "模型暂时不可用，请稍后重试或更换模型";
        }
        if (msg.length() > 240) {
            return msg.substring(0, 240);
        }
        return msg;
    }
}
