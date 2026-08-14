package com.bgssai.shortai.login.oauth;

import java.util.UUID;

public final class OAuthIds {
    private OAuthIds() {}

    public static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public static String next(String prefix) {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return prefix + raw.substring(0, 16);
    }
}
