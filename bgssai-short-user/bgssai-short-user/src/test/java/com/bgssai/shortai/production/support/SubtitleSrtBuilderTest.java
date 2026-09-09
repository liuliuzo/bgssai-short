package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.model.ShortShot;

class SubtitleSrtBuilderTest {

    @Test
    void buildsSrtFromShotDialogue() {
        ShortShot s1 = new ShortShot();
        s1.setShotId("s-1");
        s1.setShotNo(1);
        s1.setShotDurationSec(3);
        s1.setDialogueText("你好");
        ShortShot s2 = new ShortShot();
        s2.setShotId("s-2");
        s2.setShotNo(2);
        s2.setShotDurationSec(2);
        s2.setDialogueText("退婚书我签");
        String srt = SubtitleSrtBuilder.fromCues(SubtitleSrtBuilder.cuesFromShots(List.of(s1, s2)));
        assertTrue(srt.contains("00:00:00,000 --> 00:00:03,000"));
        assertTrue(srt.contains("你好"));
        assertTrue(srt.contains("00:00:03,000 --> 00:00:05,000"));
        assertTrue(srt.contains("退婚书我签"));
    }

    @Test
    void formatTsPadsFields() {
        assertEquals("00:00:01,500", SubtitleSrtBuilder.formatTs(1500));
        assertEquals("01:01:01,001", SubtitleSrtBuilder.formatTs(3_661_001));
    }
}
