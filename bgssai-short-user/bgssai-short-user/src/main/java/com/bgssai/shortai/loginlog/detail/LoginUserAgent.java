package com.bgssai.shortai.loginlog.detail;

/**
 * Reads a User-Agent into browser, OS, and device type.
 * Unrecognized strings stay empty instead of being guessed.
 */
public final class LoginUserAgent {

    public static final class Summary {
        private final String browserName;
        private final String osName;
        private final String deviceType;
        private final String deviceLabel;

        Summary(String browserName, String osName, String deviceType, String deviceLabel) {
            this.browserName = browserName;
            this.osName = osName;
            this.deviceType = deviceType;
            this.deviceLabel = deviceLabel;
        }

        public String getBrowserName() { return browserName; }
        public String getOsName() { return osName; }
        public String getDeviceType() { return deviceType; }
        public String getDeviceLabel() { return deviceLabel; }
    }

    private LoginUserAgent() {
    }

    public static Summary parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new Summary(null, null, null, null);
        }
        String ua = userAgent.length() > 1024 ? userAgent.substring(0, 1024) : userAgent;
        String browser = browser(ua);
        String os = os(ua);
        String device = device(ua, browser, os);
        return new Summary(browser, os, device, label(browser, os, device));
    }

    public static String deviceKey(String browserName, String osName, String deviceType) {
        if (blank(browserName) && blank(osName)) {
            return null;
        }
        String family = blank(osName) ? "" : osName.split(" ")[0];
        return norm(browserName) + "|" + family + "|" + norm(deviceType);
    }

    private static String browser(String ua) {
        if (contains(ua, "MicroMessenger")) {
            return "微信";
        }
        if (contains(ua, "Edg/")) {
            return withMajor("Edge", token(ua, "Edg/"));
        }
        if (contains(ua, "OPR/") || contains(ua, "Opera")) {
            return withMajor("Opera", token(ua, "OPR/"));
        }
        if (contains(ua, "Firefox/")) {
            return withMajor("Firefox", token(ua, "Firefox/"));
        }
        if (contains(ua, "Chrome/") && !contains(ua, "Chromium")) {
            return withMajor("Chrome", token(ua, "Chrome/"));
        }
        if (contains(ua, "Safari/") && contains(ua, "Version/")) {
            return withMajor("Safari", token(ua, "Version/"));
        }
        return null;
    }

    private static String os(String ua) {
        if (contains(ua, "Windows NT 10.0")) return "Windows 10";
        if (contains(ua, "Windows NT 6.3")) return "Windows 8.1";
        if (contains(ua, "Windows NT 6.1")) return "Windows 7";
        if (contains(ua, "Windows")) return "Windows";
        if (contains(ua, "Android")) {
            String ver = token(ua, "Android ");
            return ver == null ? "Android" : "Android " + major(ver);
        }
        if (contains(ua, "iPhone") || contains(ua, "iPad") || contains(ua, "CPU iPhone OS") || contains(ua, "CPU OS")) {
            String ver = token(ua, "OS ");
            if (ver == null) {
                ver = token(ua, "iPhone OS ");
            }
            return ver == null ? "iOS" : "iOS " + major(ver.replace('_', '.'));
        }
        if (contains(ua, "Mac OS X")) {
            String ver = token(ua, "Mac OS X ");
            if (ver == null) return "macOS";
            String dotted = majorMinor(ver.replace('_', '.'));
            return dotted == null ? "macOS" : "macOS " + dotted;
        }
        if (contains(ua, "Linux")) return "Linux";
        return null;
    }

    private static String device(String ua, String browser, String os) {
        String lower = ua.toLowerCase();
        if (lower.contains("bot") || lower.contains("spider") || lower.contains("crawler")) {
            return "bot";
        }
        if (contains(ua, "iPad") || lower.contains("tablet")) {
            return "tablet";
        }
        if (contains(ua, "Mobile") || contains(ua, "iPhone") || contains(ua, "Android")) {
            return "mobile";
        }
        if (browser != null || os != null) {
            return "desktop";
        }
        return null;
    }

    private static String label(String browser, String os, String device) {
        String kind = switch (device == null ? "" : device) {
            case "desktop" -> "桌面";
            case "mobile" -> "手机";
            case "tablet" -> "平板";
            case "bot" -> "爬虫";
            default -> null;
        };
        StringBuilder sb = new StringBuilder();
        append(sb, browser);
        append(sb, os);
        append(sb, kind);
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (blank(part)) return;
        if (sb.length() > 0) sb.append(" · ");
        sb.append(part);
    }

    private static String withMajor(String name, String version) {
        String major = major(version);
        return major == null ? name : name + " " + major;
    }

    private static String token(String ua, String marker) {
        int at = ua.indexOf(marker);
        if (at < 0) return null;
        int start = at + marker.length();
        int end = start;
        while (end < ua.length()) {
            char c = ua.charAt(end);
            if ((c >= '0' && c <= '9') || c == '.' || c == '_') {
                end++;
            } else {
                break;
            }
        }
        if (end == start) return null;
        return ua.substring(start, end);
    }

    private static String major(String version) {
        if (version == null || version.isBlank()) return null;
        int dot = version.indexOf('.');
        String head = dot < 0 ? version : version.substring(0, dot);
        return head.chars().allMatch(Character::isDigit) ? head : null;
    }

    private static String majorMinor(String version) {
        String major = major(version);
        if (major == null) return null;
        int dot = version.indexOf('.');
        if (dot < 0 || dot + 1 >= version.length()) return major;
        int next = version.indexOf('.', dot + 1);
        String minor = next < 0 ? version.substring(dot + 1) : version.substring(dot + 1, next);
        return minor.chars().allMatch(Character::isDigit) ? major + "." + minor : major;
    }

    private static boolean contains(String ua, String token) {
        return ua.contains(token);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String norm(String value) {
        return value == null ? "" : value.trim();
    }
}
