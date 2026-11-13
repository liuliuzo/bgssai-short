package com.bgssai.shortai.production.support;

import java.util.List;

/** Angle instructions that keep the same person and the same clothes as the front-face photo. */
public final class LookViewPrompt {

    private static final List<String> STRIP = List.of(
            "正脸", "前面", "背面", "背影", "后背", "侧面", "侧身", "站姿", "坐姿", "行走", "走动");

    private LookViewPrompt() {
    }

    public static String compose(String appearance, String angle, boolean referenced) {
        String cleaned = strip(appearance);
        String shot = switch (angle == null ? "" : angle) {
            case "BACK" -> "画这个人的背面全身，背对镜头，能看清后脑、后背和衣服背面，不要回头，不要画成正面。";
            case "SIDE" -> "画这个人的侧面全身，身体转到九十度侧对镜头，能看清侧脸轮廓和衣服侧面，不要画成正面。";
            case "FRONT" -> "画这个人的前面全身，面向镜头站直，能看清整套衣服的正面。";
            default -> "画这个人的正脸定妆，面向镜头，五官清楚。";
        };
        StringBuilder text = new StringBuilder();
        if (!cleaned.isBlank()) {
            text.append(cleaned).append('。');
        }
        text.append(shot);
        if (referenced) {
            text.append("以参考图为唯一依据，必须是同一个人、同一张脸、同一发型、同一套衣服，上衣、裤子或裙子、鞋子和颜色都不要换，不要换成别人。");
        }
        return text.toString();
    }

    public static String caption(String name, String appearance, String angle) {
        String label = switch (angle == null ? "" : angle) {
            case "FRONT" -> "前面";
            case "BACK" -> "背面";
            case "SIDE" -> "侧面";
            default -> "正脸";
        };
        String title = name == null ? "" : name.trim();
        String cleaned = strip(appearance);
        String text = (title + " " + cleaned + " " + label).replaceAll("\\s+", " ").trim();
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    static String strip(String appearance) {
        if (appearance == null || appearance.isBlank()) {
            return "";
        }
        String text = appearance;
        for (String word : STRIP) {
            text = text.replace(word, " ");
        }
        return text.replace('-', ' ').replace('—', ' ').replaceAll("\\s+", " ").trim();
    }
}
