package com.bgssai.shortai.usage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

class UsageCostMathTest {

    @Test
    void billedCostCountsOnlySuccessfulPositiveAmounts() {
        assertEquals(50, UsageCostMath.billedCent("SUCCEEDED", 50));
        assertEquals(0, UsageCostMath.billedCent("FAILED", 50));
        assertEquals(0, UsageCostMath.billedCent("SUCCEEDED", 0));
        assertEquals(0, UsageCostMath.billedCent("SUCCEEDED", null));
    }

    @Test
    void reapArtifactsAreExcludedFromAverageDuration() {
        Date start = Date.from(Instant.parse("2026-08-18T02:00:00Z"));
        Date soon = new Date(start.getTime() + 4_700L);
        Date reaped = new Date(start.getTime() + 2_973_892_000L);
        assertEquals(4L, UsageCostMath.generationSeconds(soon, new Date(soon.getTime() + 1000L)) == null
                ? -1
                : UsageCostMath.generationSeconds(start, soon));
        assertNull(UsageCostMath.generationSeconds(start, reaped));
        assertNull(UsageCostMath.generationSeconds(null, soon));
    }

    @Test
    void dayBucketUsesShanghaiCalendar() {
        Date utcEvening = Date.from(Instant.parse("2026-10-19T16:30:00Z"));
        assertEquals("2026-10-20", UsageCostMath.shanghaiDay(utcEvening));
    }
}
