package com.bgssai.shortai.common;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/** Wall-clock strings in Asia/Shanghai, independent of the JVM default zone. */
public final class ShanghaiClock {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private ShanghaiClock() {
    }

    public static String format(Date date) {
        if (date == null) {
            return null;
        }
        return FMT.format(date.toInstant());
    }
}
