package com.bgssai.shortai.configuration;

/**
 * 中文产品没有英文界面。{@code /en} 只是 {@code /login} 的别名。
 * 查询串原样带到登录页；浏览器在 302 时自己保留 hash。
 */
public final class EnglishEntryRedirect {

    private EnglishEntryRedirect() {
    }

    public static String location(String path, String rawQuery) {
        if (!isEnglishPrefix(path)) {
            return null;
        }
        if (rawQuery == null || rawQuery.isEmpty() || rawQuery.indexOf('\r') >= 0 || rawQuery.indexOf('\n') >= 0) {
            return "/login";
        }
        return "/login?" + rawQuery;
    }

    public static boolean isEnglishPrefix(String path) {
        return "/en".equals(path) || "/en/".equals(path) || (path != null && path.startsWith("/en/"));
    }

    public static String pathWithinApplication(String requestUri, String contextPath) {
        String uri = requestUri == null ? "" : requestUri;
        String context = contextPath == null ? "" : contextPath;
        if (!context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        int semi = uri.indexOf(';');
        if (semi >= 0) {
            uri = uri.substring(0, semi);
        }
        if (uri.isEmpty()) {
            return "/";
        }
        return uri;
    }
}
