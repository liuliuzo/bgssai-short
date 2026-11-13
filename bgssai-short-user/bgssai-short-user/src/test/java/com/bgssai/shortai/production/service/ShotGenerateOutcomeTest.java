package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortShot;

class ShotGenerateOutcomeTest {

    @Test
    void failedVideoRegenRestoresReadyAndLeavesTheBoundClip() {
        ShortShotMapper shots = mock(ShortShotMapper.class);
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShotGenerateOutcome outcome = outcome(shots, tasks);
        when(tasks.countByExample(any())).thenReturn(0L);
        when(shots.selectOneByExample(any())).thenReturn(shot("VIDEO_GENERATING", "vid-1", "img-1"));

        outcome.restoreBoundMedia(videoTask());

        ArgumentCaptor<ShortShot> patch = ArgumentCaptor.forClass(ShortShot.class);
        verify(shots).restoreBoundMediaAfterTerminalTask(any(), any(), patch.capture());
        assertEquals("VIDEO_READY", patch.getValue().getShotStatus());
        assertNull(patch.getValue().getVideoAssetId());
        assertNull(patch.getValue().getFirstFrameAssetId());
    }

    @Test
    void seedVideoDoesNotCountAsFinished() {
        ShortShotMapper shots = mock(ShortShotMapper.class);
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        com.bgssai.shortai.mapper.ShortAssetMapper assets = mock(com.bgssai.shortai.mapper.ShortAssetMapper.class);
        ShotGenerateOutcome outcome = outcome(shots, tasks);
        org.springframework.test.util.ReflectionTestUtils.setField(outcome, "shortAssetMapper", assets);
        when(tasks.countByExample(any())).thenReturn(0L);
        when(shots.selectOneByExample(any())).thenReturn(shot("VIDEO_GENERATING", "as-dm-001-s01v01", "as-dm-001-s01i01"));
        com.bgssai.shortai.model.ShortAsset seed = new com.bgssai.shortai.model.ShortAsset();
        seed.setStorageKey("<svg xmlns='http://www.w3.org/2000/svg'>seed 9:16</svg>");
        when(assets.selectOneByExample(any())).thenReturn(seed);

        outcome.restoreBoundMedia(videoTask());

        verify(shots, never()).restoreBoundMediaAfterTerminalTask(any(), any(), any());
    }

    @Test
    void failedFirstVideoReturnsToTheFirstFrame() {
        ShortShotMapper shots = mock(ShortShotMapper.class);
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShotGenerateOutcome outcome = outcome(shots, tasks);
        when(tasks.countByExample(any())).thenReturn(0L);
        when(shots.selectOneByExample(any())).thenReturn(shot("VIDEO_GENERATING", null, "img-1"));

        outcome.restoreBoundMedia(videoTask());

        ArgumentCaptor<ShortShot> patch = ArgumentCaptor.forClass(ShortShot.class);
        verify(shots).restoreBoundMediaAfterTerminalTask(any(), any(), patch.capture());
        assertEquals("IMAGE_READY", patch.getValue().getShotStatus());
        assertNull(patch.getValue().getVideoAssetId());
        assertNull(patch.getValue().getFirstFrameAssetId());
    }

    @Test
    void failedVideoWithNothingBoundDoesNotInventReady() {
        ShortShotMapper shots = mock(ShortShotMapper.class);
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShotGenerateOutcome outcome = outcome(shots, tasks);
        when(tasks.countByExample(any())).thenReturn(0L);
        when(shots.selectOneByExample(any())).thenReturn(shot("VIDEO_GENERATING", null, null));

        outcome.restoreBoundMedia(videoTask());

        verify(shots, never()).restoreBoundMediaAfterTerminalTask(any(), any(), any());
    }

    @Test
    void alreadyReadyClipIsNotRewritten() {
        ShortShotMapper shots = mock(ShortShotMapper.class);
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShotGenerateOutcome outcome = outcome(shots, tasks);
        when(tasks.countByExample(any())).thenReturn(0L);
        when(shots.selectOneByExample(any())).thenReturn(shot("VIDEO_READY", "vid-1", "img-1"));

        outcome.restoreBoundMedia(videoTask());

        verify(shots, never()).restoreBoundMediaAfterTerminalTask(any(), any(), any());
    }

    @Test
    void activeVideoJobKeepsGenerating() {
        ShortShotMapper shots = mock(ShortShotMapper.class);
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShotGenerateOutcome outcome = outcome(shots, tasks);
        when(tasks.countByExample(any())).thenReturn(1L);

        outcome.restoreBoundMedia(videoTask());

        verify(shots, never()).selectOneByExample(any());
        verify(shots, never()).restoreBoundMediaAfterTerminalTask(any(), any(), any());
    }

    @Test
    void failedImageRegenKeepsAnExistingVideo() {
        ShortShotMapper shots = mock(ShortShotMapper.class);
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShotGenerateOutcome outcome = outcome(shots, tasks);
        when(tasks.countByExample(any())).thenReturn(0L);
        when(shots.selectOneByExample(any())).thenReturn(shot("IMAGE_GENERATING", "vid-1", "img-1"));

        ShortAiTask task = videoTask();
        task.setTaskType("IMAGE");
        when(tasks.selectByPrimaryKey(any())).thenReturn(task);
        outcome.restoreBoundMedia(task);

        ArgumentCaptor<ShortShot> patch = ArgumentCaptor.forClass(ShortShot.class);
        verify(shots).restoreBoundMediaAfterTerminalTask(any(), any(), patch.capture());
        assertEquals("VIDEO_READY", patch.getValue().getShotStatus());
        assertNull(patch.getValue().getVideoAssetId());
    }

    @Test
    void failedImageRegenRestoresFirstFrameWhenNoVideo() {
        ShortShotMapper shots = mock(ShortShotMapper.class);
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShotGenerateOutcome outcome = outcome(shots, tasks);
        when(tasks.countByExample(any())).thenReturn(0L);
        when(shots.selectOneByExample(any())).thenReturn(shot("IMAGE_GENERATING", "  ", "img-1"));

        ShortAiTask task = videoTask();
        task.setTaskType("IMAGE");
        when(tasks.selectByPrimaryKey(any())).thenReturn(task);
        outcome.restoreBoundMedia(task);

        ArgumentCaptor<ShortShot> patch = ArgumentCaptor.forClass(ShortShot.class);
        verify(shots).restoreBoundMediaAfterTerminalTask(any(), any(), patch.capture());
        assertEquals("IMAGE_READY", patch.getValue().getShotStatus());
        assertNull(patch.getValue().getFirstFrameAssetId());
    }

    @Test
    void nonShotTaskIsIgnored() {
        ShortShotMapper shots = mock(ShortShotMapper.class);
        ShotGenerateOutcome outcome = outcome(shots, mock(ShortAiTaskMapper.class));
        ShortAiTask task = videoTask();
        task.setTargetType("PORTRAIT");
        outcome.restoreBoundMedia(task);
        outcome.restoreBoundMedia(null);
        verify(shots, never()).restoreBoundMediaAfterTerminalTask(any(), any(), any());
    }

    private static ShotGenerateOutcome outcome(ShortShotMapper shots, ShortAiTaskMapper tasks) {
        ShotGenerateOutcome outcome = new ShotGenerateOutcome();
        ReflectionTestUtils.setField(outcome, "shortShotMapper", shots);
        ReflectionTestUtils.setField(outcome, "shortAiTaskMapper", tasks);
        when(tasks.selectByPrimaryKey(any())).thenReturn(videoTask());
        return outcome;
    }

    private static ShortAiTask videoTask() {
        ShortAiTask task = new ShortAiTask();
        task.setTaskId("t-1");
        task.setId(9L); task.setUserId("owner"); task.setWorkId("work"); task.setEpisodeId("episode"); task.setRetryCount(1);
        task.setTaskType("VIDEO");
        task.setTargetType("SHOT");
        task.setTargetId("sh-1");
        task.setTaskStatus("FAILED");
        return task;
    }

    private static ShortShot shot(String status, String videoId, String frameId) {
        ShortShot shot = new ShortShot();
        shot.setId(7L);
        shot.setShotId("sh-1");
        shot.setShotStatus(status);
        shot.setVideoAssetId(videoId);
        shot.setFirstFrameAssetId(frameId);
        return shot;
    }
}
