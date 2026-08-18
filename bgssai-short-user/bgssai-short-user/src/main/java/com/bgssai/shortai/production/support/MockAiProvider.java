package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MockAiProvider {

    private MockAiProvider() {
    }

    public static List<Map<String, Object>> outline(String title, String pitch, int episodeCount, String genre) {
        List<Map<String, Object>> list = new ArrayList<>();
        String genreName = ShortLabels.genreName(genre);
        for (int i = 1; i <= episodeCount; i++) {
            Map<String, Object> ep = new LinkedHashMap<>();
            ep.put("episode_no", i);
            if (i == 1) {
                ep.put("episode_title", "开场");
                ep.put("episode_synopsis", pitch + "。第 1 集当众爆发冲突。");
                ep.put("ending_hook", "她抬眼：这事没完。");
            } else if (i == episodeCount) {
                ep.put("episode_title", "终局");
                ep.put("episode_synopsis", title + "迎来" + genreName + "收束，旧账当场清算。");
                ep.put("ending_hook", "镜头定格在她转身离开的背影。");
            } else {
                ep.put("episode_title", "第" + i + "集");
                ep.put("episode_synopsis", "第 " + i + " 集推进：" + pitch + " 的下一手。");
                ep.put("ending_hook", "反派冷笑：你以为这就赢了？");
            }
            list.add(ep);
        }
        return list;
    }

    public static List<Map<String, Object>> script(String episodeTitle, String synopsis, String extraPrompt) {
        String extra = extraPrompt == null || extraPrompt.isBlank() ? "" : extraPrompt.trim();
        List<Map<String, Object>> scenes = new ArrayList<>();
        scenes.add(scene(1, "冲突现场", synopsis,
                line(1, "女主", "退婚书我签，但有个条件。", "COLD", 3),
                line(2, "男主", "你凭什么跟我谈条件？", "ANGRY", 3)));
        scenes.add(scene(2, "三年后", "女主带着资源回归。",
                line(1, "女主", extra.isEmpty() ? "董事会，现在换我坐。" : extra, "CALM", 3),
                line(2, "配角", "她怎么回来了？", "SHOCK", 2)));
        scenes.add(scene(3, "对峙", episodeTitle + "的集末钩子。",
                line(1, "女主", "下一次，轮到你们求我。", "COLD", 3),
                line(2, "男主", "等着瞧。", "TENSE", 2)));
        return scenes;
    }

    public static String rewriteLine(String original, String instruction) {
        String base = original == null ? "" : original;
        if (instruction == null || instruction.isBlank()) {
            return base + "（更口语）";
        }
        return base + "（" + instruction.trim() + "）";
    }

    public static String svg(String title, String subtitle, String color) {
        String t = escape(title);
        String s = escape(subtitle);
        String c = color == null ? "#2563eb" : color;
        return "<svg xmlns='http://www.w3.org/2000/svg' width='1080' height='1920' viewBox='0 0 1080 1920'>"
                + "<rect width='1080' height='1920' fill='" + c + "'/>"
                + "<rect x='80' y='700' width='920' height='420' rx='24' fill='#0f172a' opacity='0.55'/>"
                + "<text x='540' y='860' text-anchor='middle' fill='#fff' font-size='64' font-family='sans-serif'>" + t
                + "</text>"
                + "<text x='540' y='980' text-anchor='middle' fill='#e2e8f0' font-size='36' font-family='sans-serif'>" + s
                + "</text>"
                + "<text x='540' y='1780' text-anchor='middle' fill='#fff' font-size='28' font-family='sans-serif'>MockAiProvider 9:16</text>"
                + "</svg>";
    }

    private static Map<String, Object> scene(int no, String location, String summary, Map<String, Object>... lines) {
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("scene_no", no);
        scene.put("scene_location", location);
        scene.put("scene_summary", summary);
        List<Map<String, Object>> lineList = new ArrayList<>();
        for (Map<String, Object> line : lines) {
            lineList.add(line);
        }
        scene.put("line_list", lineList);
        return scene;
    }

    private static Map<String, Object> line(int no, String name, String text, String emotion, int sec) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("line_no", no);
        line.put("character_name", name);
        line.put("line_text", text);
        line.put("emotion", emotion);
        line.put("estimated_sec", sec);
        return line;
    }

    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
