package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.production.service.FilmPublishToMediaService;

class AspectRatiosTest {

    @Test
    void normalizesWorkEnumCodesAndPreservesUnknownForValidation() {
        assertEquals("9:16", AspectRatios.normalize(null));
        assertEquals("9:16", AspectRatios.normalize("  "));
        assertEquals("9:16", AspectRatios.normalize("VERTICAL_9_16"));
        assertEquals("9:16", AspectRatios.normalize("9:16"));
        assertEquals("16:9", AspectRatios.normalize("HORIZONTAL_16_9"));
        assertEquals("1:1", AspectRatios.normalize("SQUARE_1_1"));
        assertEquals("3:4", AspectRatios.normalize("PORTRAIT_3_4"));
        assertEquals("21:9", AspectRatios.normalize("21:9"));

        assertTrue(FilmPublishToMediaService.isValidAspectRatio("VERTICAL_9_16"));
        assertFalse(FilmPublishToMediaService.isValidAspectRatio("21:9"));

        Map<String, Object> check = RhythmChecker.validateAspectRatioAndSafeZone("VERTICAL_9_16", 15);
        assertEquals("9:16", check.get("aspect_ratio"));
        assertEquals(Boolean.TRUE, check.get("valid"));
    }
}
