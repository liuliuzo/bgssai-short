package com.bgssai.shortai.production.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unified generation task state machine for TEXT / IMAGE / VIDEO (and VOICE / COMPOSE).
 */
public final class GenerationTaskStatus {

    public static final String QUEUED = "QUEUED";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";
    public static final String CANCELED = "CANCELED";
    public static final String PAUSED = "PAUSED";

    private static final Set<String> RECOVERABLE = Set.of(FAILED, CANCELED);
    private static final Set<String> CANCELLABLE = Set.of(QUEUED, RUNNING, PAUSED);
    private static final Set<String> TERMINAL = Set.of(SUCCEEDED, FAILED, CANCELED);

    private GenerationTaskStatus() {
    }

    public static String statusName(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case QUEUED -> "排队中";
            case RUNNING -> "执行中";
            case SUCCEEDED -> "已成功";
            case FAILED -> "已失败";
            case CANCELED -> "已取消";
            case PAUSED -> "已暂停";
            default -> status;
        };
    }

    public static String typeName(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "TEXT" -> "文本";
            case "IMAGE" -> "图片";
            case "VIDEO" -> "视频";
            case "VOICE" -> "配音";
            case "COMPOSE" -> "合成";
            default -> type;
        };
    }

    public static boolean recoverable(String status) {
        return status != null && RECOVERABLE.contains(status);
    }

    public static boolean cancellable(String status) {
        return status != null && CANCELLABLE.contains(status);
    }

    public static boolean terminal(String status) {
        return status != null && TERMINAL.contains(status);
    }

    public static Map<String, Object> flags(String status, String type) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("task_status_name", statusName(status));
        item.put("task_type_name", typeName(type));
        item.put("recoverable_flag", recoverable(status) ? 1 : 0);
        item.put("cancellable_flag", cancellable(status) ? 1 : 0);
        return item;
    }
}
