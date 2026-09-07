package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.production.dto.FrameLinkDtos.FrameLinkBatchReq;
import com.bgssai.shortai.production.dto.FrameLinkDtos.FrameLinkSaveReq;

class FrameLinkServiceTest {

    private final ShortShotMapper shotMapper = mock(ShortShotMapper.class);
    private final WorkAccessService workAccessService = mock(WorkAccessService.class);
    private final AiModelResolveService aiModelResolveService = mock(AiModelResolveService.class);
    private final AiTaskQueue aiTaskQueue = mock(AiTaskQueue.class);

    private FrameLinkService service() {
        FrameLinkService service = new FrameLinkService();
        ReflectionTestUtils.setField(service, "shortShotMapper", shotMapper);
        ReflectionTestUtils.setField(service, "workAccessService", workAccessService);
        ReflectionTestUtils.setField(service, "aiModelResolveService", aiModelResolveService);
        ReflectionTestUtils.setField(service, "aiTaskQueue", aiTaskQueue);
        when(workAccessService.requireUser()).thenReturn("u-1");
        return service;
    }

    private ShortShot shot(long id, int shotNo, Integer sceneNo, String mode, String lastFrame) {
        ShortShot shot = new ShortShot();
        shot.setId(id);
        shot.setShotId("s-" + shotNo);
        shot.setEpisodeId("e-1");
        shot.setWorkId("w-1");
        shot.setUserId("u-1");
        shot.setShotNo(shotNo);
        shot.setSceneNo(sceneNo);
        shot.setFrameLinkMode(mode);
        shot.setLastFrameAssetId(lastFrame);
        return shot;
    }

    @Test
    void inheritsPreviousShotWithinSameScene() {
        FrameLinkService service = service();
        ShortShot prev = shot(1L, 4, 3, "NONE", "a-last-4");
        ShortShot current = shot(2L, 5, 3, "INHERIT_PREV", null);
        when(shotMapper.selectByExample(any())).thenReturn(List.of(prev));

        Map<String, Object> state = service.effectiveState(current);
        assertEquals(1, state.get("link_effective_flag"));
        assertNull(state.get("downgrade_reason"));
        assertEquals("接镜 4 尾帧", state.get("link_hint"));
    }

    @Test
    void doesNotLinkAcrossScenes() {
        FrameLinkService service = service();
        // 上一场是雨夜车内，本镜已经换到白天办公室：硬接比不接更糟，换场本来就该跳
        ShortShot prev = shot(1L, 5, 3, "NONE", "a-last-5");
        ShortShot current = shot(2L, 6, 4, "INHERIT_PREV", null);
        when(shotMapper.selectByExample(any())).thenReturn(List.of(prev));

        Map<String, Object> state = service.effectiveState(current);
        assertEquals(0, state.get("link_effective_flag"));
        assertEquals("CROSS_SCENE", state.get("downgrade_reason"));
        assertNull(service.previousLinkableShot(current));
    }

    @Test
    void reportsWhenPreviousShotHasNoLastFrameButDoesNotBlock() {
        FrameLinkService service = service();
        ShortShot prev = shot(1L, 4, 3, "NONE", null);
        ShortShot current = shot(2L, 5, 3, "INHERIT_PREV", null);
        when(shotMapper.selectByExample(any())).thenReturn(List.of(prev));

        Map<String, Object> state = service.effectiveState(current);
        assertEquals(0, state.get("link_effective_flag"));
        assertEquals("PREV_NO_LAST_FRAME", state.get("downgrade_reason"));
        assertEquals("上一镜尚无尾帧，本次未衔接", state.get("link_hint"));
    }

    @Test
    void batchLinksSameSceneAndLeavesCrossSceneAlone() {
        FrameLinkService service = service();
        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("e-1");
        ep.setWorkId("w-1");
        ep.setUserId("u-1");
        when(workAccessService.requireEpisode(any())).thenReturn(ep);
        List<ShortShot> shots = new ArrayList<>();
        shots.add(shot(1L, 1, 1, "NONE", null));
        shots.add(shot(2L, 2, 1, "NONE", null));
        shots.add(shot(3L, 3, 2, "NONE", null));
        shots.add(shot(4L, 4, 2, "NONE", null));
        when(shotMapper.selectByExample(any())).thenReturn(shots);

        FrameLinkBatchReq req = new FrameLinkBatchReq();
        req.setEpisodeId("e-1");
        req.setFrameLinkMode("INHERIT_PREV");
        ApiResponse response = service.batch(req);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        // 镜 2 与镜 4 同场衔接；镜 1 是首镜、镜 3 换场，都不接
        assertEquals(2, result.get("linked_count"));
        assertEquals(1, result.get("cross_scene_count"));

        ArgumentCaptor<ShortShot> captor = ArgumentCaptor.forClass(ShortShot.class);
        verify(shotMapper, org.mockito.Mockito.times(4)).updateByExampleSelective(captor.capture(), any());
        List<ShortShot> patches = captor.getAllValues();
        assertEquals("NONE", patches.get(0).getFrameLinkMode());
        assertEquals("INHERIT_PREV", patches.get(1).getFrameLinkMode());
        assertEquals("NONE", patches.get(2).getFrameLinkMode());
        assertEquals("INHERIT_PREV", patches.get(3).getFrameLinkMode());
    }

    @Test
    void rejectsBookendOnModelWithoutLastFrameSupport() {
        FrameLinkService service = service();
        ShortShot current = shot(2L, 5, 3, "NONE", "a-last-5");
        when(shotMapper.selectOneByExample(any())).thenReturn(current);
        AiModelConfig model = new AiModelConfig();
        model.setModelCode("pixverse-video-v6.0");
        model.setConfigJson("{}");
        when(aiModelResolveService.resolve(any(), any())).thenReturn(model);

        FrameLinkSaveReq req = new FrameLinkSaveReq();
        req.setShotId("s-5");
        req.setFrameLinkMode("BOOKEND");
        BizException ex = assertThrows(BizException.class, () -> service.save(req));
        assertEquals("4006", ex.getCode());
        // 拒绝时不该留下半套状态
        verify(shotMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void acceptsBookendWhenModelDeclaresSupport() {
        FrameLinkService service = service();
        ShortShot current = shot(2L, 5, 3, "NONE", "a-last-5");
        when(shotMapper.selectOneByExample(any())).thenReturn(current);
        AiModelConfig model = new AiModelConfig();
        model.setModelCode("kling-video-v3");
        model.setConfigJson("{\"support_last_frame\":1}");
        when(aiModelResolveService.resolve(any(), any())).thenReturn(model);

        FrameLinkSaveReq req = new FrameLinkSaveReq();
        req.setShotId("s-5");
        req.setFrameLinkMode("BOOKEND");
        ApiResponse response = service.save(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertEquals("BOOKEND", result.get("frame_link_mode"));
        assertEquals(1, result.get("link_effective_flag"));
    }

    @Test
    void rejectsUnknownMode() {
        FrameLinkService service = service();
        FrameLinkSaveReq req = new FrameLinkSaveReq();
        req.setShotId("s-5");
        req.setFrameLinkMode("CHAIN");
        BizException ex = assertThrows(BizException.class, () -> service.save(req));
        assertEquals("50000001", ex.getCode());
    }

    @Test
    void marksNextShotStaleOnlyWhenItInheritsInSameScene() {
        FrameLinkService service = service();
        ShortShot changed = shot(1L, 4, 3, "NONE", "a-last-4");
        ShortShot nextSameScene = shot(2L, 5, 3, "INHERIT_PREV", null);
        when(shotMapper.selectByExample(any())).thenReturn(List.of(nextSameScene));
        service.markNextStale(changed);
        ArgumentCaptor<ShortShot> captor = ArgumentCaptor.forClass(ShortShot.class);
        verify(shotMapper).updateByExampleSelective(captor.capture(), any());
        assertEquals(Integer.valueOf(1), captor.getValue().getStaleFlag());

        // 下一镜没开衔接就不该被打扰
        FrameLinkService other = service();
        ShortShot nextNoLink = shot(3L, 5, 3, "NONE", null);
        when(shotMapper.selectByExample(any())).thenReturn(List.of(nextNoLink));
        other.markNextStale(changed);
        verify(shotMapper, org.mockito.Mockito.times(1)).updateByExampleSelective(any(), any());
    }
}
