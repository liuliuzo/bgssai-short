package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImageConditionEndpointTest {

    @Test
    void blankAndUnapprovedRoutesAreRejected() {
        assertTrue(ImageConditionEndpoint.admit(null).isEmpty());
        assertTrue(ImageConditionEndpoint.admit("  ").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("http://127.0.0.1:8007").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("http://localhost:8007").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("http://[::1]:8007").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("http://124.70.197.39:8007").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("http://172.31.10.199:8007").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("https://chat.bgssai.com").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("https://www.bgssai-tokenhub.cn").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("https://user:secret@i2v.invalid").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("https://i2v.invalid/v1/videos").isEmpty());
        assertTrue(ImageConditionEndpoint.admit("https://i2v.invalid?x=1").isEmpty());
    }

    @Test
    void laterApprovedOriginCanBeAdmittedWithoutADefault() {
        assertEquals("https://i2v.invalid", ImageConditionEndpoint.admit("https://i2v.invalid").orElseThrow().value());
        assertEquals("http://approved.example:8443",
                ImageConditionEndpoint.admit("http://approved.example:8443/").orElseThrow().value());
    }
}
