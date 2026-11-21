package com.bgssai.shortai.secret;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.production.support.ShortJson;

/**
 * Probes upstream platform-assigned API key names safely without blocking save or test flows.
 */
@Component
public class PlatformKeyNameProber {

    static final int MAX_PLATFORM_KEY_NAME_LENGTH = 128;
    private static final int TIMEOUT_SECONDS = 3;
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private static final String DEFAULT_TOKENHUB_CN_BASE_URL = "https://www.bgssai-tokenhub.cn";
    private static final String XAI_BASE_URL = "https://api.x.ai";
    private static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";

    enum ResponseMode {
        TOP_LEVEL_NAME,
        OPENROUTER_DATA_NAME
    }

    record ProbeTarget(String url, ResponseMode mode) {
    }

    @FunctionalInterface
    interface ProbeTransport {
        ProbeResponse get(String url, String bearerToken) throws IOException, InterruptedException;
    }

    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws IOException;
    }

    record ProbeResponse(int statusCode, String body) {
    }

    private final String tokenHubCnBaseUrl;
    private final ProbeTransport transport;
    private final HostResolver hostResolver;

    @Autowired
    public PlatformKeyNameProber(
            @Value("${bgssai.llm.tokenhub-cn-base-url:${bgssai.llm.gateway-base-url:https://www.bgssai-tokenhub.cn}}")
            String tokenHubCnBaseUrl) {
        this(tokenHubCnBaseUrl, defaultTransport(), InetAddress::getAllByName);
    }

    PlatformKeyNameProber(String tokenHubCnBaseUrl, ProbeTransport transport, HostResolver hostResolver) {
        this.tokenHubCnBaseUrl = normalizeOrDefault(tokenHubCnBaseUrl, DEFAULT_TOKENHUB_CN_BASE_URL);
        this.transport = transport;
        this.hostResolver = hostResolver;
    }

    public String probe(String secretKey, String secretValue) {
        if (secretValue == null || secretValue.isBlank() || isMaskedSecret(secretValue)) {
            return null;
        }
        ProbeTarget target = resolveProbeTarget(secretKey);
        if (target == null || !isSafeHttpsUrl(target.url())) {
            return null;
        }
        try {
            ProbeResponse response = transport.get(target.url(), secretValue.trim());
            if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            return extractName(response.body(), target.mode());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    ProbeTarget resolveProbeTarget(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            return null;
        }
        String normalized = secretKey.trim().toLowerCase(Locale.ROOT);
        if ("ai.tokenhub_cn.api_key".equals(normalized) || "ai.tencent_tokenhub.api_key".equals(normalized)) {
            return new ProbeTarget(buildV1ApiKeyUrl(tokenHubCnBaseUrl), ResponseMode.TOP_LEVEL_NAME);
        }
        if ("ai.xai.api_key".equals(normalized)) {
            return new ProbeTarget(buildV1ApiKeyUrl(XAI_BASE_URL), ResponseMode.TOP_LEVEL_NAME);
        }
        if ("ai.openrouter.api_key".equals(normalized)) {
            return new ProbeTarget(buildOpenRouterAuthKeyUrl(OPENROUTER_BASE_URL), ResponseMode.OPENROUTER_DATA_NAME);
        }
        return null;
    }

    private boolean isSafeHttpsUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return false;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        String lowerHost = host.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(lowerHost)
                || lowerHost.endsWith(".localhost")
                || lowerHost.endsWith(".local")
                || lowerHost.endsWith(".internal")
                || lowerHost.endsWith(".localdomain")) {
            return false;
        }
        try {
            InetAddress[] addrs = hostResolver.resolve(host);
            if (addrs == null || addrs.length == 0) {
                return false;
            }
            for (InetAddress addr : addrs) {
                if (addr == null || isForbiddenAddress(addr)) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean isForbiddenAddress(InetAddress addr) {
        if (addr.isAnyLocalAddress()
                || addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }
        byte[] raw = addr.getAddress();
        if (raw.length == 4) {
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;
            if (b0 == 0 || b0 == 127 || (b0 == 100 && b1 >= 64 && b1 <= 127) || (b0 == 169 && b1 == 254)) {
                return true;
            }
        }
        if (raw.length == 16) {
            int b0 = raw[0] & 0xFF;
            if ((b0 & 0xFE) == 0xFC) {
                return true;
            }
        }
        return false;
    }

    static String extractName(String body, ResponseMode mode) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Map<String, Object> root = ShortJson.map(body);
        if (root.isEmpty()) {
            return null;
        }
        if (mode == ResponseMode.TOP_LEVEL_NAME) {
            return normalizeName(root.get("name"));
        }
        Object dataObj = root.get("data");
        if (dataObj instanceof Map<?, ?> dataMap) {
            String name = normalizeName(dataMap.get("name"));
            if (name != null) {
                return name;
            }
            return normalizeName(dataMap.get("label"));
        }
        return null;
    }

    private static String normalizeName(Object raw) {
        if (!(raw instanceof String text)) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_PLATFORM_KEY_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_PLATFORM_KEY_NAME_LENGTH).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String buildV1ApiKeyUrl(String rawBase) {
        String base = stripTrailingSlashes(rawBase);
        if (base.endsWith("/v1")) {
            base = base.substring(0, base.length() - 3);
        }
        return base + "/v1/api-key";
    }

    private static String buildOpenRouterAuthKeyUrl(String rawBase) {
        String base = stripTrailingSlashes(rawBase);
        return base + "/auth/key";
    }

    private static String stripTrailingSlashes(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String normalizeOrDefault(String value, String fallback) {
        String stripped = stripTrailingSlashes(value);
        return stripped.isEmpty() ? fallback : stripped;
    }

    private static boolean isMaskedSecret(String value) {
        return value.contains("****");
    }

    private static ProbeTransport defaultTransport() {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        return (url, bearerToken) -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] bytes = response.body();
            if (bytes == null || bytes.length == 0) {
                return new ProbeResponse(response.statusCode(), "");
            }
            int len = Math.min(bytes.length, MAX_BODY_BYTES);
            return new ProbeResponse(response.statusCode(), new String(bytes, 0, len, StandardCharsets.UTF_8));
        };
    }
}
