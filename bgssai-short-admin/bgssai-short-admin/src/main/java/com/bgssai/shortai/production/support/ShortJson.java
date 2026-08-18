package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ShortJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ShortJson() {
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("json write failed", ex);
        }
    }

    public static List<Map<String, Object>> list(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public static Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }
}
