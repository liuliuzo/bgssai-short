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

    /**
     * Accepts ["id"] or [{"character_id":"id"}] / [{"scene_card_id":"id"}].
     */
    public static List<String> idList(String json, String... idKeys) {
        List<String> ids = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return ids;
        }
        try {
            List<Object> raw = MAPPER.readValue(json, new TypeReference<List<Object>>() {
            });
            for (Object item : raw) {
                if (item == null) {
                    continue;
                }
                if (item instanceof String text && !text.isBlank()) {
                    ids.add(text.trim());
                    continue;
                }
                if (item instanceof Map<?, ?> map) {
                    for (String key : idKeys) {
                        Object value = map.get(key);
                        if (value != null && !String.valueOf(value).isBlank()) {
                            ids.add(String.valueOf(value).trim());
                            break;
                        }
                    }
                }
            }
            return ids;
        } catch (Exception ex) {
            return ids;
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

    public static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static Integer integer(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static Double decimal(Object value, double fallback) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
