package com.bgssai.shortai.usage.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Billed cost and duration rules for the admin usage summary.
 * Failed tasks are not billed. Reaped tasks whose wall clock spans hours are not
 * generation time and are left out of the average.
 */
public final class UsageCostMath {

    public static final int MAX_DURATION_SEC = 3600;

    public static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(SHANGHAI);

    private UsageCostMath() {
    }

    public static int billedCent(String taskStatus, Integer costCent) {
        if (!"SUCCEEDED".equals(taskStatus) || costCent == null || costCent <= 0) {
            return 0;
        }
        return costCent;
    }

    /** Seconds of a finished generation, or null when the sample must not enter the average. */
    public static Long generationSeconds(Date started, Date finished) {
        if (started == null || finished == null) {
            return null;
        }
        long seconds = Math.max(0L, (finished.getTime() - started.getTime()) / 1000L);
        if (seconds > MAX_DURATION_SEC) {
            return null;
        }
        return seconds;
    }

    public static String shanghaiDay(Date date) {
        if (date == null) {
            return "";
        }
        return DAY.format(date.toInstant());
    }
}
