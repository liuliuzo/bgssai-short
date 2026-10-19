package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.model.ShortShot;

class PromptComposerTest {

    @Test
    void previzTextHasFiveElementsWithoutPromptJargon() {
        ShortShot shot = shot();
        String text = PromptComposer.previzText(shot);
        assertTrue(text.contains("谁："));
        assertTrue(text.contains("在哪："));
        assertTrue(text.contains("做什么："));
        assertTrue(text.contains("怎么拍："));
        assertTrue(text.contains("什么感觉："));
        assertFalse(text.contains("cfg"));
        assertFalse(text.contains("9:16"));
    }

    @Test
    void lookPlainIsAudienceLanguage() {
        String plain = PromptComposer.lookPlain(shot());
        assertTrue(plain.startsWith("观众会看到"));
        assertTrue(plain.contains("中景"));
        assertTrue(plain.contains("收购案"));
    }

    @Test
    void segmentsNameTheSourcesOfTheImagePrompt() {
        List<Map<String, String>> segments = PromptComposer.segments(shot());
        assertFalse(segments.isEmpty());
        assertEquals("画面", segments.get(0).get("label"));
        assertEquals("DESC", segments.get(0).get("source"));
        assertTrue(segments.stream().anyMatch(item -> "对白".equals(item.get("label"))));
    }

    @Test
    void checkScoresEmptyPromptLow() {
        Map<String, Object> empty = PromptComposer.check(shot(), "");
        assertTrue((Integer) empty.get("prompt_score") < 70);
        assertEquals(0, empty.get("pass_flag"));
        Map<String, Object> ok = PromptComposer.check(shot(), PromptComposer.compose(shot(), "CINEMATIC"));
        assertTrue((Integer) ok.get("prompt_score") >= 50);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) ok.get("element_list");
        assertEquals(8, elements.size());
    }

    private static ShortShot shot() {
        ShortShot shot = new ShortShot();
        shot.setShotDesc("会议室里，沈知微把收购案推过桌面");
        shot.setDialogueText("这份收购案，谁批准的？");
        shot.setShotSize("MEDIUM");
        shot.setCameraMove("FIXED");
        shot.setEmotion("COLD");
        shot.setLighting("COLD_BLUE");
        return shot;
    }
}
