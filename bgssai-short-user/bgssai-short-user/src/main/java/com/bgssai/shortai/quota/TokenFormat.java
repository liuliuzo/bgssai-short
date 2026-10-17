package com.bgssai.shortai.quota;

/**
 * Compact token display (REQ-SHORT-COMMON-019). Same rules as the React helper:
 * 0/999 plain, 1000→1K, M/B with at most one decimal. APIs keep precise integers.
 */
public final class TokenFormat {

    private TokenFormat() {
    }

    public static String compact(long value) {
        if (value == 0L) {
            return "0";
        }
        long abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs < 1_000L) {
            return sign + abs;
        }
        if (abs < 1_000_000L) {
            return sign + oneDecimal(abs / 1000.0) + "K";
        }
        if (abs < 1_000_000_000L) {
            return sign + oneDecimal(abs / 1_000_000.0) + "M";
        }
        return sign + oneDecimal(abs / 1_000_000_000.0) + "B";
    }

    private static String oneDecimal(double v) {
        double rounded = Math.round(v * 10.0) / 10.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-9) {
            return String.valueOf((long) Math.rint(rounded));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", rounded);
    }
}
