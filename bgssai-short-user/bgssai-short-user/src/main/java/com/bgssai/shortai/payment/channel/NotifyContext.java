package com.bgssai.shortai.payment.channel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class NotifyContext {

    private final String rawBody;
    private final Map<String, String> headers;
    private final Map<String, String> formParams;

    public NotifyContext(String rawBody, Map<String, String> headers, Map<String, String> formParams) {
        this.rawBody = rawBody == null ? "" : rawBody;
        this.headers = normalizeHeaders(headers);
        this.formParams = formParams == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(formParams));
    }

    public static NotifyContext ofBody(String rawBody, Map<String, String> headers) {
        return new NotifyContext(rawBody, headers, null);
    }

    public static NotifyContext ofForm(Map<String, String> formParams, Map<String, String> headers) {
        return new NotifyContext(null, headers, formParams);
    }

    public String getRawBody() {
        return rawBody;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getFormParams() {
        return formParams;
    }

    public String header(String name) {
        if (name == null) {
            return null;
        }
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    private static Map<String, String> normalizeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> normalized = new LinkedHashMap<>(headers.size() * 2);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey() != null) {
                normalized.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
            }
        }
        return Collections.unmodifiableMap(normalized);
    }
}
