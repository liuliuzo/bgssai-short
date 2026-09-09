package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmPublishToMediaReq;
import com.bgssai.shortai.production.media.MediaIngestClient;
import com.bgssai.shortai.production.media.MediaIngestContract;

@Service
public class FilmPublishToMediaService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ShortFilmMapper shortFilmMapper;
    @Autowired
    private AssetService assetService;
    @Autowired
    private MediaIngestClient mediaIngestClient;

    public ApiResponse publish(FilmPublishToMediaReq req) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_id or film_id required");
        }
        ShortFilm film = resolveFilm(req.getEpisodeId(), req.getFilmId());
        ShortEpisode episode = workAccessService.requireEpisode(film.getEpisodeId());
        ShortWork work = workAccessService.requireWork(film.getWorkId());
        assertPublishable(film, episode);

        String videoUrl = assetService.resolveUrl(film.getFilmAssetId());
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "film video URL unavailable");
        }
        String coverUrl = null;
        if (work.getCoverAssetId() != null && !work.getCoverAssetId().isBlank()) {
            coverUrl = assetService.resolveUrl(work.getCoverAssetId());
        }
        String title = buildTitle(work, episode);
        List<String> tags = new ArrayList<>();
        if (work.getGenre() != null && !work.getGenre().isBlank()) {
            tags.add(work.getGenre());
        }
        Map<String, Object> payload = MediaIngestContract.buildPayload(
                film.getWorkId(), film.getEpisodeId(), film.getFilmId(),
                title, coverUrl, videoUrl, film.getFilmDurationSec(), tags);

        try {
            MediaIngestClient.IngestResult ingested = mediaIngestClient.ingest(payload);
            Date publishedAt = new Date();
            persistSuccess(film, ingested, publishedAt);
            return Responses.ok(toResult(film, ingested, payload.get("idempotency_key"), publishedAt));
        } catch (BizException ex) {
            persistFailure(film, ex.getMessage());
            throw ex;
        }
    }

    static void assertPublishable(ShortFilm film, ShortEpisode episode) {
        if (film == null || (film.getDeletedFlag() != null && film.getDeletedFlag() == 1)) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "film not found");
        }
        if (!"READY".equals(film.getComposeStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "film must be READY before publish to media");
        }
        if (film.getExpiredFlag() != null && film.getExpiredFlag() == 1) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "expired film cannot publish to media");
        }
        if (film.getFilmAssetId() == null || film.getFilmAssetId().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "film asset missing");
        }
        if (!"APPROVED".equals(episode.getReviewStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "episode review must be APPROVED before publish");
        }
    }

    static String buildTitle(ShortWork work, ShortEpisode episode) {
        String workTitle = work.getWorkTitle() == null ? "" : work.getWorkTitle().trim();
        Integer no = episode.getEpisodeNo();
        String epTitle = episode.getEpisodeTitle() == null ? "" : episode.getEpisodeTitle().trim();
        StringBuilder sb = new StringBuilder();
        sb.append(workTitle);
        if (no != null) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append('第').append(no).append('集');
        }
        if (!epTitle.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(epTitle);
        }
        return sb.toString().isBlank() ? filmFallback(work, episode) : sb.toString();
    }

    private static String filmFallback(ShortWork work, ShortEpisode episode) {
        return work.getWorkId() + "/" + episode.getEpisodeId();
    }

    private ShortFilm resolveFilm(String episodeId, String filmId) {
        if ((episodeId == null || episodeId.isBlank()) && (filmId == null || filmId.isBlank())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_id or film_id required");
        }
        if (filmId != null && !filmId.isBlank()) {
            ShortFilmExample ex = new ShortFilmExample();
            ex.createCriteria().andFilmIdEqualTo(filmId.trim()).andDeletedFlagEqualTo(0);
            ShortFilm film = shortFilmMapper.selectOneByExample(ex);
            if (film == null) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "film not found");
            }
            if (episodeId != null && !episodeId.isBlank() && !episodeId.trim().equals(film.getEpisodeId())) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_id does not match film");
            }
            return film;
        }
        ShortFilmExample ex = new ShortFilmExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId.trim()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        ShortFilm film = shortFilmMapper.selectOneByExample(ex);
        if (film == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "film not found for episode");
        }
        return film;
    }

    private void persistSuccess(ShortFilm film, MediaIngestClient.IngestResult ingested, Date publishedAt) {
        ShortFilm patch = new ShortFilm();
        patch.setMediaId(ingested.mediaId());
        patch.setMediaPlayUrl(ingested.playUrl());
        patch.setMediaPublishStatus(ingested.status());
        patch.setMediaPublishedAt(publishedAt);
        patch.setMediaLastError("");
        patch.setUpdatedTime(publishedAt);
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);
        film.setMediaId(ingested.mediaId());
        film.setMediaPlayUrl(ingested.playUrl());
        film.setMediaPublishStatus(ingested.status());
        film.setMediaPublishedAt(publishedAt);
        film.setMediaLastError("");
    }

    private void persistFailure(ShortFilm film, String error) {
        String msg = error == null ? "media publish failed" : error;
        if (msg.length() > 1000) {
            msg = msg.substring(0, 1000);
        }
        ShortFilm patch = new ShortFilm();
        patch.setMediaPublishStatus("FAILED");
        patch.setMediaLastError(msg);
        patch.setUpdatedTime(new Date());
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);
    }

    private static Map<String, Object> toResult(ShortFilm film, MediaIngestClient.IngestResult ingested,
            Object idempotencyKey, Date publishedAt) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("film_id", film.getFilmId());
        result.put("episode_id", film.getEpisodeId());
        result.put("work_id", film.getWorkId());
        result.put("media_id", ingested.mediaId());
        result.put("play_url", ingested.playUrl());
        result.put("status", ingested.status());
        result.put("idempotency_key", idempotencyKey);
        result.put("published_at", publishedAt);
        return result;
    }
}
