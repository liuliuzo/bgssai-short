package com.bgssai.shortai.production.support;

import java.util.UUID;

public final class ShortIds {

    private ShortIds() {
    }

    public static String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
