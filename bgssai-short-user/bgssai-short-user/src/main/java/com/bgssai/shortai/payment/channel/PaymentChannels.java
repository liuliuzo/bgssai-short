package com.bgssai.shortai.payment.channel;

import java.util.Locale;

public final class PaymentChannels {

    private PaymentChannels() {
        throw new IllegalStateException("Utility class");
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("WECHAT_PAY".equals(s) || "WX".equals(s)) {
            return "WECHAT";
        }
        if ("ALI".equals(s)) {
            return "ALIPAY";
        }
        return s;
    }
}
