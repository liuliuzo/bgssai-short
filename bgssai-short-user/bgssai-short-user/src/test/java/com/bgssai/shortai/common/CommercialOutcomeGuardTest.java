package com.bgssai.shortai.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

class CommercialOutcomeGuardTest {

    @Test
    void stubSchemeIsNeverCommercialSuccess() {
        BizException ex = assertThrows(BizException.class,
                () -> CommercialOutcomeGuard.rejectFakeUrl("stub://film/demo.mp4", "film video_url"));
        assertEquals(CommonErrorCode.RULE_REJECT.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("stub") || ex.getMessage().contains("placeholder"));
    }

    @Test
    void mockAndPlaceholderSchemesRejected() {
        assertThrows(BizException.class,
                () -> CommercialOutcomeGuard.rejectFakeUrl("mock://asset", "url"));
        assertThrows(BizException.class,
                () -> CommercialOutcomeGuard.rejectFakeUrl("placeholder://x", "url"));
        assertThrows(BizException.class,
                () -> CommercialOutcomeGuard.rejectFakeUrl("https://example.com/a.mp4", "url"));
    }

    @Test
    void realHttpsAllowed() {
        CommercialOutcomeGuard.rejectFakeUrl("https://obs.example-cdn.com/films/a.mp4", "film video_url");
    }

    @Test
    void blankUrlRejected() {
        assertThrows(BizException.class, () -> CommercialOutcomeGuard.rejectFakeUrl("  ", "play_url"));
    }

    @Test
    void classifyReadyPendingFailedDistinct() {
        assertEquals("READY", CommercialOutcomeGuard.classifyMediaStatus("READY"));
        assertEquals("READY", CommercialOutcomeGuard.classifyMediaStatus("SUCCEEDED"));
        assertEquals("PENDING", CommercialOutcomeGuard.classifyMediaStatus("PENDING"));
        assertEquals("PENDING", CommercialOutcomeGuard.classifyMediaStatus("PROCESSING"));
        assertEquals("FAILED", CommercialOutcomeGuard.classifyMediaStatus("FAILED"));
        assertEquals("UNKNOWN", CommercialOutcomeGuard.classifyMediaStatus("UNKNOWN_X"));
        assertEquals("UNKNOWN", CommercialOutcomeGuard.classifyMediaStatus("UNKNOWN"));
        assertEquals("NOT_FOUND", CommercialOutcomeGuard.classifyMediaStatus("NOT_FOUND"));
        assertTrue(CommercialOutcomeGuard.isReadyStatus("READY"));
        assertTrue(CommercialOutcomeGuard.isPendingStatus("PENDING"));
        assertTrue(CommercialOutcomeGuard.isFailedStatus("FAILED"));
        assertFalse(CommercialOutcomeGuard.isReadyStatus("PENDING"));
        assertFalse(CommercialOutcomeGuard.isPendingStatus("FAILED"));
    }

    @Test
    void isFakeUrlAndSafeCommercialUrl() {
        assertTrue(CommercialOutcomeGuard.isFakeUrl(null));
        assertTrue(CommercialOutcomeGuard.isFakeUrl("stub://x"));
        assertFalse(CommercialOutcomeGuard.isFakeUrl("https://obs.my-cdn.com/a.mp4"));
        assertNull(CommercialOutcomeGuard.safeCommercialUrl("mock://x"));
        assertEquals("https://obs.my-cdn.com/a.mp4",
                CommercialOutcomeGuard.safeCommercialUrl("https://obs.my-cdn.com/a.mp4"));
    }
}
