package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.bgssai.shortai.model.ShortShot;

class PromptComposerTest {

    @ParameterizedTest
    @ValueSource(strings = {"真人写实", "二维动画", "三维动画", "水墨动画", "黑白胶片"})
    void explicitShotStyleOverridesWorkRenderingAndSurvivesRegeneratingPreviz(String style) {
        ShortShot shot = shot();
        String marker = "【画面风格：" + style + "】";
        shot.setPrevizText("做什么：她推开门。【连贯：衣着不变】 " + marker);
        String image = PromptComposer.compose(shot, "MODERN_URBAN");
        assertTrue(image.startsWith(marker));
        assertEquals(image.indexOf(marker), image.lastIndexOf(marker));
        assertFalse(image.contains("现代都市写实"));
        assertFalse(image.contains("photorealistic short drama"));
        assertTrue(image.contains("固定不动"));
        assertTrue(PromptComposer.previzText(shot).endsWith(marker));
        String motion = PromptComposer.generationPrompt(shot, "VIDEO");
        assertTrue(motion.startsWith(marker));
        assertTrue(motion.contains("她推开门"));
        assertTrue(motion.contains("【连贯：衣着不变】"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) PromptComposer.check(shot, marker).get("element_list");
        assertTrue(elements.stream().anyMatch(el -> "STYLE".equals(el.get("element")) && Integer.valueOf(1).equals(el.get("present_flag"))));
    }

    @Test
    void unmarkedAndLegacyGenreNarrativeKeepTheExistingRendering() {
        ShortShot shot = shot();
        shot.setShotDesc("会议室里她推开门。【基调：悬疑暗黑 / 罪案反转】");
        String image = PromptComposer.compose(shot, "MODERN_URBAN");
        assertTrue(image.contains(shot.getShotDesc()));
        assertTrue(image.contains("现代都市写实"));
        assertTrue(image.contains("photorealistic short drama"));
        assertEquals(shot.getShotDesc(), PromptComposer.generationPrompt(shot, "VIDEO"));
    }

    @Test
    void reusingAnOldFramePromptDropsOnlyItsGeneratedWorkStyleAndRenderingDefaults() {
        ShortShot shot = shot();
        shot.setImagePrompt(PromptComposer.compose(shot, "MODERN_URBAN"));
        shot.setPrevizText("【画面风格：三维动画】");
        String frame = PromptComposer.generationPrompt(shot, "IMAGE");
        assertTrue(frame.startsWith("【画面风格：三维动画】"));
        assertFalse(frame.contains("现代都市写实"));
        assertFalse(frame.contains("photorealistic"));
        assertTrue(frame.contains(shot.getShotDesc()));
        assertTrue(frame.contains("no watermark"));
    }

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
        assertTrue(text.contains("在哪：会议室"));
        assertFalse(text.contains("未写清场景"));
    }

    @Test
    void narrativePlaceBeatsADifferentSceneCard() {
        ShortShot shot = new ShortShot();
        shot.setShotDesc("重生回到豪门别墅，她决定先摸清所有人的底牌");
        shot.setShotSize("MEDIUM");
        shot.setCameraMove("FIXED");
        String text = PromptComposer.previzText(shot, "宴会厅");
        assertTrue(text.contains("在哪：豪门别墅"));
        assertFalse(text.contains("未写清场景"));
        assertFalse(text.contains("在哪：宴会厅"));
    }

    @Test
    void boundSceneCardFillsPlaceWhenTheSentenceHasNone() {
        ShortShot shot = new ShortShot();
        shot.setShotDesc("她把文件推了过去");
        shot.setShotSize("MEDIUM");
        shot.setCameraMove("FIXED");
        String filled = PromptComposer.previzText(shot, "总裁办公室");
        assertTrue(filled.contains("在哪：总裁办公室"));
        String blank = PromptComposer.previzText(shot, "sc-dm-002-02");
        assertTrue(blank.contains("在哪：未写清场景"));
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
