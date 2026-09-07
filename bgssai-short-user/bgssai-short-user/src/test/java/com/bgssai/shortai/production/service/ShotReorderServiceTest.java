package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.production.dto.ShotReorderDtos.ShotReorderReq;

class ShotReorderServiceTest {

    private final ShortShotMapper shotMapper = mock(ShortShotMapper.class);
    private final WorkAccessService workAccessService = mock(WorkAccessService.class);

    private ShotReorderService service(List<ShortShot> shots) {
        ShotReorderService service = new ShotReorderService();
        ReflectionTestUtils.setField(service, "shortShotMapper", shotMapper);
        ReflectionTestUtils.setField(service, "workAccessService", workAccessService);
        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("e-1");
        ep.setWorkId("w-1");
        ep.setUserId("u-1");
        when(workAccessService.requireEpisode(any())).thenReturn(ep);
        when(shotMapper.selectByExample(any())).thenReturn(shots);
        return service;
    }

    private ShortShot shot(long id, int shotNo, Integer sceneNo, String status, String linkMode) {
        ShortShot shot = new ShortShot();
        shot.setId(id);
        shot.setShotId("s-" + shotNo);
        shot.setEpisodeId("e-1");
        shot.setShotNo(shotNo);
        shot.setSceneNo(sceneNo);
        shot.setShotStatus(status);
        shot.setFrameLinkMode(linkMode);
        return shot;
    }

    private ShotReorderReq req(Integer sceneNo, List<String> ids) {
        ShotReorderReq req = new ShotReorderReq();
        req.setEpisodeId("e-1");
        req.setSceneNo(sceneNo);
        req.setShotIdList(ids);
        return req;
    }

    @Test
    void reordersWithinSceneAndKeepsOtherScenesUntouched() {
        List<ShortShot> shots = new ArrayList<>();
        shots.add(shot(1L, 1, 1, "PENDING", "NONE"));
        shots.add(shot(2L, 2, 1, "PENDING", "NONE"));
        shots.add(shot(3L, 3, 1, "PENDING", "NONE"));
        shots.add(shot(4L, 4, 2, "PENDING", "NONE"));
        ShotReorderService service = service(shots);

        ApiResponse response = service.reorder(req(1, List.of("s-3", "s-1", "s-2")));
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertEquals(3, result.get("reordered_count"));

        ArgumentCaptor<ShortShot> captor = ArgumentCaptor.forClass(ShortShot.class);
        verify(shotMapper, org.mockito.Mockito.atLeast(1)).updateByExampleSelective(captor.capture(), any());
        // 只重新分配本场次原有的镜号 1..3，场次 2 的镜 4 不参与
        for (ShortShot patch : captor.getAllValues()) {
            if (patch.getShotNo() != null) {
                assertTrue(patch.getShotNo() >= 1 && patch.getShotNo() <= 3,
                        "unexpected shot_no " + patch.getShotNo());
            }
        }
    }

    @Test
    void rejectsShotFromAnotherScene() {
        List<ShortShot> shots = new ArrayList<>();
        shots.add(shot(1L, 1, 1, "PENDING", "NONE"));
        shots.add(shot(2L, 2, 2, "PENDING", "NONE"));
        ShotReorderService service = service(shots);

        BizException ex = assertThrows(BizException.class,
                () -> service.reorder(req(1, List.of("s-1", "s-2"))));
        assertEquals("50000001", ex.getCode());
        verify(shotMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void rejectsLockedShot() {
        List<ShortShot> shots = new ArrayList<>();
        shots.add(shot(1L, 1, 1, "LOCKED", "NONE"));
        shots.add(shot(2L, 2, 1, "PENDING", "NONE"));
        ShotReorderService service = service(shots);

        BizException ex = assertThrows(BizException.class,
                () -> service.reorder(req(1, List.of("s-2", "s-1"))));
        assertEquals("4006", ex.getCode());
        verify(shotMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void rejectsPartialSceneOrder() {
        List<ShortShot> shots = new ArrayList<>();
        shots.add(shot(1L, 1, 1, "PENDING", "NONE"));
        shots.add(shot(2L, 2, 1, "PENDING", "NONE"));
        shots.add(shot(3L, 3, 1, "PENDING", "NONE"));
        ShotReorderService service = service(shots);

        BizException ex = assertThrows(BizException.class,
                () -> service.reorder(req(1, List.of("s-2", "s-1"))));
        assertEquals("50000001", ex.getCode());
    }

    @Test
    void rejectsEmptyList() {
        ShotReorderService service = service(new ArrayList<>());
        BizException ex = assertThrows(BizException.class,
                () -> service.reorder(req(1, new ArrayList<>())));
        assertEquals("50000001", ex.getCode());
    }
}
