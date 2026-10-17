package com.bgssai.shortai.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TokenFormatTest {

    @Test
    void compactBoundaries() {
        assertEquals("0", TokenFormat.compact(0));
        assertEquals("999", TokenFormat.compact(999));
        assertEquals("1K", TokenFormat.compact(1000));
        assertEquals("418.9M", TokenFormat.compact(418_912_345L));
        assertEquals("1.1B", TokenFormat.compact(1_100_000_000L));
    }
}
