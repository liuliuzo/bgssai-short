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

    /**
     * Short is a CN-only product: every model call is pinned to the CN hub.
     * Global / overseas hosts and model prefixes must not redirect traffic.
     */
    public static String resolveHub(Map<String, Object> cfg, String modelCode) {
        return HUB_CN;
    }

    /** True when config or model would have selected Global before the CN pin. */
    public static boolean wouldSelectGlobal(Map<String, Object> cfg, String modelCode) {
        String hub = ShortJson.text(cfg == null ? null : cfg.get("hub")).trim().toLowerCase(Locale.ROOT);
        if ("global".equals(hub) || "intl".equals(hub) || "overseas".equals(hub)) {
            return true;
        }
        String base = ShortJson.text(cfg == null ? null : cfg.get("base_url")).toLowerCase(Locale.ROOT);
        if (base.contains("tokenhub-global") || base.contains("bgssai-tokenhub-global")
                || base.contains("tokenhub-intl")) {
            return true;
        }
        if (base.contains("bgssai-tokenhub.com") && !base.contains("bgssai-tokenhub.cn")) {
            return true;
        }
        String model = modelCode == null ? "" : modelCode.trim().toLowerCase(Locale.ROOT);
        return model.startsWith("gpt-") || model.startsWith("claude") || model.startsWith("gemini")
                || model.startsWith("grok") || model.startsWith("o1") || model.startsWith("o3")
                || model.startsWith("o4");
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

    public static boolean isOfficialHub(String value) {
        try {
            java.net.URI uri = java.net.URI.create(value);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null && host != null
                    && java.util.Set.of(
                        "bgssai-tokenhub.cn", "www.bgssai-tokenhub.cn",
                        "dev.user.bgssai-tokenhub.cn", "dev.admin.bgssai-tokenhub.cn",
                        "bgssai-tokenhub.com", "www.bgssai-tokenhub.com",
                        "dev.user.bgssai-tokenhub.com", "dev.admin.bgssai-tokenhub.com",
                        "bgssai-tokenhub-global.com", "www.bgssai-tokenhub-global.com").contains(host.toLowerCase(Locale.ROOT));
        } catch (Exception ignored) { return false; }
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
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.contains("remote host terminated the handshake")
                || lower.contains("connection reset")
                || lower.contains("unexpected eof while reading")
                || lower.contains("sslhandshake")
                || lower.contains("handshake_failure")) {
            return "模型网关连接失败，请稍后重试";
        }
        if (lower.contains("provider is not configured")) {
            String provider = GatewayErrorText.providerName(msg);
            return "模型服务暂未开通：" + (provider.isEmpty() ? "当前模型" : provider) + "，请稍后重试";
        }
        if (lower.matches("http \\d{3}") || lower.matches("download status \\d{3}")) {
            return "模型服务暂时不可用，请稍后重试";
        }
        if (TOKENHUB_WORD.matcher(msg).find() || lower.contains("tencentmaas")
                || SECRET_REF.matcher(msg).find() || BEARER_LEAK.matcher(msg).find()
                || lower.contains("api key missing")
                || lower.contains("secret_ref")) {
            return "模型暂时不可用，请稍后重试或更换模型";
        }
        if (msg.length() > 240) {
            return msg.substring(0, 240);
        }
        return msg;
    }
}
