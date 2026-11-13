package com.bgssai.shortai.production.support;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.bgssai.shortai.model.ShortAiTask;

/**
 * Company video ({@code bgssai-video}) shares one serial GPU.
 * Only the clip actually in that lane is 执行中. The rest stay 排队中.
 * A gateway timeout is not a finished failure while the original receipt is still open.
 * Seed SVG stand-ins are not playable clips.
 */
public final class OfficialVideoLane {

    public static final String MODEL = "bgssai-video";
    /** One status slice. The worker releases the thread and polls the same receipt again. */
    public static final int SLICE_SEC = 20;
    /** A single clip may outlive the gateway budget. Give up only after this long. */
    public static final int MAX_OPEN_SEC = 2 * 60 * 60;
    private static final int EXPECTED_SEC = 12 * 60;

    private OfficialVideoLane() {
    }

    public static boolean isOfficial(ShortAiTask task) {
        return task != null && "VIDEO".equals(task.getTaskType()) && MODEL.equals(task.getModelCode());
    }

    public static String resumeId(ShortAiTask task) {
        if (task == null) {
            return "";
        }
        return ShortJson.text(ShortJson.map(task.getRequestJson()).get("resume_request_id"));
    }

    public static boolean isImageToVideo(ShortAiTask task) {
        return task != null && "IMAGE_TO_VIDEO".equals(task.getTargetType());
    }

    /** Skip official clips while one is already in the lane, and keep later non-video work moving. */
    public static ShortAiTask nextClaim(List<ShortAiTask> queuedOldestFirst, boolean officialLaneBusy) {
        return nextClaim(queuedOldestFirst, officialLaneBusy, false);
    }

    /**
     * Image-to-video is CPU and does not take the text-to-video GPU lane.
     * A running image-to-video job only holds back the next image-to-video job.
     */
    public static ShortAiTask nextClaim(List<ShortAiTask> queuedOldestFirst, boolean officialLaneBusy,
            boolean imageToVideoBusy) {
        if (queuedOldestFirst == null) {
            return null;
        }
        for (ShortAiTask task : queuedOldestFirst) {
            if (task == null) {
                continue;
            }
            if (officialLaneBusy && isOfficial(task)) {
                continue;
            }
            if (imageToVideoBusy && isImageToVideo(task)) {
                continue;
            }
            return task;
        }
        return null;
    }

    public static boolean deferrable(String code, String message) {
        if ("504000".equals(code)) {
            return true;
        }
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return text.contains("timeout") || text.contains("timed out") || text.contains("生成超时")
                || text.contains("model call interrupted") || text.contains("任务可能仍在处理")
                || text.contains("interrupted");
    }

    /**
     * Demo seeds store an SVG document in {@code storage_key}. A real clip is an mp4 object.
     * Unknown keys that are neither SVG nor mp4 are not a finished video.
     */
    public static boolean isPlaceholderVideo(String storageKey, String mimeType, Integer fileSize) {
        if (storageKey == null || storageKey.isBlank()) {
            return true;
        }
        String key = storageKey.trim();
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.startsWith("<svg") || lower.startsWith("<?xml") || lower.contains("<svg")) {
            return true;
        }
        if (lower.contains("seed 9:16") || lower.contains("seed 16:9")) {
            return true;
        }
        String mime = mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
        if (mime.contains("svg") || mime.startsWith("image/")) {
            return true;
        }
        if (fileSize != null && fileSize <= 0) {
            return true;
        }
        return !"video/mp4".equals(mime) && !lower.endsWith(".mp4");
    }

    /** A demo SVG or a non-photo is not a 定妆 look or a first frame. */
    public static boolean isPlaceholderImage(String storageKey, String mimeType, Integer fileSize) {
        if (storageKey == null || storageKey.isBlank()) {
            return true;
        }
        String key = storageKey.trim();
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.startsWith("<svg") || lower.startsWith("<?xml") || lower.contains("<svg")
                || lower.contains("seed 9:16") || lower.contains("seed 16:9")) {
            return true;
        }
        String mime = mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
        if (mime.contains("svg") || mime.startsWith("video/")) {
            return true;
        }
        if (fileSize != null && fileSize <= 0) {
            return true;
        }
        return !(mime.contains("png") || mime.contains("jpeg") || mime.contains("jpg") || mime.contains("webp")
                || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp"));
    }

    public static boolean isRealMp4(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return false;
        }
        int limit = Math.min(bytes.length - 4, 32);
        for (int i = 0; i <= limit; i++) {
            if (bytes[i] == 'f' && bytes[i + 1] == 't' && bytes[i + 2] == 'y' && bytes[i + 3] == 'p') {
                return true;
            }
        }
        return false;
    }

    public static void enrich(Map<String, Object> item, ShortAiTask row, int ahead, long nowMs) {
        if (item == null || !isOfficial(row)) {
            return;
        }
        String status = row.getTaskStatus();
        long elapsed = elapsedSec(row, nowMs);
        item.put("elapsed_sec", elapsed);
        if ("QUEUED".equals(status) || "PAUSED".equals(status)) {
            int waiting = Math.max(0, ahead);
            item.put("queue_ahead", waiting);
            item.put("progress_percent", 0);
            item.put("status_note", queuedNote(waiting));
            item.put("progress_hint", queuedHint(waiting));
            return;
        }
        if ("RUNNING".equals(status)) {
            item.put("queue_ahead", null);
            item.put("progress_percent", runningPercent(elapsed));
            item.put("status_note", runningNote(elapsed));
            item.put("progress_hint", runningHint(elapsed));
        }
    }

    public static int runningPercent(long elapsedSec) {
        long elapsed = Math.max(0, elapsedSec);
        long raw = 8 + elapsed * 82 / EXPECTED_SEC;
        if (raw < 8) {
            raw = 8;
        }
        if (raw > 90) {
            raw = 90;
        }
        return (int) raw;
    }

    public static String queuedHint(int ahead) {
        return "排队中，" + queuedNote(ahead) + "。公司视频一次只生成一条，通常每条约十多分钟。";
    }

    public static String runningHint(long elapsedSec) {
        return "正在生成，已等待 " + elapsedText(elapsedSec) + "。公司视频通常每条约十多分钟，一次只生成一条。";
    }

    public static String queuedNote(int ahead) {
        return ahead <= 0 ? "马上开始" : "前面还有 " + ahead + " 条";
    }

    public static String runningNote(long elapsedSec) {
        return "已等待 " + elapsedText(elapsedSec);
    }

    static long elapsedSec(ShortAiTask row, long nowMs) {
        Date origin = "RUNNING".equals(row.getTaskStatus()) && row.getStartedTime() != null
                ? row.getStartedTime() : row.getQueuedTime();
        if (origin == null) {
            origin = row.getStartedTime() != null ? row.getStartedTime() : row.getCreatedTime();
        }
        if (origin == null) {
            return 0;
        }
        return Math.max(0, (nowMs - origin.getTime()) / 1000L);
    }

    private static String elapsedText(long elapsedSec) {
        if (elapsedSec < 60) {
            return "不到 1 分钟";
        }
        return Math.max(1, elapsedSec / 60) + " 分钟";
    }
}
