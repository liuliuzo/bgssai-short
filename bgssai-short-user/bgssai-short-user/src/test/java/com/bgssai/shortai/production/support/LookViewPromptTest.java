package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LookViewPromptTest {

    @Test
    void sideAndBackKeepTheFaceClothesAndDoNotRepeatTheOldAngle() {
        String appearance = "齐刘海长马尾，白色短袖衬衫，深色短裤，黑色高跟鞋 白衬衫-正脸";
        String back = LookViewPrompt.compose(appearance, "BACK", true);
        assertTrue(back.contains("白色短袖衬衫"));
        assertTrue(back.contains("深色短裤"));
        assertTrue(back.contains("背面全身"));
        assertTrue(back.contains("不要换成别人"));
        assertFalse(back.contains("正脸定妆"));
        assertFalse(back.contains("侧面"));

        String side = LookViewPrompt.compose("白衬衫-侧面-站姿", "SIDE", true);
        assertTrue(side.contains("侧面全身"));
        assertTrue(side.contains("同一套衣服"));
        assertFalse(side.contains("站姿"));

        String caption = LookViewPrompt.caption("白衬衫", appearance, "FRONT");
        assertTrue(caption.contains("白衬衫"));
        assertTrue(caption.endsWith("前面"));
        assertFalse(caption.contains("正脸"));
    }
}
