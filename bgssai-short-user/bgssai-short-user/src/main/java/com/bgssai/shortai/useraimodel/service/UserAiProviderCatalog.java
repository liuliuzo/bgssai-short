package com.bgssai.shortai.useraimodel.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/**
 * 用户自有厂商 API Key 支持目录及 URL/认证工具。
 */
public final class UserAiProviderCatalog {

    public record ProviderSpec(
            String providerCode,
            String providerName,
            String defaultBaseUrl,
            boolean supportsAkSk,
            String defaultChatModel,
            String defaultVideoModel
    ) {
    }

    private static final List<ProviderSpec> SPECS = List.of(
            new ProviderSpec("DEEPSEEK", "DeepSeek", "https://api.deepseek.com", false, "deepseek-chat", ""),
            new ProviderSpec("MINIMAX", "MiniMax", "https://api.minimaxi.com", false, "MiniMax-Text-01", "MiniMax-Hailuo-2.3"),
            new ProviderSpec("KLING", "可灵 AI", "https://api-beijing.klingai.com", true, "", "kling-v2-master"),
            new ProviderSpec("DASHSCOPE", "阿里百炼 DashScope", "https://dashscope.aliyuncs.com/compatible-mode/v1", false, "qwen-plus", "wanx2.1-t2v-turbo"),
            new ProviderSpec("ZHIPU", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", false, "glm-4-plus", "cogvideox-2"),
            new ProviderSpec("MOONSHOT", "月之暗面 Kimi", "https://api.moonshot.cn/v1", false, "moonshot-v1-8k", ""),
            new ProviderSpec("VOLCENGINE", "火山引擎方舟", "https://ark.cn-beijing.volces.com/api/v3", false, "doubao-1-5-pro-32k-250115", "doubao-seedance-1-0-pro-250528"),
            new ProviderSpec("HUNYUAN", "腾讯混元", "https://api.hunyuan.cloud.tencent.com/v1", false, "hunyuan-turbo", ""),
            new ProviderSpec("OPENAI", "OpenAI", "https://api.openai.com/v1", false, "gpt-4o", "sora-2"),
            new ProviderSpec("ANTHROPIC", "Anthropic", "https://api.anthropic.com", false, "claude-3-5-sonnet-latest", ""),
            new ProviderSpec("GOOGLE", "Google Gemini", "https://generativelanguage.googleapis.com", false, "gemini-2.5-flash", "veo-3.0-generate-preview")
    );

    private static final Map<String, ProviderSpec> BY_CODE;

    static {
        Map<String, ProviderSpec> map = new LinkedHashMap<>();
        for (ProviderSpec spec : SPECS) {
            map.put(spec.providerCode(), spec);
        }
        BY_CODE = Map.copyOf(map);
    }

    private static final Set<String> RUNTIME_ONLY_ALIASES = Set.of("SEEDANCE", "VEO");

    private UserAiProviderCatalog() {
    }

    public static List<ProviderSpec> all() {
        return SPECS;
    }

    public static ProviderSpec get(String providerCode) {
        return BY_CODE.get(normalizeProviderCode(providerCode));
    }

    /**
     * 规范化用户提交的厂商编码（含 KLINGAI -> KLING）。
     */
    public static String normalizeProviderCode(String raw) {
        if (raw == null) {
            return "";
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if ("KLINGAI".equals(code)) {
            return "KLING";
        }
        return code;
    }

    /**
     * 运行期将具体模型厂商编码归一到 user_ai_provider_key 的厂商编码。
     */
    public static String normalizeRuntimeProviderCode(String raw) {
        String code = normalizeProviderCode(raw);
        if ("SEEDANCE".equals(code)) {
            return "VOLCENGINE";
        }
        if ("VEO".equals(code)) {
            return "GOOGLE";
        }
        if (BY_CODE.containsKey(code)) {
            return code;
        }
        return code;
    }

    public static String requireSupportedProviderCode(String raw) {
        String code = normalizeProviderCode(raw);
        if (!BY_CODE.containsKey(code)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "不支持的厂商编码: " + raw);
        }
        return code;
    }

    public static boolean isSupported(String providerCode) {
        return BY_CODE.containsKey(normalizeProviderCode(providerCode));
    }

    public static boolean isRuntimeSupported(String providerCode) {
        String code = normalizeProviderCode(providerCode);
        return BY_CODE.containsKey(code) || RUNTIME_ONLY_ALIASES.contains(code);
    }

    public static String defaultBaseUrl(String providerCode) {
        ProviderSpec spec = get(providerCode);
        return spec == null ? "" : spec.defaultBaseUrl();
    }

    public static boolean supportsAkSk(String providerCode) {
        ProviderSpec spec = get(providerCode);
        return spec != null && spec.supportsAkSk();
    }

    /**
     * 校验并返回规范化后的自定义 base_url（必须为公网 HTTPS 地址）。
     */
    public static String validateBaseUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://")) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "base_url 必须使用 https:// 协议");
        }
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "base_url 格式无效");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "base_url 不支持用户信息、查询参数或片段");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "base_url 缺少有效域名");
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(lowerHost)
                || lowerHost.endsWith(".localhost")
                || lowerHost.endsWith(".local")
                || lowerHost.endsWith(".internal")
                || "0.0.0.0".equals(lowerHost)
                || "::1".equals(lowerHost)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "base_url 不能指向本地或内网地址");
        }
        if (isPrivateOrReservedIpLiteral(lowerHost)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "base_url 不能指向内网 IP 地址");
        }
        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) {
                if (isDisallowedAddress(addr)) {
                    throw new BizException(CommonErrorCode.INVALID_PARAM, "base_url 不能解析到内网或保留地址");
                }
            }
        } catch (UnknownHostException ignored) {
            // 离线环境/受限网络环境下保留公网域名格式校验，不误杀合法自定义域名
        }
        return trimSlash(trimmed);
    }

    public static String resolveEffectiveBaseUrl(String customBaseUrl, String providerCode) {
        String custom = customBaseUrl == null ? "" : trimSlash(customBaseUrl.trim());
        String canonical = normalizeRuntimeProviderCode(providerCode);
        ProviderSpec spec = BY_CODE.get(canonical);
        if (custom.isEmpty()) {
            return spec == null ? "" : spec.defaultBaseUrl();
        }
        if (spec != null && hasNoPath(custom) && hasPath(spec.defaultBaseUrl())) {
            URI defaultUri = URI.create(spec.defaultBaseUrl());
            return custom + trimSlash(defaultUri.getPath());
        }
        return custom;
    }

    /**
     * 可灵 BYOK 认证 Token 解析：
     * 优先 access_key + secret_key HS256 JWT，其次单 Bearer api_key。
     */
    public static String resolveKlingBearerToken(String apiKey, String accessKey, String secretKey) {
        String ak = accessKey == null ? "" : accessKey.trim();
        String sk = secretKey == null ? "" : secretKey.trim();
        String key = apiKey == null ? "" : apiKey.trim();
        if (!ak.isEmpty() && !sk.isEmpty()) {
            return KlingJwtSupport.signJwt(ak, sk);
        }
        if (!key.isEmpty() && !sk.isEmpty()) {
            return KlingJwtSupport.signJwt(key, sk);
        }
        if (!key.isEmpty()) {
            return key;
        }
        throw new BizException(CommonErrorCode.RULE_REJECT, "可灵自有密钥不完整");
    }

    public static boolean containsMaskPattern(String value) {
        return value != null && (value.contains("****") || value.contains("••••"));
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        String v = url.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static boolean hasNoPath(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            return path == null || path.isEmpty() || "/".equals(path);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean hasPath(String url) {
        return !hasNoPath(url);
    }

    private static boolean isPrivateOrReservedIpLiteral(String host) {
        String stripped = host;
        if (stripped.startsWith("[") && stripped.endsWith("]")) {
            stripped = stripped.substring(1, stripped.length() - 1);
        }
        boolean looksLikeIpv4 = stripped.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
        boolean looksLikeIpv6 = stripped.contains(":");
        if (!looksLikeIpv4 && !looksLikeIpv6) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(stripped);
            return isDisallowedAddress(addr);
        } catch (UnknownHostException e) {
            return true;
        }
    }

    private static boolean isDisallowedAddress(InetAddress addr) {
        if (addr.isAnyLocalAddress()
                || addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            if (b0 == 0 || b0 == 127 || b0 == 10) {
                return true;
            }
            if (b0 == 172 && b1 >= 16 && b1 <= 31) {
                return true;
            }
            if (b0 == 192 && b1 == 168) {
                return true;
            }
            if (b0 == 169 && b1 == 254) {
                return true;
            }
            if (b0 == 100 && b1 >= 64 && b1 <= 127) {
                return true;
            }
        } else if (bytes.length == 16) {
            int first = bytes[0] & 0xFF;
            if ((first & 0xFE) == 0xFC) {
                return true;
            }
        }
        return false;
    }
}
