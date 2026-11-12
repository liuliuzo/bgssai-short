package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;

class DramaAssistServiceTest {

    @Test
    void normalizeRoleRejectsUnknown() {
        assertThrows(BizException.class, () -> DramaAssistService.normalizeRole("PRODUCER"));
        assertEquals("SCRIPT", DramaAssistService.normalizeRole("script"));
    }

    @Test
    void parseStructuredReadsJsonAndDefaults() {
        String raw = "{\"advice_text\":\"加一句反转\",\"summary\":\"反转\",\"action_list\":[\"改末句\"],\"apply_target\":\"dialogue_text\",\"apply_text\":\"你输了\"}";
        Map<String, Object> parsed = DramaAssistService.parseStructured(raw, "SCRIPT");
        assertEquals("加一句反转", parsed.get("advice_text"));
        assertEquals("dialogue_text", parsed.get("apply_target"));
        assertEquals("你输了", parsed.get("apply_text"));
        assertEquals(List.of("改末句"), parsed.get("action_list"));
    }

    @Test
    void parseStructuredFallsBackToRawText() {
        Map<String, Object> parsed = DramaAssistService.parseStructured("纯文本建议", "EDITOR");
        assertEquals("纯文本建议", parsed.get("advice_text"));
        assertEquals("none", parsed.get("apply_target"));
        assertEquals("", parsed.get("apply_text"));
    }

    @Test
    void promptContainsRoleDutyAndJsonContract() {
        String prompt = DramaAssistService.buildPrompt("DIRECTOR", "检查跳轴", Map.of("work_title", "样例"));
        assertTrue(prompt.contains("分镜导演"));
        assertTrue(prompt.contains("apply_target"));
        assertTrue(prompt.contains("检查跳轴"));
        assertTrue(prompt.contains("样例"));
    }
}
