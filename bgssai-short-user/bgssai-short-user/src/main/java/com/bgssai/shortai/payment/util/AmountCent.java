package com.bgssai.shortai.payment.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountCent {

    private AmountCent() {
        throw new IllegalStateException("Utility class");
    }

    public static String toYuan(int amountCent) {
        return BigDecimal.valueOf(amountCent, 2).toPlainString();
    }

    public static Integer yuanToCent(String yuan) {
        if (yuan == null || yuan.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(yuan).movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
