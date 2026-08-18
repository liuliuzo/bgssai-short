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
