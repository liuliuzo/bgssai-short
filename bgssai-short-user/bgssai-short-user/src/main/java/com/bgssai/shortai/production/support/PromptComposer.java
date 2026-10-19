package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bgssai.shortai.model.ShortShot;

/**
 * 人话分镜、白话解读、模型提示词共用同一套镜头语义，避免三处各写各的。
 */
public final class PromptComposer {

    private PromptComposer() {
    }

    public static String compose(String desc, String dialogue, String extra, String visualStyle) {
        StringBuilder sb = new StringBuilder();
        sb.append("vertical 9:16 cinematic still, ").append(styleName(visualStyle));
        if (desc != null && !desc.isBlank()) {
            sb.append("，").append(desc.trim());
        }
        if (dialogue != null && !dialogue.isBlank()) {
            sb.append("，对白：").append(dialogue.trim());
        }
        if (extra != null && !extra.isBlank()) {
            sb.append("，").append(extra.trim());
        }
        sb.append("，photorealistic short drama, no watermark, no extra fingers");
        return sb.toString();
    }

    public static String compose(ShortShot shot, String visualStyle) {
        if (shot == null) {
            return compose(null, null, null, visualStyle);
        }
        String extra = join("，",
                sizeName(shot.getShotSize()),
                cameraName(shot.getCameraMove()),
                lightName(shot.getLighting()),
                emotionName(shot.getEmotion()),
                shot.getExtraPrompt());
        return compose(shot.getShotDesc(), shot.getDialogueText(), extra, visualStyle);
    }

    /**
     * 五要素人话分镜：谁 / 在哪 / 做什么 / 怎么拍 / 什么感觉。不含提示词术语。
     */
    public static String previzText(ShortShot shot) {
        if (shot == null) {
            return "";
        }
        String who = firstSentence(shot.getShotDesc());
        if (who.isBlank() && shot.getDialogueText() != null) {
            who = "对白角色";
        }
        String where = placeOf(shot.getShotDesc());
        String action = shot.getShotDesc() == null ? "" : shot.getShotDesc().trim();
        String how = join("，", sizeName(shot.getShotSize()), cameraName(shot.getCameraMove()));
        String feel = join("，", lightName(shot.getLighting()), emotionName(shot.getEmotion()));
        StringBuilder sb = new StringBuilder();
        sb.append("谁：").append(blankTo(who, "未写清出场人物")).append("。");
        sb.append("在哪：").append(blankTo(where, "未写清场景")).append("。");
        sb.append("做什么：").append(blankTo(action, "未写清动作")).append("。");
        sb.append("怎么拍：").append(blankTo(how, "中景，固定机位")).append("。");
        sb.append("什么感觉：").append(blankTo(feel, "情绪待定"));
        if (shot.getDialogueText() != null && !shot.getDialogueText().isBlank()) {
            sb.append("。台词：").append(shot.getDialogueText().trim());
        }
        return sb.toString();
    }

    /**
     * 给创作者看的一句白话：按当前提示词，观众会看到什么。
     */
    public static String lookPlain(ShortShot shot) {
        if (shot == null) {
            return "还没有这一镜的画面构想。";
        }
        String desc = shot.getShotDesc() == null ? "" : shot.getShotDesc().trim();
        String size = sizeName(shot.getShotSize());
        String camera = cameraName(shot.getCameraMove());
        String feel = join("、", lightName(shot.getLighting()), emotionName(shot.getEmotion()));
        StringBuilder sb = new StringBuilder("观众会看到");
        if (!size.isBlank()) {
            sb.append(size);
        }
        if (!desc.isBlank()) {
            sb.append("：").append(trimTo(desc, 48));
        } else {
            sb.append("一段尚未写清的画面");
        }
        if (!camera.isBlank()) {
            sb.append("。镜头").append(camera);
        }
        if (!feel.isBlank()) {
            sb.append("，").append(feel);
        }
        if (shot.getDialogueText() != null && !shot.getDialogueText().isBlank()) {
            sb.append("。字幕：「").append(trimTo(shot.getDialogueText().trim(), 24)).append("」");
        }
        sb.append("。");
        return sb.toString();
    }

    /** Source tags for the composed image prompt, shown beside the storyboard. */
    public static List<Map<String, String>> segments(ShortShot shot) {
        List<Map<String, String>> list = new ArrayList<>();
        if (shot == null) {
            return list;
        }
        addSegment(list, "DESC", "画面", shot.getShotDesc());
        addSegment(list, "DIALOGUE", "对白", shot.getDialogueText());
        addSegment(list, "SIZE", "景别", sizeName(shot.getShotSize()));
        addSegment(list, "CAMERA", "运镜", cameraName(shot.getCameraMove()));
        addSegment(list, "LIGHT", "光线", lightName(shot.getLighting()));
        addSegment(list, "EMOTION", "情绪", emotionName(shot.getEmotion()));
        addSegment(list, "EXTRA", "补充", shot.getExtraPrompt());
        return list;
    }

    private static void addSegment(List<Map<String, String>> list, String source, String label, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Map<String, String> item = new LinkedHashMap<>();
        item.put("source", source);
        item.put("label", label);
        item.put("text", text.trim());
        list.add(item);
    }

    public static Map<String, Object> check(ShortShot shot, String prompt) {
        String p = prompt == null ? "" : prompt;
        List<Map<String, Object>> issues = new ArrayList<>();
        List<Map<String, Object>> elements = new ArrayList<>();
        addElement(elements, "SUBJECT", "主体", containsAny(p, shot == null ? null : shot.getShotDesc(), "人", "女", "男", "谁"));
        addElement(elements, "COSTUME", "服装", containsAny(p, "服装", "穿", "衣", "妆"));
        addElement(elements, "SCENE", "场景", containsAny(p, shot == null ? null : shot.getShotDesc(), "会议室", "室内", "走廊", "场景", "在哪"));
        addElement(elements, "ACTION", "动作", containsAny(p, "推", "走", "看", "站", "坐", "做"));
        addElement(elements, "SHOT_SIZE", "景别", containsAny(p, "特写", "近景", "中景", "全景", "景") || (shot != null && shot.getShotSize() != null));
        addElement(elements, "CAMERA", "运镜", containsAny(p, "推进", "固定", "跟拍", "摇", "运镜") || (shot != null && shot.getCameraMove() != null));
        addElement(elements, "LIGHTING", "光线", containsAny(p, "光", "夜", "日", "冷蓝", "暖") || (shot != null && shot.getLighting() != null));
        addElement(elements, "STYLE", "风格", containsAny(p, "电影", "写实", "短剧", "9:16", "vertical"));
        int present = 0;
        for (Map<String, Object> el : elements) {
            if (Integer.valueOf(1).equals(el.get("present_flag"))) {
                present++;
            } else {
                issues.add(issue("MISSING", String.valueOf(el.get("element_name")) + "不完整",
                        "在人话分镜里补上「" + el.get("element_name") + "」，系统会自动写进提示词"));
            }
        }
        if (p.isBlank()) {
            issues.add(issue("EMPTY", "还没有可生成的画面描述", "先用人话写清这一镜，再出图"));
        } else if (p.length() < 16) {
            issues.add(issue("TOO_SHORT", "描述太短，模型只能猜", "补充谁在哪做什么、镜头怎么走"));
        }
        if (containsAny(p, "特写") && containsAny(p, "全身", "全景")) {
            issues.add(issue("CONFLICT", "同时写了特写和全身，模型会二选一", "只保留一个景别"));
        }
        if (containsAny(p, "固定") && containsAny(p, "快速推进", "猛推")) {
            issues.add(issue("CONFLICT", "固定机位和快速推进互相打架", "改成其中一种运镜"));
        }
        if (containsAny(p, "夜景", "夜晚") && containsAny(p, "日光", "大白天", "明亮阳光")) {
            issues.add(issue("CONFLICT", "夜景和日光写在同一镜", "统一成一种光线"));
        }
        if (!containsAny(p, "水印", "多指", "negative", "避免")) {
            issues.add(issue("NEGATIVE", "未给否定词，容易出现多指和水印", "已建议在提示词末尾自动带上「无水印、不多指」"));
        }
        int score = Math.max(0, Math.min(100, present * 12 + (p.length() > 40 ? 4 : 0) - issues.size() * 4));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("look_plain", lookPlain(shot));
        result.put("prompt_score", score);
        result.put("pass_flag", score >= 70 && issues.stream().noneMatch(i -> "CONFLICT".equals(i.get("issue_type"))
                || "EMPTY".equals(i.get("issue_type"))) ? 1 : 0);
        result.put("element_list", elements);
        result.put("issue_list", issues);
        return result;
    }

    public static String sizeName(String code) {
        if (code == null) {
            return "";
        }
        return switch (code) {
            case "ECU", "CLOSE_UP", "CU" -> "特写";
            case "CLOSE", "CLOSEUP" -> "近景";
            case "MEDIUM", "MS" -> "中景";
            case "WIDE", "WS", "LONG" -> "全景";
            default -> code;
        };
    }

    public static String cameraName(String code) {
        if (code == null) {
            return "";
        }
        return switch (code) {
            case "FIXED", "STATIC" -> "固定不动";
            case "PUSH" -> "缓缓推进";
            case "PULL" -> "慢慢拉远";
            case "PAN" -> "横向摇过";
            case "FOLLOW" -> "跟着人物走";
            case "TILT" -> "上下摇";
            default -> code;
        };
    }

    public static String emotionName(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return switch (code) {
            case "COLD" -> "冷静克制";
            case "ANGRY" -> "怒意压着";
            case "CALM" -> "平静";
            case "SHOCK" -> "震惊";
            case "TENSE" -> "紧绷";
            case "SAD" -> "难过";
            case "HAPPY" -> "轻松";
            default -> code;
        };
    }

    public static String lightName(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return switch (code) {
            case "COLD_BLUE", "COLD" -> "冷蓝顶光";
            case "WARM" -> "暖黄室内光";
            case "NIGHT" -> "夜色";
            case "DAY" -> "日光";
            case "SOFT" -> "柔光";
            default -> code;
        };
    }

    public static String styleName(String code) {
        if (code == null || code.isBlank()) {
            return "现代都市短剧";
        }
        // 取值对齐原型 docs/demo-static/web/user/work-create.html 的画风下拉。
        return switch (code) {
            case "MODERN_URBAN" -> "现代都市写实";
            case "CINEMATIC" -> "电影感质感";
            case "ANCIENT_CHINESE", "ANCIENT" -> "古装剧质感";
            case "ANIME" -> "动漫风格";
            case "SWEET" -> "甜宠柔光";
            default -> code;
        };
    }

    private static void addElement(List<Map<String, Object>> list, String code, String name, boolean present) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("element", code);
        m.put("element_name", name);
        m.put("present_flag", present ? 1 : 0);
        list.add(m);
    }

    private static Map<String, Object> issue(String type, String desc, String suggestion) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issue_type", type);
        m.put("issue_code", type);
        m.put("issue_desc", desc);
        m.put("suggestion", suggestion);
        return m;
    }

    private static boolean containsAny(String text, String... keys) {
        if (text == null || text.isBlank() || keys == null) {
            return false;
        }
        String t = text;
        for (String k : keys) {
            if (k != null && !k.isBlank() && t.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        if (parts == null) {
            return "";
        }
        for (String p : parts) {
            if (p == null || p.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(p.trim());
        }
        return sb.toString();
    }

    private static String blankTo(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static String firstSentence(String desc) {
        if (desc == null || desc.isBlank()) {
            return "";
        }
        String t = desc.trim();
        int cut = indexOfAny(t, "，", ",", "。", "/", " ");
        if (cut > 0 && cut <= 16) {
            return t.substring(0, cut);
        }
        return trimTo(t, 16);
    }

    private static String placeOf(String desc) {
        if (desc == null) {
            return "";
        }
        for (String k : new String[] { "会议室", "走廊", "办公室", "家里", "卧室", "客厅", "街上", "车里", "天台", "门口" }) {
            if (desc.contains(k)) {
                return k;
            }
        }
        return "";
    }

    private static int indexOfAny(String t, String... keys) {
        int best = -1;
        for (String k : keys) {
            int i = t.indexOf(k);
            if (i >= 0 && (best < 0 || i < best)) {
                best = i;
            }
        }
        return best;
    }

    private static String trimTo(String t, int max) {
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }
}
