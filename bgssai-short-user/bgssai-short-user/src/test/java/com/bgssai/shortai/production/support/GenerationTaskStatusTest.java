package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class GenerationTaskStatusTest {

    @Test
    void namesAndFlags() {
        assertEquals("排队中", GenerationTaskStatus.statusName("QUEUED"));
        assertEquals("视频", GenerationTaskStatus.typeName("VIDEO"));
        assertTrue(GenerationTaskStatus.recoverable("FAILED"));
        assertTrue(GenerationTaskStatus.recoverable("CANCELED"));
        assertFalse(GenerationTaskStatus.recoverable("RUNNING"));
        assertTrue(GenerationTaskStatus.cancellable("QUEUED"));
        assertFalse(GenerationTaskStatus.cancellable("SUCCEEDED"));
        Map<String, Object> flags = GenerationTaskStatus.flags("FAILED", "IMAGE");
        assertEquals("已失败", flags.get("task_status_name"));
        assertEquals(1, flags.get("recoverable_flag"));
        assertEquals(0, flags.get("cancellable_flag"));
    }
}
