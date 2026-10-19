package com.bgssai.shortai.configuration;

import java.util.Locale;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Browser navigations to unknown front-end paths must receive the SPA shell.
 * API prefixes stay JSON so clients can tell a missing endpoint from a page.
 */
public final class SpaDocumentRequests {

    private SpaDocumentRequests() {
    }

    public static boolean shouldServeIndex(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        Object errorUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (errorUri instanceof String value && !value.isBlank()) {
            uri = value;
        }
        return shouldServeIndex(request.getMethod(), uri, request.getHeader("Accept"));
    }

    public static boolean shouldServeIndex(String method, String uri, String accept) {
        if (method == null || !"GET".equalsIgnoreCase(method)) {
            return false;
        }
        if (uri == null || uri.isBlank() || "/index.html".equals(uri)) {
            return false;
        }
        if (uri.startsWith("/bgssai/") || "/bgssai".equals(uri)) {
            return false;
        }
        if (uri.startsWith("/api/") || "/api".equals(uri)) {
            return false;
        }
        if (accept == null) {
            return false;
        }
        return accept.toLowerCase(Locale.ROOT).contains("text/html");
    }
}
