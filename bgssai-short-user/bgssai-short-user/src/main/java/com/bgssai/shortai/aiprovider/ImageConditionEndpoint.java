package com.bgssai.shortai.aiprovider;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Decides whether an administrator-configured image-to-video base URL may be called.
 * <p>
 * Contract v0.2-validated-20261111-1137Z: the model process listens on loopback of the
 * LLM host only. This class never supplies a default URL. An application server's own
 * loopback is rejected. Port 8007 on the LLM host's public or private address is the
 * unexposed listener and is rejected. Any other origin written into the model row,
 * including a later dedicated gateway, is eligible. Users cannot supply this URL.
 */
public final class ImageConditionEndpoint {

    static final int UNEXPOSED_LISTENER_PORT = 8007;

    static final Set<String> DENIED_HOSTS = Set.of(
            "localhost",
            "localhost.localdomain",
            "ip6-localhost",
            "metadata.google.internal",
            "metadata.google",
            CompanyCompatGateway.HOST);

    /** Addresses that are not a cross-host route while the listener stays on loopback. */
    static final Set<String> UNEXPOSED_LISTENER_HOSTS = Set.of(
            "124.70.197.39",
            "172.31.10.199");

    private ImageConditionEndpoint() {
    }

    public record Origin(String value) {
    }

    /**
     * @return the origin without a trailing slash when the URL is an admitted
     *         server-side base; empty when it is blank or not an approved route
     */
    public static Optional<Origin> admit(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
            return Optional.empty();
        }
        if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
            return Optional.empty();
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return Optional.empty();
        }
        String path = uri.getPath();
        if (path != null && !path.isBlank() && !"/".equals(path)) {
            return Optional.empty();
        }
        String host = uri.getHost();
        if (host == null || host.isBlank() || !hostAllowed(host) || unexposedListener(host, uri.getPort())) {
            return Optional.empty();
        }
        if (TokenHubClientSupport.isTokenHubHost(raw) || TokenHubClientSupport.isOfficialHub(raw)
                || CompanyCompatGateway.allows(raw)) {
            return Optional.empty();
        }
        int port = uri.getPort();
        String origin = scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT);
        if (port != -1) {
            origin = origin + ":" + port;
        }
        return Optional.of(new Origin(origin));
    }

    private static boolean hostAllowed(String host) {
        String normalized = normalize(host);
        if (DENIED_HOSTS.contains(normalized) || normalized.endsWith(".local")
                || normalized.contains("tokenhub")) {
            return false;
        }
        if (isIpLiteral(normalized)) {
            try {
                InetAddress address = InetAddress.getByName(normalized);
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isMulticastAddress()) {
                    return false;
                }
            } catch (UnknownHostException ex) {
                return false;
            }
        }
        return true;
    }

    private static boolean unexposedListener(String host, int port) {
        return port == UNEXPOSED_LISTENER_PORT && UNEXPOSED_LISTENER_HOSTS.contains(normalize(host));
    }

    private static String normalize(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean isIpLiteral(String host) {
        if (host.indexOf(':') >= 0) {
            return host.chars().allMatch(ch -> Character.digit(ch, 16) >= 0 || ch == ':' || ch == '.');
        }
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)) {
                return false;
            }
            int value = Integer.parseInt(part);
            if (value < 0 || value > 255) {
                return false;
            }
        }
        return true;
    }
}
