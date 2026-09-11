package com.bgssai.shortai.common;

import java.util.Locale;
import java.util.Set;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/**
 * Commercial-readiness guards for SHORT-01.
 * <p>
 * Unconfigured / placeholder / stub outcomes must never be treated as READY success
 * (same class of failure as long's {@code stub://} fake path).
 */
public final class CommercialOutcomeGuard {

    private static final Set<String> READY = Set.of("READY", "SUCCEEDED", "SUCCESS");
    private static final Set<String> PENDING = Set.of("PENDING", "PROCESSING", "QUEUED", "ACCEPTED", "RUNNING");
    private static final Set<String> FAILED = Set.of("FAILED", "FAIL", "ERROR", "REJECTED", "CANCELLED", "CANCELED");

    private CommercialOutcomeGuard() {
    }

    /**
     * Rejects stub/mock/placeholder URLs so callers cannot persist SUCCEEDED with fake addresses.
     */
    public static void rejectFakeUrl(String url, String fieldLabel) {
        String label = fieldLabel == null || fieldLabel.isBlank() ? "url" : fieldLabel;
        if (url == null || url.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, label + " unavailable");
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("stub:") || lower.startsWith("mock:") || lower.startsWith("fake:")
                || lower.startsWith("placeholder:") || lower.startsWith("about:")
                || lower.startsWith("null:") || lower.startsWith("test:")) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    label + " is stub/placeholder (" + schemeOf(trimmed) + "); refusing fake commercial success");
        }
        if (lower.contains("://example.") || lower.contains("://localhost/placeholder")
                || lower.contains("/placeholder/") || lower.endsWith("/placeholder")
                || "placeholder".equals(lower) || "todo".equals(lower)) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    label + " looks like a placeholder; refusing fake commercial success");
        }
    }

    public static boolean isReadyStatus(String status) {
        return status != null && READY.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isPendingStatus(String status) {
        return status != null && PENDING.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isFailedStatus(String status) {
        return status != null && FAILED.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Normalizes Media handoff status into READY / PENDING / FAILED.
     * Unknown values fail closed (not commercial READY).
     */
    public static String classifyMediaStatus(String status) {
        if (status == null || status.isBlank()) {
            return "READY";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (READY.contains(normalized)) {
            return "READY";
        }
        if (PENDING.contains(normalized)) {
            return "PENDING";
        }
        if (FAILED.contains(normalized)) {
            return "FAILED";
        }
        return "FAILED";
    }

    private static String schemeOf(String url) {
        int idx = url.indexOf(':');
        return idx <= 0 ? url : url.substring(0, idx + 1);
    }
}
