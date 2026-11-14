package com.bgssai.shortai.loginlog.detail;

import java.net.InetAddress;
import java.net.URI;

import org.slf4j.MDC;

import com.bgssai.shortai.common.logging.MdcSupport;
import com.bgssai.shortai.loginlog.location.IpLiteral;
import com.bgssai.shortai.loginlog.location.LoginClientIp;

import jakarta.servlet.http.HttpServletRequest;
import javax.net.ssl.SSLSession;

/**
 * Facts already present on the login request. Unknown headers stay null.
 * Query strings are never kept, so an OAuth code cannot land in the log.
 */
public final class LoginRequestFacts {

    private final String ip;
    private final String userAgent;
    private final String browserName;
    private final String osName;
    private final String deviceType;
    private final String deviceLabel;
    private final String scheme;
    private final String tlsProtocol;
    private final String refererHost;
    private final String entryPath;
    private final String requestId;
    private final String acceptLanguage;
    private final String clientTimezone;
    private final String clientHints;
    private final String clientChannel;

    private LoginRequestFacts(String ip, String userAgent, String browserName, String osName, String deviceType,
                              String deviceLabel, String scheme, String tlsProtocol, String refererHost,
                              String entryPath, String requestId, String acceptLanguage, String clientTimezone,
                              String clientHints, String clientChannel) {
        this.ip = ip;
        this.userAgent = userAgent;
        this.browserName = browserName;
        this.osName = osName;
        this.deviceType = deviceType;
        this.deviceLabel = deviceLabel;
        this.scheme = scheme;
        this.tlsProtocol = tlsProtocol;
        this.refererHost = refererHost;
        this.entryPath = entryPath;
        this.requestId = requestId;
        this.acceptLanguage = acceptLanguage;
        this.clientTimezone = clientTimezone;
        this.clientHints = clientHints;
        this.clientChannel = clientChannel;
    }

    public static LoginRequestFacts read(HttpServletRequest request, String defaultChannel) {
        if (request == null) {
            return new LoginRequestFacts(null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, channel(null, defaultChannel));
        }
        String ua = clip(dropSecret(request.getHeader("User-Agent")), 512);
        LoginUserAgent.Summary summary = LoginUserAgent.parse(ua);
        return new LoginRequestFacts(
                LoginClientIp.resolve(request),
                ua,
                summary.getBrowserName(),
                summary.getOsName(),
                summary.getDeviceType(),
                summary.getDeviceLabel(),
                scheme(request),
                tls(request),
                refererHost(request),
                entryPath(request),
                requestId(),
                language(request),
                timezone(request),
                hints(request),
                channel(request, defaultChannel));
    }

    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public String getBrowserName() { return browserName; }
    public String getOsName() { return osName; }
    public String getDeviceType() { return deviceType; }
    public String getDeviceLabel() { return deviceLabel; }
    public String getScheme() { return scheme; }
    public String getTlsProtocol() { return tlsProtocol; }
    public String getRefererHost() { return refererHost; }
    public String getEntryPath() { return entryPath; }
    public String getRequestId() { return requestId; }
    public String getAcceptLanguage() { return acceptLanguage; }
    public String getClientTimezone() { return clientTimezone; }
    public String getClientHints() { return clientHints; }
    public String getClientChannel() { return clientChannel; }

    static String dropSecret(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.startsWith("eyJ")) return null;
        if (trimmed.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) return null;
        return trimmed;
    }

    private static String scheme(HttpServletRequest request) {
        String scheme = request.getScheme() == null ? "" : request.getScheme().toLowerCase();
        InetAddress peer = IpLiteral.parse(request.getRemoteAddr());
        if (peer != null && peer.isLoopbackAddress()) {
            String forwarded = request.getHeader("X-Forwarded-Proto");
            if (forwarded != null && !forwarded.isBlank()) {
                String first = forwarded.split(",")[0].trim().toLowerCase();
                if ("https".equals(first) || "http".equals(first)) {
                    scheme = first;
                }
            }
        }
        return "https".equals(scheme) || "http".equals(scheme) ? scheme : null;
    }

    private static String tls(HttpServletRequest request) {
        Object session = request.getAttribute("jakarta.servlet.request.ssl_session");
        if (session instanceof SSLSession ssl && ssl.getProtocol() != null
                && ssl.getProtocol().matches("TLSv1(?:\\.[0-3])?|SSLv[23]")) {
            return ssl.getProtocol();
        }
        Object version = request.getAttribute("org.apache.tomcat.util.net.secure_protocol_version");
        if (version instanceof String text && text.matches("TLSv1(?:\\.[0-3])?")) {
            return text;
        }
        return null;
    }

    private static String refererHost(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            referer = request.getHeader("Referrer");
        }
        if (referer == null || referer.isBlank() || referer.length() > 2048) {
            return null;
        }
        try {
            URI uri = URI.create(referer.trim());
            if (uri.getUserInfo() != null || uri.getHost() == null || uri.getHost().isBlank()) {
                return null;
            }
            return clip(uri.getHost(), 255);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String entryPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/")) return null;
        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);
        if (path.toLowerCase().contains("code=")) return null;
        return clip(path, 255);
    }

    private static String requestId() {
        String trace = MDC.get(MdcSupport.TRACE_ID);
        if (trace == null) return null;
        String trimmed = trace.trim();
        if (!trimmed.matches("[A-Za-z0-9-]{8,64}")) return null;
        if (trimmed.startsWith("eyJ")) return null;
        return trimmed;
    }

    private static String language(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header == null || header.isBlank()) return null;
        String tag = header.split(",")[0].trim();
        int quality = tag.indexOf(';');
        if (quality >= 0) tag = tag.substring(0, quality).trim();
        return tag.matches("(?i)[a-z]{1,8}(-[a-z0-9]{1,8})*") ? tag : null;
    }

    private static String timezone(HttpServletRequest request) {
        for (String name : new String[] {"X-Client-Timezone", "X-Timezone"}) {
            String value = request.getHeader(name);
            if (value == null) continue;
            String trimmed = value.trim();
            if (trimmed.matches("UTC|GMT|[A-Za-z]+/[A-Za-z0-9_+\\-]+|[+-]\\d{2}:\\d{2}")) {
                return clip(trimmed, 64);
            }
        }
        return null;
    }

    private static String hints(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        String platform = unquote(request.getHeader("Sec-CH-UA-Platform"));
        if (platform != null && platform.matches("[A-Za-z0-9 ._-]{1,32}")) {
            add(sb, "platform=" + platform);
        }
        String mobile = request.getHeader("Sec-CH-UA-Mobile");
        if ("?0".equals(mobile) || "?1".equals(mobile)) {
            add(sb, "mobile=" + mobile.charAt(1));
        }
        String width = request.getHeader("Sec-CH-Viewport-Width");
        if (width == null || width.isBlank()) {
            width = request.getHeader("Viewport-Width");
        }
        if (width != null && width.trim().matches("\\d{2,5}")) {
            add(sb, "viewport=" + width.trim());
        }
        return sb.length() == 0 ? null : clip(sb.toString(), 160);
    }

    private static String channel(HttpServletRequest request, String defaultChannel) {
        String fallback = "web".equals(defaultChannel) || "admin".equals(defaultChannel) ? defaultChannel : "web";
        if (request == null) return fallback;
        String hinted = request.getHeader("X-Bgssai-Client");
        if (hinted != null && hinted.trim().matches("(?i)web|admin|app")) {
            return hinted.trim().toLowerCase();
        }
        return fallback;
    }

    private static String unquote(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void add(StringBuilder sb, String part) {
        if (sb.length() > 0) sb.append("; ");
        sb.append(part);
    }

    private static String clip(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
