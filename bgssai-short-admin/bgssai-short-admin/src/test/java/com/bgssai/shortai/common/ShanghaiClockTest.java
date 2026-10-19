package com.bgssai.shortai.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

class ShanghaiClockTest {

    @Test
    void formatsEpochAsShanghaiWallClock() {
        assertNull(ShanghaiClock.format(null));
        assertEquals("2026-10-19 22:00:00", ShanghaiClock.format(Date.from(Instant.parse("2026-10-19T14:00:00Z"))));
    }
}
