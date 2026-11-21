package com.bgssai.shortai.useraimodel.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.production.support.ShortJson;

/**
 * Probes supported BYOK platform key names safely without blocking key saves or updates.
 */
@Component
public class PlatformKeyNameProber {

    static final int MAX_PLATFORM_KEY_NAME_LENGTH = 128;
    private static final int TIMEOUT_SECONDS = 3;
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private static final String DEFAULT_XAI_BASE_URL = "https://api.x.ai";
    private static final String DEFAULT_OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
    private static final List<String> TOKENHUB_HOST_SUFFIXES = List.of(
            "bgssai-tokenhub.cn",
            "bgssai-tokenhub.com",
            "bgssai-tokenhub-global.com",
            "developers.bgssai.cn",
            "developers.bgssai.com");

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

    private final ProbeTransport transport;
    private final HostResolver hostResolver;

    @Autowired
    public PlatformKeyNameProber() {
        this(defaultTransport(), InetAddress::getAllByName);
    }

    PlatformKeyNameProber(ProbeTransport transport, HostResolver hostResolver) {
        this.transport = transport;
        this.hostResolver = hostResolver;
    }

    public String probe(String providerCode, String baseUrl, String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("****")) {
            return null;
        }
        ProbeTarget target = resolveProbeTarget(providerCode, baseUrl);
        if (target == null || !isSafeHttpsUrl(target.url())) {
            return null;
        }
        try {
            ProbeResponse response = transport.get(target.url(), apiKey.trim());
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

    ProbeTarget resolveProbeTarget(String providerCode, String baseUrl) {
        String provider = providerCode == null ? "" : providerCode.trim().toUpperCase(Locale.ROOT);
        String rawBase = stripTrailingSlashes(baseUrl);
        if (!rawBase.isEmpty()) {
            URI uri = parseHttpsUriStructure(rawBase);
            if (uri == null) {
                return null;
            }
            String lowerHost = uri.getHost().trim().toLowerCase(Locale.ROOT);
            if (isTokenHubHost(lowerHost)) {
                return new ProbeTarget(buildV1ApiKeyUrl(rawBase), ResponseMode.TOP_LEVEL_NAME);
            }
            if (isXaiHost(lowerHost) || isXaiProvider(provider)) {
                return new ProbeTarget(buildV1ApiKeyUrl(rawBase), ResponseMode.TOP_LEVEL_NAME);
            }
            if (isOpenRouterHost(lowerHost) || isOpenRouterProvider(provider)) {
                return new ProbeTarget(buildOpenRouterAuthKeyUrl(uri, rawBase), ResponseMode.OPENROUTER_DATA_NAME);
            }
            return null;
        }
        if (isXaiProvider(provider)) {
            return new ProbeTarget(buildV1ApiKeyUrl(DEFAULT_XAI_BASE_URL), ResponseMode.TOP_LEVEL_NAME);
        }
        if (isOpenRouterProvider(provider)) {
            return new ProbeTarget(DEFAULT_OPENROUTER_BASE_URL + "/auth/key", ResponseMode.OPENROUTER_DATA_NAME);
        }
        return null;
    }

    private boolean isSafeHttpsUrl(String rawUrl) {
        URI uri = parseHttpsUriStructure(rawUrl);
        if (uri == null) {
            return false;
        }
        String host = uri.getHost();
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

    private static URI parseHttpsUriStructure(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return null;
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return null;
        }
        String host = uri.getHost();
        return (host == null || host.isBlank()) ? null : uri;
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

    private static boolean isTokenHubHost(String lowerHost) {
        for (String domain : TOKENHUB_HOST_SUFFIXES) {
            if (matchesDomain(lowerHost, domain)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isXaiHost(String lowerHost) {
        return matchesDomain(lowerHost, "api.x.ai");
    }

    private static boolean isOpenRouterHost(String lowerHost) {
        return matchesDomain(lowerHost, "openrouter.ai");
    }

    private static boolean isXaiProvider(String provider) {
        return "XAI".equals(provider) || "GROK".equals(provider);
    }

    private static boolean isOpenRouterProvider(String provider) {
        return "OPENROUTER".equals(provider);
    }

    private static boolean matchesDomain(String lowerHost, String domain) {
        return lowerHost.equals(domain) || lowerHost.endsWith("." + domain);
    }

    private static String buildV1ApiKeyUrl(String rawBase) {
        String base = stripTrailingSlashes(rawBase);
        if (base.endsWith("/v1")) {
            base = base.substring(0, base.length() - 3);
        }
        return base + "/v1/api-key";
    }

    private static String buildOpenRouterAuthKeyUrl(URI uri, String rawBase) {
        String path = stripTrailingSlashes(uri.getPath());
        String lowerHost = uri.getHost().trim().toLowerCase(Locale.ROOT);
        if (isOpenRouterHost(lowerHost)) {
            if (path.isEmpty()) {
                return rawBase + "/api/v1/auth/key";
            }
            if ("/api".equals(path)) {
                return rawBase + "/v1/auth/key";
            }
        }
        return rawBase + "/auth/key";
    }

    private static String stripTrailingSlashes(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
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
