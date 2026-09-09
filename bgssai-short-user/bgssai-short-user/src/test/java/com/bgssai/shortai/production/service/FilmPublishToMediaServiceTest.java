package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmPublishToMediaReq;
import com.bgssai.shortai.production.media.MediaIngestClient;
import com.bgssai.shortai.production.media.MediaIngestContract;

class FilmPublishToMediaServiceTest {

    private final ShortFilmMapper filmMapper = mock(ShortFilmMapper.class);
    private final WorkAccessService workAccessService = mock(WorkAccessService.class);
    private final AssetService assetService = mock(AssetService.class);
    private final MediaIngestClient mediaIngestClient = mock(MediaIngestClient.class);

    private FilmPublishToMediaService service() {
        FilmPublishToMediaService service = new FilmPublishToMediaService();
        ReflectionTestUtils.setField(service, "shortFilmMapper", filmMapper);
        ReflectionTestUtils.setField(service, "workAccessService", workAccessService);
        ReflectionTestUtils.setField(service, "assetService", assetService);
        ReflectionTestUtils.setField(service, "mediaIngestClient", mediaIngestClient);
        return service;
    }

    private ShortFilm readyFilm() {
        ShortFilm film = new ShortFilm();
        film.setId(9L);
        film.setFilmId("fl-1");
        film.setEpisodeId("ep-1");
        film.setWorkId("w-1");
        film.setUserId("u-1");
        film.setFilmAssetId("as-film");
        film.setFilmDurationSec(88);
        film.setComposeStatus("READY");
        film.setExpiredFlag(0);
        film.setDeletedFlag(0);
        return film;
    }

    private ShortEpisode approvedEpisode() {
        ShortEpisode ep = new ShortEpisode();
        ep.setId(1L);
        ep.setEpisodeId("ep-1");
        ep.setWorkId("w-1");
        ep.setUserId("u-1");
        ep.setEpisodeNo(1);
        ep.setEpisodeTitle("开场");
        ep.setReviewStatus("APPROVED");
        return ep;
    }

    private ShortWork work() {
        ShortWork work = new ShortWork();
        work.setWorkId("w-1");
        work.setWorkTitle("退婚三年后她成了首富");
        work.setGenre("REVENGE");
        work.setCoverAssetId("as-cover");
        return work;
    }

    @Test
    void payloadMatchesSharedContract() {
        Map<String, Object> payload = MediaIngestContract.buildPayload(
                "w-1", "ep-1", "fl-1", "退婚三年后她成了首富 第1集 开场",
                "https://cover", "https://video.mp4", 88, List.of("REVENGE"));
        assertEquals("bgssai-short", payload.get("source_system"));
        assertEquals("w-1", payload.get("source_work_id"));
        assertEquals("ep-1", payload.get("source_episode_id"));
        assertEquals("fl-1", payload.get("source_film_id"));
        assertEquals("退婚三年后她成了首富 第1集 开场", payload.get("title"));
        assertEquals("https://cover", payload.get("cover_url"));
        assertEquals("https://video.mp4", payload.get("video_url"));
        assertEquals(88, payload.get("duration_sec"));
        assertEquals("9:16", payload.get("aspect_ratio"));
        assertEquals("zh-CN", payload.get("language"));
        assertEquals(List.of("REVENGE"), payload.get("tags"));
        assertEquals("short:w-1:ep-1:fl-1", payload.get("idempotency_key"));
        assertEquals(12, payload.size());
    }

    @Test
    void payloadOmitsBlankCover() {
        Map<String, Object> payload = MediaIngestContract.buildPayload(
                "w-1", "ep-1", "fl-1", "t", null, "https://v.mp4", 10, List.of());
        assertFalse(payload.containsKey("cover_url"));
        assertEquals(11, payload.size());
    }

    @Test
    void gateRejectsWhenNotReady() {
        ShortFilm film = readyFilm();
        film.setComposeStatus("COMPOSING");
        BizException ex = assertThrows(BizException.class,
                () -> FilmPublishToMediaService.assertPublishable(film, approvedEpisode()));
        assertEquals(CommonErrorCode.RULE_REJECT.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("READY"));
    }

    @Test
    void gateRejectsWhenExpired() {
        ShortFilm film = readyFilm();
        film.setExpiredFlag(1);
        BizException ex = assertThrows(BizException.class,
                () -> FilmPublishToMediaService.assertPublishable(film, approvedEpisode()));
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    void gateRejectsWhenNotApproved() {
        ShortEpisode ep = approvedEpisode();
        ep.setReviewStatus("PENDING");
        BizException ex = assertThrows(BizException.class,
                () -> FilmPublishToMediaService.assertPublishable(readyFilm(), ep));
        assertTrue(ex.getMessage().contains("APPROVED"));
    }

    @Test
    void publishSuccessPersistsAndReturnsMediaFields() {
        ShortFilm film = readyFilm();
        ShortEpisode ep = approvedEpisode();
        ShortWork work = work();
        when(filmMapper.selectOneByExample(any())).thenReturn(film);
        when(workAccessService.requireEpisode("ep-1")).thenReturn(ep);
        when(workAccessService.requireWork("w-1")).thenReturn(work);
        when(assetService.resolveUrl("as-film")).thenReturn("https://signed/video.mp4");
        when(assetService.resolveUrl("as-cover")).thenReturn("https://signed/cover.png");
        when(mediaIngestClient.ingest(any())).thenReturn(
                new MediaIngestClient.IngestResult("md-9", "https://media/play/md-9", "READY"));

        FilmPublishToMediaReq req = new FilmPublishToMediaReq();
        req.setEpisodeId("ep-1");
        ApiResponse response = service().publish(req);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertEquals("md-9", result.get("media_id"));
        assertEquals("https://media/play/md-9", result.get("play_url"));
        assertEquals("READY", result.get("status"));
        assertEquals("short:w-1:ep-1:fl-1", result.get("idempotency_key"));
        verify(filmMapper).updateByExampleSelective(any(), any());
        verify(mediaIngestClient).ingest(any());
    }

    @Test
    void publishDoesNotCallMediaWhenGateFails() {
        ShortFilm film = readyFilm();
        film.setComposeStatus("NONE");
        when(filmMapper.selectOneByExample(any())).thenReturn(film);
        when(workAccessService.requireEpisode("ep-1")).thenReturn(approvedEpisode());
        when(workAccessService.requireWork("w-1")).thenReturn(work());

        FilmPublishToMediaReq req = new FilmPublishToMediaReq();
        req.setFilmId("fl-1");
        assertThrows(BizException.class, () -> service().publish(req));
        verify(mediaIngestClient, never()).ingest(any());
    }

    @Test
    void mediaDownSurfacesBizExceptionAndPersistsError() {
        ShortFilm film = readyFilm();
        when(filmMapper.selectOneByExample(any())).thenReturn(film);
        when(workAccessService.requireEpisode("ep-1")).thenReturn(approvedEpisode());
        when(workAccessService.requireWork("w-1")).thenReturn(work());
        when(assetService.resolveUrl("as-film")).thenReturn("https://signed/video.mp4");
        when(assetService.resolveUrl("as-cover")).thenReturn(null);
        when(mediaIngestClient.ingest(any())).thenThrow(
                new BizException(CommonErrorCode.UPSTREAM_ERROR, "media service unavailable: 404"));

        FilmPublishToMediaReq req = new FilmPublishToMediaReq();
        req.setEpisodeId("ep-1");
        BizException ex = assertThrows(BizException.class, () -> service().publish(req));
        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
        verify(filmMapper).updateByExampleSelective(any(), any());
    }

    @Test
    void buildTitleIncludesWorkEpisodeAndName() {
        String title = FilmPublishToMediaService.buildTitle(work(), approvedEpisode());
        assertEquals("退婚三年后她成了首富 第1集 开场", title);
    }

    @Test
    void buildPublishLogWhenHistoryExists() {
        ShortFilm film = readyFilm();
        film.setMediaId("md-1");
        film.setMediaPlayUrl("https://play/1");
        film.setMediaPublishStatus("READY");
        Map<String, Object> log = FilmPublishToMediaService.buildPublishLog(film);
        assertEquals("md-1", log.get("media_id"));
        assertEquals("https://play/1", log.get("play_url"));
        assertEquals("short:w-1:ep-1:fl-1", log.get("idempotency_key"));
    }

    @Test
    void buildPublishLogNullWhenNone() {
        ShortFilm film = readyFilm();
        film.setMediaPublishStatus("NONE");
        assertEquals(null, FilmPublishToMediaService.buildPublishLog(film));
    }

    @Test
    void idempotentRepublishUpdatesLatestResult() {
        ShortFilm film = readyFilm();
        when(filmMapper.selectOneByExample(any())).thenReturn(film);
        when(workAccessService.requireEpisode("ep-1")).thenReturn(approvedEpisode());
        when(workAccessService.requireWork("w-1")).thenReturn(work());
        when(assetService.resolveUrl("as-film")).thenReturn("https://signed/video.mp4");
        when(assetService.resolveUrl("as-cover")).thenReturn(null);
        when(mediaIngestClient.ingest(any()))
                .thenReturn(new MediaIngestClient.IngestResult("md-1", "https://p/1", "READY", 200, 1))
                .thenReturn(new MediaIngestClient.IngestResult("md-1", "https://p/1b", "READY", 200, 1));

        FilmPublishToMediaReq req = new FilmPublishToMediaReq();
        req.setFilmId("fl-1");
        ApiResponse first = service().publish(req);
        ApiResponse second = service().publish(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> r2 = (Map<String, Object>) second.getResult();
        assertEquals("md-1", r2.get("media_id"));
        assertEquals("https://p/1b", r2.get("play_url"));
        assertEquals("short:w-1:ep-1:fl-1", r2.get("idempotency_key"));
        verify(mediaIngestClient, org.mockito.Mockito.times(2)).ingest(any());
        verify(filmMapper, org.mockito.Mockito.times(2)).updateByExampleSelective(any(), any());
        assertTrue(first.isSuccess());
    }

    @Test
    void richPublishLogContainsIdempotencyAndHttpStatus() {
        Map<String, Object> log = FilmPublishToMediaService.richPublishLog(
                "short:w:e:f", "md-9", "https://p", "READY", 200, "", 2, new java.util.Date());
        assertEquals("short:w:e:f", log.get("idempotency_key"));
        assertEquals("md-9", log.get("media_id"));
        assertEquals(200, log.get("http_status"));
        assertEquals(2, log.get("attempts"));
    }
}
