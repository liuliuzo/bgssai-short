package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortFilm;

class FinishedFilmLifecycleTest {

    @Test
    void upstreamVideoChangeExpiresCutAndDropsMediaReplay() {
        ShortFilmMapper filmMapper = mock(ShortFilmMapper.class);
        ShortEpisodeMapper episodeMapper = mock(ShortEpisodeMapper.class);
        WorkStatusService workStatus = mock(WorkStatusService.class);
        FinishedFilmLifecycle lifecycle = lifecycle(filmMapper, episodeMapper, workStatus);

        ShortFilm film = publishedFilm();
        ShortEpisode episode = episode("APPROVED", "READY");
        when(filmMapper.selectOneByExample(any())).thenReturn(film);
        when(episodeMapper.selectOneByExample(any())).thenReturn(episode);

        lifecycle.expireAfterUpstreamChange("ep-1");

        ArgumentCaptor<ShortFilm> filmCap = ArgumentCaptor.forClass(ShortFilm.class);
        verify(filmMapper).updateByExampleSelective(filmCap.capture(), any());
        assertEquals("NONE", filmCap.getValue().getComposeStatus());
        assertEquals(1, filmCap.getValue().getExpiredFlag());
        assertEquals("NONE", filmCap.getValue().getMediaPublishStatus());
        assertEquals("", filmCap.getValue().getMediaPlayUrl());

        ArgumentCaptor<ShortEpisode> epCap = ArgumentCaptor.forClass(ShortEpisode.class);
        verify(episodeMapper).updateByExampleSelective(epCap.capture(), any());
        assertEquals("NONE", epCap.getValue().getComposeStatus());
        assertEquals("NOT_SUBMITTED", epCap.getValue().getReviewStatus());
        verify(workStatus).refresh("w-1");
    }

    @Test
    void noFilmYetDoesNotWrite() {
        ShortFilmMapper filmMapper = mock(ShortFilmMapper.class);
        ShortEpisodeMapper episodeMapper = mock(ShortEpisodeMapper.class);
        FinishedFilmLifecycle lifecycle = lifecycle(filmMapper, episodeMapper, mock(WorkStatusService.class));
        when(filmMapper.selectOneByExample(any())).thenReturn(null);
        lifecycle.expireAfterUpstreamChange("ep-1");
        verify(filmMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void freshComposeClearsExpiryAndStopsReplay() {
        FinishedFilmLifecycle lifecycle = new FinishedFilmLifecycle();
        ShortFilm previous = publishedFilm();
        ShortFilm patch = new ShortFilm();
        lifecycle.applyFreshCompose(patch, previous);
        assertEquals(0, patch.getExpiredFlag());
        assertEquals(0, patch.getSubtitleStaleFlag());
        assertEquals("NONE", patch.getMediaPublishStatus());
        assertEquals("", patch.getMediaPlayUrl());
    }

    @Test
    void failedComposeRestoresPreviousReadyCut() {
        ShortFilmMapper filmMapper = mock(ShortFilmMapper.class);
        ShortEpisodeMapper episodeMapper = mock(ShortEpisodeMapper.class);
        FinishedFilmLifecycle lifecycle = lifecycle(filmMapper, episodeMapper, mock(WorkStatusService.class));
        ShortFilm film = publishedFilm();
        film.setMediaPublishStatus("NONE");
        when(filmMapper.selectOneByExample(any())).thenReturn(film);
        when(episodeMapper.selectOneByExample(any())).thenReturn(episode("APPROVED", "COMPOSING"));

        lifecycle.onComposeTaskFailed("ep-1");

        ArgumentCaptor<ShortEpisode> epCap = ArgumentCaptor.forClass(ShortEpisode.class);
        verify(episodeMapper).updateByExampleSelective(epCap.capture(), any());
        assertEquals("READY", epCap.getValue().getComposeStatus());
        verify(filmMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void failedFirstComposeRecordsFailed() {
        ShortFilmMapper filmMapper = mock(ShortFilmMapper.class);
        ShortEpisodeMapper episodeMapper = mock(ShortEpisodeMapper.class);
        FinishedFilmLifecycle lifecycle = lifecycle(filmMapper, episodeMapper, mock(WorkStatusService.class));
        ShortFilm film = new ShortFilm();
        film.setId(3L);
        film.setComposeStatus("NONE");
        film.setExpiredFlag(0);
        when(filmMapper.selectOneByExample(any())).thenReturn(film);
        when(episodeMapper.selectOneByExample(any())).thenReturn(episode("NOT_SUBMITTED", "COMPOSING"));

        lifecycle.onComposeTaskFailed("ep-1");

        ArgumentCaptor<ShortEpisode> epCap = ArgumentCaptor.forClass(ShortEpisode.class);
        verify(episodeMapper).updateByExampleSelective(epCap.capture(), any());
        assertEquals("FAILED", epCap.getValue().getComposeStatus());
        ArgumentCaptor<ShortFilm> filmCap = ArgumentCaptor.forClass(ShortFilm.class);
        verify(filmMapper).updateByExampleSelective(filmCap.capture(), any());
        assertEquals("FAILED", filmCap.getValue().getComposeStatus());
    }

    private static FinishedFilmLifecycle lifecycle(ShortFilmMapper filmMapper, ShortEpisodeMapper episodeMapper,
            WorkStatusService workStatus) {
        FinishedFilmLifecycle lifecycle = new FinishedFilmLifecycle();
        ReflectionTestUtils.setField(lifecycle, "shortFilmMapper", filmMapper);
        ReflectionTestUtils.setField(lifecycle, "shortEpisodeMapper", episodeMapper);
        ReflectionTestUtils.setField(lifecycle, "workStatusService", workStatus);
        return lifecycle;
    }

    private static ShortFilm publishedFilm() {
        ShortFilm film = new ShortFilm();
        film.setId(9L);
        film.setFilmId("fl-1");
        film.setEpisodeId("ep-1");
        film.setWorkId("w-1");
        film.setFilmAssetId("a-film");
        film.setComposeStatus("READY");
        film.setExpiredFlag(0);
        film.setMediaId("md-1");
        film.setMediaPlayUrl("https://media.example/play/md-1");
        film.setMediaPublishStatus("READY");
        return film;
    }

    private static ShortEpisode episode(String review, String compose) {
        ShortEpisode episode = new ShortEpisode();
        episode.setId(1L);
        episode.setEpisodeId("ep-1");
        episode.setWorkId("w-1");
        episode.setReviewStatus(review);
        episode.setComposeStatus(compose);
        return episode;
    }
}
