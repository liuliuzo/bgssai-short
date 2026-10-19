package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RhythmCheckerTest {

    @Test
    void hookAndEndingBoostScore() {
        List<Map<String, Object>> scenes = new ArrayList<>();
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("scene_no", 1);
        List<Map<String, Object>> lines = new ArrayList<>();
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("line_text", "退婚书我签，但有个条件。");
        first.put("estimated_sec", 3);
        Map<String, Object> last = new LinkedHashMap<>();
        last.put("line_text", "下集等着瞧？");
        last.put("estimated_sec", 2);
        lines.add(first);
        lines.add(last);
        scene.put("line_list", lines);
        scenes.add(scene);
        Map<String, Object> rhythm = RhythmChecker.check(scenes, 90);
        assertEquals(82, rhythm.get("hook_score"));
        assertEquals(80, rhythm.get("ending_score"));
        assertTrue(((Integer) rhythm.get("total_score")) >= 60);

        List<Map<String, Object>> markers = RhythmChecker.extractRhythmMarkers(scenes);
        assertEquals(2, markers.size());
        assertEquals("HOOK_3S", markers.get(0).get("marker_type"));
        assertEquals("CLIFFHANGER", markers.get(1).get("marker_type"));

        Map<String, Object> validSafeZone = RhythmChecker.validateAspectRatioAndSafeZone("9:16", 15);
        assertEquals(Boolean.TRUE, validSafeZone.get("valid"));
        Map<String, Object> invalidSafeZone = RhythmChecker.validateAspectRatioAndSafeZone("9:16", 3);
        assertEquals(Boolean.FALSE, invalidSafeZone.get("valid"));
    }
}
