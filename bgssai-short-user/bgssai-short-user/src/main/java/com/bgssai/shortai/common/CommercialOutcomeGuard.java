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
        if (isFakeUrl(url)) {
            String trimmed = url.trim();
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    label + " is stub/placeholder (" + schemeOf(trimmed)
                            + "); refusing fake commercial success");
        }
    }

    /**
     * Soft check for read paths: never surface a placeholder as a playable commercial URL.
     */
    public static boolean isFakeUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("stub:") || lower.startsWith("mock:") || lower.startsWith("fake:")
                || lower.startsWith("placeholder:") || lower.startsWith("about:")
                || lower.startsWith("null:") || lower.startsWith("test:")) {
            return true;
        }
        return lower.contains("://example.") || lower.contains("://localhost/placeholder")
                || lower.contains("/placeholder/") || lower.endsWith("/placeholder")
                || "placeholder".equals(lower) || "todo".equals(lower);
    }

    /** Returns null when blank or fake so callers cannot mark PASS on placeholders. */
    public static String safeCommercialUrl(String url) {
        return isFakeUrl(url) ? null : url.trim();
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
     * Normalizes Tube handoff status into READY / PENDING / FAILED / UNKNOWN.
     * Blank stays READY only for the historical success body that omits status.
     * Any unrecognized status is UNKNOWN: not commercial success and not a definite failure.
     */
    public static String classifyMediaStatus(String status) {
        if (status == null || status.isBlank()) {
            return "READY";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("UNKNOWN".equals(normalized) || "INDETERMINATE".equals(normalized) || "NOT_FOUND".equals(normalized)) {
            return "UNKNOWN".equals(normalized) || "INDETERMINATE".equals(normalized) ? "UNKNOWN" : "NOT_FOUND";
        }
        if (READY.contains(normalized)) {
            return "READY";
        }
        if (PENDING.contains(normalized)) {
            return "PENDING";
        }
        if (FAILED.contains(normalized)) {
            return "FAILED";
        }
        return "UNKNOWN";
    }

    private static String schemeOf(String url) {
        int idx = url.indexOf(':');
        return idx <= 0 ? url : url.substring(0, idx + 1);
    }
}
