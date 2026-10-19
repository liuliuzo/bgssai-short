package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortEditExportMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.EditProjectDtos.EditProjectExportReq;

class EditProjectServiceTest {

    private final ShortShotMapper shotMapper = mock(ShortShotMapper.class);
    private final ShortAssetMapper assetMapper = mock(ShortAssetMapper.class);
    private final ShortDubbingMapper dubbingMapper = mock(ShortDubbingMapper.class);
    private final ShortEditExportMapper exportMapper = mock(ShortEditExportMapper.class);
    private final WorkAccessService workAccessService = mock(WorkAccessService.class);
    private final TaskExecutor executor = mock(TaskExecutor.class);

    private EditProjectService service(List<ShortShot> shots, List<ShortAsset> videos) {
        EditProjectService service = new EditProjectService();
        ReflectionTestUtils.setField(service, "shortShotMapper", shotMapper);
        ReflectionTestUtils.setField(service, "shortAssetMapper", assetMapper);
        ReflectionTestUtils.setField(service, "shortDubbingMapper", dubbingMapper);
        ReflectionTestUtils.setField(service, "shortEditExportMapper", exportMapper);
        ReflectionTestUtils.setField(service, "workAccessService", workAccessService);
        ReflectionTestUtils.setField(service, "editExportExecutor", executor);

        ShortEpisode episode = new ShortEpisode();
        episode.setId(1L);
        episode.setEpisodeId("e-1");
        episode.setWorkId("w-1");
        episode.setUserId("u-1");
        episode.setEpisodeNo(14);
        // 刻意造一个「远没准备好发布」的状态：没合成、没提审
        episode.setComposeStatus("NONE");
        episode.setReviewStatus("NOT_SUBMITTED");
        when(workAccessService.requireEpisode(any())).thenReturn(episode);

        ShortWork work = new ShortWork();
        work.setId(2L);
        work.setWorkId("w-1");
        work.setUserId("u-1");
        work.setWorkTitle("退婚三年后她成了首富");
        when(workAccessService.requireWork(any())).thenReturn(work);

        when(shotMapper.selectByExample(any())).thenReturn(shots);
        when(dubbingMapper.selectByExample(any())).thenReturn(new ArrayList<>());
        when(assetMapper.selectOneByExample(any())).thenAnswer(invocation -> videos.isEmpty()
                ? null : videos.get(0));
        when(exportMapper.selectOneByExample(any())).thenReturn(null);
        return service;
    }

    private ShortShot shot(int shotNo, String videoAssetId) {
        ShortShot shot = new ShortShot();
        shot.setShotId("s-" + shotNo);
        shot.setEpisodeId("e-1");
        shot.setShotNo(shotNo);
        shot.setShotDesc("镜头 " + shotNo);
        shot.setShotDurationSec(4);
        shot.setVideoAssetId(videoAssetId);
        return shot;
    }

    private ShortAsset video(long sizeByte) {
        ShortAsset asset = new ShortAsset();
        asset.setId(9L);
        asset.setAssetId("a-1");
        asset.setAssetType("VIDEO");
        asset.setMimeType("video/mp4");
        asset.setStorageKey("short/local/u-1/w-1/a-1.mp4");
        asset.setFileSizeByte((int) Math.min(sizeByte, Integer.MAX_VALUE));
        asset.setWidth(1080);
        asset.setHeight(1920);
        asset.setDurationSec(4);
        return asset;
    }

    @Test
    void exportsEvenWhenEpisodeIsNeitherComposedNorApproved() {
        List<ShortShot> shots = List.of(shot(1, "a-1"), shot(2, null));
        EditProjectService service = service(shots, List.of(video(10_000_000L)));

        EditProjectExportReq req = new EditProjectExportReq();
        req.setEpisodeId("e-1");
        req.setProjectFormat("FCPXML");
        ApiResponse response = service.export(req);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertEquals("QUEUED", result.get("export_status"));
        verify(exportMapper).insertSelective(any());
        verify(executor).execute(any());
    }

    @Test
    void rejectsWhenNoShotHasVideo() {
        List<ShortShot> shots = List.of(shot(1, null), shot(2, null));
        EditProjectService service = service(shots, List.of());

        EditProjectExportReq req = new EditProjectExportReq();
        req.setEpisodeId("e-1");
        req.setProjectFormat("EDL");
        BizException ex = assertThrows(BizException.class, () -> service.export(req));
        assertEquals("4006", ex.getCode());
        verify(exportMapper, never()).insertSelective(any());
    }

    @Test
    void rejectsJianyingWithoutDraftDir() {
        EditProjectService service = service(List.of(shot(1, "a-1")), List.of(video(10_000_000L)));

        EditProjectExportReq req = new EditProjectExportReq();
        req.setEpisodeId("e-1");
        req.setProjectFormat("JIANYING");
        BizException ex = assertThrows(BizException.class, () -> service.export(req));
        assertEquals("50000001", ex.getCode());
    }

    @Test
    void rejectsUnknownProjectFormat() {
        EditProjectService service = service(List.of(shot(1, "a-1")), List.of(video(10_000_000L)));

        EditProjectExportReq req = new EditProjectExportReq();
        req.setEpisodeId("e-1");
        req.setProjectFormat("PREMIERE");
        BizException ex = assertThrows(BizException.class, () -> service.export(req));
        assertEquals("50000001", ex.getCode());
    }

    @Test
    void rejectsWithMediaOverTwoGigabytesButAllowsLinkOnly() {
        List<ShortShot> shots = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            shots.add(shot(i, "a-1"));
        }
        // 三镜各接近 2 GB 上限的三分之一以上，合计越界
        EditProjectService service = service(shots, List.of(video(900_000_000L)));

        EditProjectExportReq withMedia = new EditProjectExportReq();
        withMedia.setEpisodeId("e-1");
        withMedia.setProjectFormat("EDL");
        withMedia.setMediaMode("WITH_MEDIA");
        BizException ex = assertThrows(BizException.class, () -> service.export(withMedia));
        assertEquals("4017", ex.getCode());

        EditProjectExportReq linkOnly = new EditProjectExportReq();
        linkOnly.setEpisodeId("e-1");
        linkOnly.setProjectFormat("EDL");
        linkOnly.setMediaMode("LINK_ONLY");
        ApiResponse response = service.export(linkOnly);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertEquals("QUEUED", result.get("export_status"));
    }

    @Test
    void precheckNamesMissingShotsAndStaysExportable() {
        List<ShortShot> shots = List.of(shot(1, "a-1"), shot(2, null), shot(3, null));
        EditProjectService service = service(shots, List.of(video(10_000_000L)));

        ApiResponse response = service.precheck("e-1");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertEquals(1, result.get("exportable_flag"));
        assertEquals(1, result.get("ready_shot_count"));
        assertEquals(3, result.get("total_shot_count"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> missing = (List<Map<String, Object>>) result.get("missing_shot_list");
        assertEquals(2, missing.size());
        assertEquals(2, missing.get(0).get("shot_no"));
        assertEquals(3, missing.get(1).get("shot_no"));
        @SuppressWarnings("unchecked")
        Map<String, Object> timeline = (Map<String, Object>) result.get("timeline_version");
        assertEquals("DRAFT", timeline.get("card_status"));
        assertEquals("", timeline.get("version_label"));
    }
}
