package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.model.ShortAiTask;

class OfficialVideoLaneTest {

    @Test
    void waitingOfficialClipsStayQueuedBehindTheOneThatIsRunning() {
        ShortAiTask image = task(1L, "IMAGE", "QUEUED", "bgssai-image");
        ShortAiTask first = task(2L, "VIDEO", "QUEUED", "bgssai-video");
        ShortAiTask second = task(3L, "VIDEO", "QUEUED", "bgssai-video");
        assertEquals(image.getId(), OfficialVideoLane.nextClaim(List.of(first, image, second), true).getId());
        assertEquals(first.getId(), OfficialVideoLane.nextClaim(List.of(first, second), false).getId());
        assertNull(OfficialVideoLane.nextClaim(List.of(first, second), true));
        ShortAiTask imageToVideo = task(4L, "VIDEO", "QUEUED", "bgssai-video-i2v");
        imageToVideo.setTargetType("IMAGE_TO_VIDEO");
        assertEquals(first.getId(), OfficialVideoLane.nextClaim(List.of(imageToVideo, first), false, true).getId());
        assertNull(OfficialVideoLane.nextClaim(List.of(imageToVideo), false, true));
        assertEquals(imageToVideo.getId(), OfficialVideoLane.nextClaim(List.of(imageToVideo, first), true, false).getId());
    }

    @Test
    void taskCenterCopyShowsQueueAndElapsedInsteadOfAFrozenThirty() {
        ShortAiTask running = task(8L, "VIDEO", "RUNNING", "bgssai-video");
        running.setStartedTime(new Date(0));
        Map<String, Object> runningView = new LinkedHashMap<>();
        OfficialVideoLane.enrich(runningView, running, 0, 6 * 60 * 1000L);
        assertEquals("执行中", GenerationTaskStatus.statusName("RUNNING"));
        assertTrue(runningView.get("progress_hint").toString().contains("已等待"));
        assertTrue(runningView.get("progress_hint").toString().contains("十多分钟"));
        assertNotEquals(30, runningView.get("progress_percent"));

        ShortAiTask queued = task(9L, "VIDEO", "QUEUED", "bgssai-video");
        Map<String, Object> queuedView = new LinkedHashMap<>();
        OfficialVideoLane.enrich(queuedView, queued, 2, 0);
        assertEquals("排队中", GenerationTaskStatus.statusName("QUEUED"));
        assertEquals(0, queuedView.get("progress_percent"));
        assertTrue(queuedView.get("progress_hint").toString().contains("前面还有 2 条"));
        assertEquals("前面还有 2 条", queuedView.get("status_note"));
    }

    @Test
    void seedAssetsAreNotFinishedMediaAndTimeoutCanResume() {
        assertTrue(OfficialVideoLane.isPlaceholderVideo("<svg xmlns='http://www.w3.org/2000/svg'>seed 9:16</svg>", null, null));
        assertTrue(OfficialVideoLane.isPlaceholderImage("<svg xmlns='http://www.w3.org/2000/svg'></svg>", null, null));
        assertFalse(OfficialVideoLane.isPlaceholderVideo("short/dev/u/w/clip.mp4", "video/mp4", 252705));
        assertFalse(OfficialVideoLane.isPlaceholderImage("short/dev/u/w/look.jpg", "image/jpeg", 20000));
        byte[] mp4 = new byte[] {0, 0, 0, 20, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        assertTrue(OfficialVideoLane.isRealMp4(mp4));
        assertFalse(OfficialVideoLane.isRealMp4("<svg".getBytes()));
        assertTrue(OfficialVideoLane.deferrable("504000", "生成超时，任务可能仍在处理"));
        assertTrue(OfficialVideoLane.deferrable(null, "model call interrupted"));
        assertFalse(OfficialVideoLane.deferrable("4006", "内容命中拦截词"));
    }

    private static ShortAiTask task(long id, String type, String status, String model) {
        ShortAiTask task = new ShortAiTask();
        task.setId(id);
        task.setTaskType(type);
        task.setTaskStatus(status);
        task.setModelCode(model);
        return task;
    }
}
