package com.bgssai.shortai.production.support;

import java.util.Locale;

/**
 * Normalizes stored work aspect ratio enum codes (such as VERTICAL_9_16)
 * into canonical compose/publish ratio strings (9:16, 16:9, 1:1, 3:4).
 */
public final class AspectRatios {

    private AspectRatios() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "9:16";
        }
        String trimmed = raw.trim();
        return switch (trimmed.toUpperCase(Locale.ROOT)) {
            case "9:16", "VERTICAL_9_16", "PORTRAIT_9_16" -> "9:16";
            case "16:9", "HORIZONTAL_16_9", "LANDSCAPE_16_9" -> "16:9";
            case "1:1", "SQUARE_1_1" -> "1:1";
            case "3:4", "VERTICAL_3_4", "PORTRAIT_3_4" -> "3:4";
            default -> trimmed;
        };
    }
}
