package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ComplianceLexicon {

    private static final String[][] BLOCK = {
            {"自杀", "VIOLENCE", "改为离开现场，不要描写自伤"},
            {"血腥屠杀", "VIOLENCE", "弱化为推搡或对峙"},
            {"未成年", "MINOR", "改为已成年角色并去掉相关描写"}
    };

    private static final String[][] WARN = {
            {"复仇", "VIOLENCE", "保留情绪但避免血腥细节"},
            {"打脸", "OTHER", "可用当众揭穿替代肢体羞辱"},
            {"血腥", "VIOLENCE", "减少画面暴力细节"}
    };

    private ComplianceLexicon() {
    }

    public static Map<String, Object> scan(String text, Integer sceneNo, Integer lineNo, Integer shotNo) {
        List<Map<String, Object>> hits = new ArrayList<>();
        String source = text == null ? "" : text;
        String worst = "PASS";
        for (String[] row : BLOCK) {
            if (source.contains(row[0])) {
                hits.add(hit("BLOCK", row[1], row[0], row[2], sceneNo, lineNo, shotNo));
                worst = "BLOCK";
            }
        }
        for (String[] row : WARN) {
            if (source.contains(row[0])) {
                hits.add(hit("WARN", row[1], row[0], row[2], sceneNo, lineNo, shotNo));
                if (!"BLOCK".equals(worst)) {
                    worst = "WARN";
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("risk_level", worst);
        result.put("hit_list", hits);
        return result;
    }

    public static Map<String, Object> merge(List<Map<String, Object>> parts) {
        List<Map<String, Object>> hits = new ArrayList<>();
        String worst = "PASS";
        for (Map<String, Object> part : parts) {
            if ("BLOCK".equals(part.get("risk_level"))) {
                worst = "BLOCK";
            } else if ("WARN".equals(part.get("risk_level")) && !"BLOCK".equals(worst)) {
                worst = "WARN";
            }
            Object list = part.get("hit_list");
            if (list instanceof List<?>) {
                for (Object item : (List<?>) list) {
                    if (item instanceof Map<?, ?>) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> hit = (Map<String, Object>) item;
                        hits.add(hit);
                    }
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("risk_level", worst);
        result.put("hit_list", hits);
        return result;
    }

    private static Map<String, Object> hit(String level, String category, String text, String suggestion,
            Integer sceneNo, Integer lineNo, Integer shotNo) {
        Map<String, Object> hit = new LinkedHashMap<>();
        hit.put("risk_level", level);
        hit.put("risk_category", category);
        hit.put("hit_text", text);
        hit.put("suggestion", suggestion);
        hit.put("scene_no", sceneNo);
        hit.put("line_no", lineNo);
        hit.put("shot_no", shotNo);
        return hit;
    }
}
