package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RhythmChecker {

    private RhythmChecker() {
    }

    public static Map<String, Object> check(List<Map<String, Object>> sceneList, int targetSec) {
        List<Map<String, Object>> issues = new ArrayList<>();
        int estimated = 0;
        int lineCount = 0;
        int longLines = 0;
        String firstText = "";
        String lastText = "";
        if (sceneList != null) {
            for (Map<String, Object> scene : sceneList) {
                List<Map<String, Object>> lines = ShortJson.list(ShortJson.toJson(scene.get("line_list")));
                if (scene.get("line_list") instanceof List<?>) {
                    lines = castList(scene.get("line_list"));
                }
                for (Map<String, Object> line : lines) {
                    lineCount++;
                    String text = ShortJson.text(line.get("line_text"));
                    int sec = ShortJson.integer(line.get("estimated_sec"), Math.max(2, (text.length() + 4) / 5));
                    estimated += sec;
                    if (firstText.isEmpty()) {
                        firstText = text;
                    }
                    lastText = text;
                    if (text.length() > 28) {
                        longLines++;
                    }
                }
            }
        }
        int hook = scoreHook(firstText);
        int beat = sceneList == null || sceneList.isEmpty() ? 40 : Math.min(100, 50 + sceneList.size() * 8);
        int dialogue = lineCount == 0 ? 50 : Math.max(40, 100 - longLines * 12);
        int ending = scoreEnding(lastText);
        if (targetSec > 0) {
            int diff = Math.abs(estimated - targetSec);
            if (diff * 100 > targetSec * 15) {
                Map<String, Object> issue = new LinkedHashMap<>();
                issue.put("scene_no", 0);
                issue.put("issue_desc", estimated > targetSec
                        ? ("超时 " + (estimated - targetSec) + " 秒")
                        : ("不足 " + (targetSec - estimated) + " 秒"));
                issues.add(issue);
                beat = Math.max(30, beat - 20);
            }
        }
        if (hook < 60) {
            Map<String, Object> issue = new LinkedHashMap<>();
            issue.put("scene_no", 1);
            issue.put("issue_desc", "开场钩子偏弱，建议前 3 秒给出冲突");
            issues.add(issue);
        }
        int total = (hook + beat + dialogue + ending) / 4;
        Map<String, Object> rhythm = new LinkedHashMap<>();
        rhythm.put("hook_score", hook);
        rhythm.put("beat_score", beat);
        rhythm.put("dialogue_score", dialogue);
        rhythm.put("ending_score", ending);
        rhythm.put("total_score", total);
        rhythm.put("issue_list", issues);
        rhythm.put("estimated_total_sec", estimated);
        return rhythm;
    }

    static int scoreHook(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("退婚") || t.contains("竟然") || t.contains("但是") || t.contains("凭什么") || t.contains("不可能")) {
            return 82;
        }
        return t.isEmpty() ? 40 : 58;
    }

    static int scoreEnding(String text) {
        if (text.contains("？") || text.contains("?") || text.contains("下集") || text.contains("等着")) {
            return 80;
        }
        return text.isEmpty() ? 40 : 62;
    }

    public static List<Map<String, Object>> extractRhythmMarkers(List<Map<String, Object>> sceneList) {
        List<Map<String, Object>> markers = new ArrayList<>();
        if (sceneList == null || sceneList.isEmpty()) {
            return markers;
        }
        int elapsedSec = 0;
        boolean firstLineChecked = false;
        Map<String, Object> lastLineMarker = null;
        for (Map<String, Object> scene : sceneList) {
            int sceneNo = ShortJson.integer(scene.get("scene_no"), 1);
            List<Map<String, Object>> lines = ShortJson.list(ShortJson.toJson(scene.get("line_list")));
            if (scene.get("line_list") instanceof List<?>) {
                lines = castList(scene.get("line_list"));
            }
            for (Map<String, Object> line : lines) {
                String text = ShortJson.text(line.get("line_text"));
                int sec = ShortJson.integer(line.get("estimated_sec"), Math.max(2, (text.length() + 4) / 5));
                if (!firstLineChecked) {
                    firstLineChecked = true;
                    if (scoreHook(text) >= 80) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("marker_type", "HOOK_3S");
                        m.put("scene_no", sceneNo);
                        m.put("at_sec", elapsedSec);
                        m.put("line_text", text);
                        markers.add(m);
                    }
                }
                if (text.contains("竟然") || text.contains("其实") || text.contains("反转")
                        || text.contains("没想到") || text.contains("真正的")) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("marker_type", "REVERSAL");
                    m.put("scene_no", sceneNo);
                    m.put("at_sec", elapsedSec);
                    m.put("line_text", text);
                    markers.add(m);
                }
                elapsedSec += sec;
                if (scoreEnding(text) >= 80) {
                    lastLineMarker = new LinkedHashMap<>();
                    lastLineMarker.put("marker_type", "CLIFFHANGER");
                    lastLineMarker.put("scene_no", sceneNo);
                    lastLineMarker.put("at_sec", elapsedSec);
                    lastLineMarker.put("line_text", text);
                } else {
                    lastLineMarker = null;
                }
            }
        }
        if (lastLineMarker != null) {
            markers.add(lastLineMarker);
        }
        return markers;
    }

    public static Map<String, Object> validateAspectRatioAndSafeZone(String aspectRatio, Integer subtitleMarginPct) {
        String normalized = (aspectRatio == null || aspectRatio.isBlank()) ? "9:16" : aspectRatio.trim();
        boolean ratioValid = "9:16".equals(normalized)
                || "16:9".equals(normalized)
                || "1:1".equals(normalized)
                || "3:4".equals(normalized);
        int margin = subtitleMarginPct == null ? 15 : subtitleMarginPct;
        boolean safeZoneValid = !"9:16".equals(normalized) || (margin >= 8 && margin <= 25);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("aspect_ratio", normalized);
        out.put("aspect_ratio_valid", ratioValid);
        out.put("subtitle_margin_pct", margin);
        out.put("safe_zone_valid", safeZoneValid);
        out.put("valid", ratioValid && safeZoneValid);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object raw) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(raw instanceof List<?>)) {
            return out;
        }
        for (Object item : (List<?>) raw) {
            if (item instanceof Map<?, ?>) {
                out.add((Map<String, Object>) item);
            }
        }
        return out;
    }
}
