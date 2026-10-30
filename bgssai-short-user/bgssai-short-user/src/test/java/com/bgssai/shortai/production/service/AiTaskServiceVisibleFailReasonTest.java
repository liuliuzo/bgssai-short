package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AiTaskServiceVisibleFailReasonTest {

    @Test
    void successOrEmptyCodeDoesNotInventAFailureSentence() {
        assertNull(AiTaskService.visibleFailReason("SUCCEEDED", null, null));
        assertNull(AiTaskService.visibleFailReason(
                "success", null, "模型暂时不可用，请稍后重试或更换模型"));
        assertNull(AiTaskService.visibleFailReason(
                "SUCCEEDED", "MODEL_TIMEOUT", "模型超时，已自动重试 2 次"));
        assertNull(AiTaskService.visibleFailReason(
                "FAILED", null, "模型暂时不可用，请稍后重试或更换模型"));
        assertNull(AiTaskService.visibleFailReason("FAILED", "  ", "模型超时"));
        assertNull(AiTaskService.visibleFailReason("RUNNING", null, null));
    }

    @Test
    void codedFailureStillShowsScrubbedReason() {
        assertEquals("内容命中拦截词",
                AiTaskService.visibleFailReason("FAILED", "CONTENT_BLOCKED", "内容命中拦截词"));
        String scrubbed = AiTaskService.visibleFailReason(
                "FAILED", "MODEL_TIMEOUT", "tokenhub gateway down");
        assertFalse(scrubbed.toLowerCase().contains("tokenhub"));
        assertEquals("模型暂时不可用，请稍后重试或更换模型",
                AiTaskService.visibleFailReason("CANCELED", "MODEL_TIMEOUT", null));
    }
}
