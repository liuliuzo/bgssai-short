package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DubbingAlignNameTest {

    @Test
    void pendingShotWithDialogueAndNoAudioIsNotGenerating() {
        assertEquals("未配音", DubbingService.alignName("PENDING", false));
        assertEquals("未配音", DubbingService.alignName(null, false));
        assertEquals("未配音", DubbingService.alignName("GENERATING", false));
        assertEquals("已对齐", DubbingService.alignName("ALIGNED", true));
        assertEquals("未对齐", DubbingService.alignName("MISALIGNED", true));
        assertEquals("生成中", DubbingService.alignName("GENERATING", true));
        assertEquals("待对齐", DubbingService.alignName("PENDING", true));
    }
}
