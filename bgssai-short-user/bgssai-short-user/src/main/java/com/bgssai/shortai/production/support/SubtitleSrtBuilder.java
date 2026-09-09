package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortSubtitle;

/**
 * Builds a minimal SRT from per-shot dialogue (or subtitle rows) for ffmpeg burn-in.
 */
public final class SubtitleSrtBuilder {

    private SubtitleSrtBuilder() {
    }

    public static String fromCues(List<Map<String, Object>> cues) {
        if (cues == null || cues.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (Map<String, Object> cue : cues) {
            String text = ShortJson.text(cue.get("subtitle_text"));
            if (text.isBlank()) {
                continue;
            }
            int startMs = ShortJson.integer(cue.get("start_ms"), 0);
            int endMs = ShortJson.integer(cue.get("end_ms"), startMs + 1000);
            if (endMs <= startMs) {
                endMs = startMs + 1000;
            }
            sb.append(index++).append('\n');
            sb.append(formatTs(startMs)).append(" --> ").append(formatTs(endMs)).append('\n');
            sb.append(escapeText(text)).append("\n\n");
        }
        return sb.toString();
    }

    /** Prefer persisted subtitle rows; otherwise fall back to shot dialogue. */
    public static List<Map<String, Object>> cuesForEpisode(List<ShortSubtitle> subs, List<ShortShot> shots) {
        if (subs != null && !subs.isEmpty()) {
            List<Map<String, Object>> cues = new ArrayList<>();
            for (ShortSubtitle sub : subs) {
                if (sub == null || sub.getSubtitleText() == null || sub.getSubtitleText().isBlank()) {
                    continue;
                }
                Map<String, Object> cue = new LinkedHashMap<>();
                cue.put("start_ms", sub.getStartMs() == null ? 0 : sub.getStartMs());
                cue.put("end_ms", sub.getEndMs() == null ? 0 : sub.getEndMs());
                cue.put("subtitle_text", sub.getSubtitleText().trim());
                cue.put("shot_id", sub.getShotId());
                cues.add(cue);
            }
            if (!cues.isEmpty()) {
                return cues;
            }
        }
        return cuesFromShots(shots);
    }

    public static List<Map<String, Object>> cuesFromShots(List<ShortShot> shots) {
        List<Map<String, Object>> cues = new ArrayList<>();
        int tMs = 0;
        if (shots == null) {
            return cues;
        }
        for (ShortShot shot : shots) {
            int durSec = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
            if (durSec <= 0) {
                durSec = 3;
            }
            String dialogue = shot.getDialogueText();
            if (dialogue != null && !dialogue.isBlank()) {
                Map<String, Object> cue = new LinkedHashMap<>();
                cue.put("start_ms", tMs);
                cue.put("end_ms", tMs + durSec * 1000);
                cue.put("subtitle_text", dialogue.trim());
                cue.put("shot_id", shot.getShotId());
                cues.add(cue);
            }
            tMs += durSec * 1000;
        }
        return cues;
    }

    public static String formatTs(int ms) {
        int safe = Math.max(0, ms);
        int h = safe / 3_600_000;
        int m = (safe % 3_600_000) / 60_000;
        int s = (safe % 60_000) / 1000;
        int frac = safe % 1000;
        return String.format("%02d:%02d:%02d,%03d", h, m, s, frac);
    }

    private static String escapeText(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }
}
