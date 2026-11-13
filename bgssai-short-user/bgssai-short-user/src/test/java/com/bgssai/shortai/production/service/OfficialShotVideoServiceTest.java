package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;
import com.bgssai.shortai.production.dto.ProductionDtos.AiTaskReq;
import com.bgssai.shortai.production.support.OfficialVideoCopy;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.quota.service.QuotaWalletService;

class OfficialShotVideoServiceTest {

    private OfficialShotVideoService service;
    private AiTaskService tasks;
    private AiTaskQueue queue;
    private ImageToVideoService imageToVideo;
    private ShortShotMapper shots;
    private ShortAssetMapper assets;
    private ShortAiTaskMapper aiTasks;
    private AiModelResolveService models;

    @BeforeEach
    void setUp() {
        service = new OfficialShotVideoService();
        tasks = org.mockito.Mockito.mock(AiTaskService.class);
        queue = org.mockito.Mockito.mock(AiTaskQueue.class);
        imageToVideo = org.mockito.Mockito.mock(ImageToVideoService.class);
        shots = org.mockito.Mockito.mock(ShortShotMapper.class);
        assets = org.mockito.Mockito.mock(ShortAssetMapper.class);
        aiTasks = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        models = org.mockito.Mockito.mock(AiModelResolveService.class);
        WorkAccessService access = org.mockito.Mockito.mock(WorkAccessService.class);
        QuotaWalletService wallet = org.mockito.Mockito.mock(QuotaWalletService.class);
        QuotaConsumeService consume = org.mockito.Mockito.mock(QuotaConsumeService.class);
        ReflectionTestUtils.setField(service, "aiTaskService", tasks);
        ReflectionTestUtils.setField(service, "aiTaskQueue", queue);
        ReflectionTestUtils.setField(service, "imageToVideoService", imageToVideo);
        ReflectionTestUtils.setField(service, "shortShotMapper", shots);
        ReflectionTestUtils.setField(service, "shortAssetMapper", assets);
        ReflectionTestUtils.setField(service, "shortCharacterMapper", org.mockito.Mockito.mock(ShortCharacterMapper.class));
        ReflectionTestUtils.setField(service, "shortSceneCardMapper", org.mockito.Mockito.mock(ShortSceneCardMapper.class));
        ReflectionTestUtils.setField(service, "shortAiTaskMapper", aiTasks);
        ReflectionTestUtils.setField(service, "aiModelResolveService", models);
        ReflectionTestUtils.setField(service, "quotaWalletService", wallet);
        ReflectionTestUtils.setField(service, "quotaConsumeService", consume);
        ReflectionTestUtils.setField(service, "paidOrderGrantService", org.mockito.Mockito.mock(PaidOrderGrantService.class));
        ReflectionTestUtils.setField(service, "workAccessService", access);
        when(access.requireUser()).thenReturn("u-1");
        UserQuota quota = new UserQuota();
        quota.setBalanceCent(5000);
        quota.setExtraBalanceCent(0);
        when(wallet.requireWallet("u-1")).thenReturn(quota);
        AiModelConfig video = new AiModelConfig();
        video.setModelCode("bgssai-video");
        video.setPriceUnit("PER_SECOND");
        video.setUnitPriceCent(20);
        when(models.resolve(eq("VIDEO"), eq("bgssai-video"))).thenReturn(video);
        when(models.costCent(any(), anyInt())).thenReturn(20);
    }

    @Test
    void realPhotoStartsImageToVideoAndKeepsTheOldClip() {
        ShortShot shot = readyShot();
        when(tasks.resolveShots(any())).thenReturn(List.of(shot));
        when(assets.selectOneByExample(any())).thenReturn(jpeg("frame-1"));
        when(imageToVideo.readyForOfficial()).thenReturn(true);
        when(aiTasks.selectByExample(any())).thenReturn(List.of());
        ShortAiTask queued = new ShortAiTask();
        queued.setTaskId("t-new");
        when(imageToVideo.enqueueForShot(any(), eq("frame-1"), anyString(), anyInt())).thenReturn(queued);

        AiTaskReq preview = req();
        @SuppressWarnings("unchecked")
        Map<String, Object> planned = (Map<String, Object>) service.preview(preview).getResult();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) planned.get("shot_list");
        assertEquals(OfficialVideoCopy.ROUTE_I2V, rows.get(0).get("route"));
        assertTrue(String.valueOf(rows.get(0).get("size_note")).contains("384×256"));
        assertTrue(String.valueOf(rows.get(0).get("size_note")).contains("不是竖屏"));
        verify(imageToVideo, never()).enqueueForShot(any(), any(), any(), anyInt());

        AiTaskReq missing = req();
        assertThrows(BizException.class, () -> service.submit(missing));
        verify(imageToVideo, never()).enqueueForShot(any(), any(), any(), anyInt());

        AiTaskReq submit = req();
        submit.setConfirmCost(1);
        service.submit(submit);
        verify(queue, never()).enqueue(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any());
        ArgumentCaptor<ShortShot> patch = ArgumentCaptor.forClass(ShortShot.class);
        verify(shots).updateByExampleSelective(patch.capture(), any());
        assertEquals("VIDEO_GENERATING", patch.getValue().getShotStatus());
        assertNull(patch.getValue().getVideoAssetId());
        assertEquals("vid-old", shot.getVideoAssetId());
        @SuppressWarnings("unchecked")
        Map<String, Object> done = (Map<String, Object>) service.submit(submit).getResult();
        assertEquals(false, done.get("resumed_existing_video"));
    }

    @Test
    void seedPlaceholderFallsBackToTextToVideoWithoutResume() {
        ShortShot shot = readyShot();
        when(tasks.resolveShots(any())).thenReturn(List.of(shot));
        ShortAsset seed = new ShortAsset();
        seed.setAssetId("frame-1");
        seed.setStorageKey("<svg xmlns='http://www.w3.org/2000/svg'>seed 9:16</svg>");
        seed.setMimeType("image/svg+xml");
        seed.setFileSizeByte(80);
        when(assets.selectOneByExample(any())).thenReturn(seed);
        when(imageToVideo.readyForOfficial()).thenReturn(true);
        when(aiTasks.selectByExample(any())).thenReturn(List.of());
        ShortAiTask queued = new ShortAiTask();
        queued.setTaskId("t-t2v");
        when(queue.enqueue(anyString(), anyString(), anyString(), eq("VIDEO"), eq("SHOT"), eq("s-1"),
                any(), anyString(), eq(0), eq("bgssai-video"))).thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> request = invocation.getArgument(6);
                    assertFalse(request.containsKey("resume_request_id"));
                    assertEquals("bgssai-video", request.get("model_code"));
                    return queued;
                });
        AiTaskReq submit = req();
        submit.setConfirmCost(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> done = (Map<String, Object>) service.submit(submit).getResult();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) done.get("shot_list");
        assertEquals(OfficialVideoCopy.ROUTE_T2V, rows.get(0).get("route"));
        assertTrue(String.valueOf(rows.get(0).get("size_note")).contains("720×480"));
        verify(imageToVideo, never()).enqueueForShot(any(), any(), any(), anyInt());
        assertEquals("vid-old", shot.getVideoAssetId());
    }

    @Test
    void aRunningOfficialVideoDoesNotStartAnotherBilledJob() {
        ShortShot shot = readyShot();
        when(tasks.resolveShots(any())).thenReturn(List.of(shot));
        when(assets.selectOneByExample(any())).thenReturn(jpeg("frame-1"));
        when(imageToVideo.readyForOfficial()).thenReturn(true);
        ShortAiTask running = new ShortAiTask();
        running.setTargetId("s-1");
        running.setTaskType("VIDEO");
        running.setTargetType("SHOT");
        running.setTaskStatus("RUNNING");
        running.setModelCode("bgssai-video");
        when(aiTasks.selectByExample(any())).thenReturn(List.of(running));
        AiTaskReq submit = req();
        submit.setConfirmCost(1);
        BizException ex = assertThrows(BizException.class, () -> service.submit(submit));
        assertTrue(ex.getMessage().contains("已在生成中"));
        verify(imageToVideo, never()).enqueueForShot(any(), any(), any(), anyInt());
        verify(queue, never()).enqueue(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any());
        verify(shots, never()).updateByExampleSelective(any(), any());
    }

    private static AiTaskReq req() {
        AiTaskReq req = new AiTaskReq();
        req.setShotIds(List.of("s-1"));
        return req;
    }

    private static ShortShot readyShot() {
        ShortShot shot = new ShortShot();
        shot.setId(9L);
        shot.setShotId("s-1");
        shot.setShotNo(1);
        shot.setUserId("u-1");
        shot.setWorkId("w-1");
        shot.setEpisodeId("e-1");
        shot.setShotStatus("VIDEO_READY");
        shot.setVideoAssetId("vid-old");
        shot.setFirstFrameAssetId("frame-1");
        shot.setImagePrompt("对峙");
        shot.setShotDurationSec(6);
        return shot;
    }

    private static ShortAsset jpeg(String id) {
        ShortAsset asset = new ShortAsset();
        asset.setAssetId(id);
        asset.setStorageKey("short/dev/u/frame.jpg");
        asset.setMimeType("image/jpeg");
        asset.setFileSizeByte(1200);
        return asset;
    }
}
