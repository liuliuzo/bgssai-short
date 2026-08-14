package com.bgssai.shortai.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Offline unit test for JwtUtil (no Spring context, no database).
 * Lives in the same package so it can call the package-private configure() hook that
 * JwtConfig normally invokes at startup.
 */
class JwtUtilTest {

    @BeforeAll
    static void setup() {
        JwtUtil.configure("bgssai-short-unit-test-secret-key-at-least-32-bytes", 86400000L);
    }

    @Test
    void generateAndValidateRoundTrip() {
        String token = JwtUtil.generateToken("u-1", "张三", "01", null, null, "PLATFORM_ADMIN");
        assertTrue(JwtUtil.validateToken(token));
        assertEquals("u-1", JwtUtil.getUserIdFromToken(token));
        assertEquals("张三", JwtUtil.getRealNameFromToken(token));
        assertEquals("PLATFORM_ADMIN", JwtUtil.getRoleFromToken(token));
    }

    @Test
    void rejectsGarbageToken() {
        assertFalse(JwtUtil.validateToken("not-a-real-token"));
    }
}
