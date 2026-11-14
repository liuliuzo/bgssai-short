package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;

class LookAngleTest {

    @Test
    void classifiesCanonicalLabelsAndLeavesPoseAsTheSameAngle() {
        assertEquals("FRONT_FACE", LookAngle.classify("齐刘海长马尾，白色短袖衬衫，深色短裤 白衬衫-正脸"));
        assertEquals("FRONT", LookAngle.classify("白色短袖衬衫，深色短裤 白衬衫-前面"));
        assertEquals("BACK", LookAngle.classify("白色短袖衬衫，深色短裤 白衬衫-背面"));
        assertEquals("SIDE", LookAngle.classify("白色短袖衬衫，深色短裤 白衬衫-侧面"));
        assertEquals("SIDE", LookAngle.classify("白色短袖衬衫，深色短裤 白衬衫-侧面-站姿"));
        assertTrue(LookAngle.poseExtra("白衬衫-侧面-站姿"));
        assertFalse(LookAngle.poseExtra("白衬衫-侧面"));
        assertNull(LookAngle.classify("白色短袖衬衫定妆"));
        assertNull(LookAngle.classify("侧面也能看，前面也要"));
    }

    @Test
    void blankAngleFollowsCaptionAndDefaultFaceDumpDoesNotHideAnotherAngle() {
        assertEquals("FRONT", LookAngle.resolve(null, "运动装-前面"));
        assertEquals("BACK", LookAngle.resolve("FRONT_FACE", "运动装-背面"));
        assertEquals("SIDE", LookAngle.resolve("SIDE", "白衬衫-侧面-站姿"));
        assertEquals("FRONT", LookAngle.resolve("FRONT", "白衬衫 前面"));
        assertEquals("FRONT_FACE", LookAngle.resolve("FRONT_FACE", "白衬衫-正脸"));
        assertEquals("SIDE", LookAngle.resolve("SIDE", "请放在前面这一格"));
        assertNull(LookAngle.resolve(null, "白色短袖衬衫定妆"));
        assertThrows(BizException.class, () -> LookAngle.resolve("TOP", "白衬衫"));
    }

    @Test
    void planSupportsAllFourAnglesIncludingFrontFaceRedo() {
        assertEquals(java.util.List.of("FRONT"), LookAngle.plan(Set.of("FRONT_FACE", "BACK", "SIDE"), null, false));
        assertEquals(java.util.List.of("BACK"), LookAngle.plan(Set.of("FRONT_FACE", "FRONT", "SIDE"), null, false));
        assertEquals(java.util.List.of("FRONT_FACE"), LookAngle.plan(Set.of("FRONT", "BACK", "SIDE"), null, false));
        assertEquals(java.util.List.of("SIDE"), LookAngle.plan(Set.of("SIDE"), "SIDE", true));
        assertEquals(java.util.List.of("FRONT_FACE"), LookAngle.plan(Set.of("FRONT_FACE"), "FRONT_FACE", true));
        assertEquals(java.util.List.of("FRONT_FACE"), LookAngle.plan(Set.of(), "FRONT_FACE", false));
        assertThrows(BizException.class, () -> LookAngle.plan(Set.of("FRONT_FACE"), "FRONT_FACE", false));
        assertThrows(BizException.class, () -> LookAngle.plan(Set.of("FRONT_FACE", "FRONT", "BACK", "SIDE"), null, false));
        assertThrows(BizException.class, () -> LookAngle.plan(Set.of("FRONT"), "FRONT", false));
    }
}
