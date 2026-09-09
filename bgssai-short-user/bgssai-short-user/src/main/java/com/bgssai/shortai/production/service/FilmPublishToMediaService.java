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
import com.bgssai.shortai.production.media.MediaPublishException;
import com.bgssai.shortai.production.media.RestMediaIngestClient;
import com.bgssai.shortai.production.support.ShortJson;

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
        String idempotencyKey = String.valueOf(payload.get("idempotency_key"));

        try {
            MediaIngestClient.IngestResult ingested = mediaIngestClient.ingest(payload);
            Date publishedAt = new Date();
            persistSuccess(film, ingested, idempotencyKey, publishedAt);
            return Responses.ok(toResult(film, ingested, idempotencyKey, publishedAt));
        } catch (BizException ex) {
            Integer httpStatus = null;
            int attempts = 1;
            if (ex instanceof MediaPublishException mpe) {
                httpStatus = mpe.getHttpStatus();
                attempts = mpe.getAttempts();
            }
            persistFailure(film, idempotencyKey, httpStatus, attempts, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Dev-friendly preflight: config + film gates + last publish_log snapshot.
     * Does not call media.
     */
    public ApiResponse preflight(String episodeId, String filmId) {
        Map<String, Object> result = new LinkedHashMap<>();
        RestMediaIngestClient.ConfigStatus config = resolveConfigStatus();
        result.put("base_url_configured", config.baseUrlConfigured());
        result.put("token_configured", config.tokenConfigured());
        result.put("config_ready", config.ready());

        List<String> warnings = new ArrayList<>();
        if (!config.baseUrlConfigured()) {
            warnings.add("未配置 bgssai.media.base-url：请在 application-*.properties 填写 media 服务地址");
        }
        if (!config.tokenConfigured()) {
            warnings.add("未配置 bgssai.media.ingest-token：请写 properties 占位或在 Admin/sys_secret 写入"
                    + "（勿提交真实密钥）");
        }

        boolean filmReady = false;
        boolean reviewApproved = false;
        boolean publishable = false;
        String gateReason = "";
        Map<String, Object> publishLog = null;

        if ((episodeId != null && !episodeId.isBlank()) || (filmId != null && !filmId.isBlank())) {
            try {
                ShortFilm film = resolveFilm(episodeId, filmId);
                ShortEpisode episode = workAccessService.requireEpisode(film.getEpisodeId());
                filmReady = "READY".equals(film.getComposeStatus())
                        && (film.getExpiredFlag() == null || film.getExpiredFlag() == 0)
                        && film.getFilmAssetId() != null && !film.getFilmAssetId().isBlank();
                reviewApproved = "APPROVED".equals(episode.getReviewStatus());
                try {
                    assertPublishable(film, episode);
                    publishable = true;
                } catch (BizException gate) {
                    gateReason = gate.getMessage();
                    publishable = false;
                }
                publishLog = buildPublishLog(film);
                result.put("film_id", film.getFilmId());
                result.put("episode_id", film.getEpisodeId());
                result.put("work_id", film.getWorkId());
                result.put("compose_status", film.getComposeStatus());
                result.put("review_status", episode.getReviewStatus());
            } catch (BizException ex) {
                gateReason = ex.getMessage();
                warnings.add(ex.getMessage());
            }
        }

        result.put("film_ready", filmReady);
        result.put("review_approved", reviewApproved);
        result.put("publishable", publishable && config.ready());
        result.put("gate_reason", gateReason);
        result.put("warnings", warnings);
        if (publishLog != null) {
            result.put("publish_log", publishLog);
        }
        return Responses.ok(result);
    }

    static Map<String, Object> buildPublishLog(ShortFilm film) {
        if (film == null) {
            return null;
        }
        Map<String, Object> fromJson = ShortJson.map(film.getMediaPublishLog());
        if (!fromJson.isEmpty()) {
            return fromJson;
        }
        String status = film.getMediaPublishStatus();
        boolean hasLog = (status != null && !status.isBlank() && !"NONE".equals(status))
                || (film.getMediaId() != null && !film.getMediaId().isBlank())
                || (film.getMediaLastError() != null && !film.getMediaLastError().isBlank());
        if (!hasLog) {
            return null;
        }
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("idempotency_key", MediaIngestContract.idempotencyKey(
                film.getWorkId(), film.getEpisodeId(), film.getFilmId()));
        log.put("media_id", film.getMediaId());
        log.put("play_url", film.getMediaPlayUrl());
        log.put("status", status == null || status.isBlank() ? "NONE" : status);
        log.put("http_status", null);
        log.put("error", film.getMediaLastError());
        log.put("published_at", film.getMediaPublishedAt());
        log.put("attempts", 1);
        return log;
    }

    static Map<String, Object> richPublishLog(String idempotencyKey, String mediaId, String playUrl,
            String status, Integer httpStatus, String error, int attempts, Date at) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("idempotency_key", idempotencyKey);
        log.put("media_id", mediaId);
        log.put("play_url", playUrl);
        log.put("status", status);
        log.put("http_status", httpStatus);
        log.put("error", error == null ? "" : error);
        log.put("attempts", attempts);
        log.put("published_at", at);
        return log;
    }

    private RestMediaIngestClient.ConfigStatus resolveConfigStatus() {
        if (mediaIngestClient instanceof RestMediaIngestClient rest) {
            return rest.configStatus();
        }
        return new RestMediaIngestClient.ConfigStatus(true, true);
    }

    static void assertPublishable(ShortFilm film, ShortEpisode episode) {
        if (film == null || (film.getDeletedFlag() != null && film.getDeletedFlag() == 1)) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "成片不存在，无法发布到 Media");
        }
        if (!"READY".equals(film.getComposeStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "成片未就绪（compose_status=" + nullToDash(film.getComposeStatus())
                            + "，需 READY）后才能发布到 Media");
        }
        if (film.getExpiredFlag() != null && film.getExpiredFlag() == 1) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "成片已过期，请重新合成后再发布到 Media");
        }
        if (film.getFilmAssetId() == null || film.getFilmAssetId().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "成片素材缺失，无法发布到 Media");
        }
        if (!"APPROVED".equals(episode.getReviewStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "剧集审核未通过（review_status=" + nullToDash(episode.getReviewStatus())
                            + "，需 APPROVED）后才能发布到 Media");
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

    private static String nullToDash(String v) {
        return v == null || v.isBlank() ? "-" : v;
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

    private void persistSuccess(ShortFilm film, MediaIngestClient.IngestResult ingested,
            String idempotencyKey, Date publishedAt) {
        Map<String, Object> log = richPublishLog(idempotencyKey, ingested.mediaId(), ingested.playUrl(),
                ingested.status(), ingested.httpStatus(), "", ingested.attempts(), publishedAt);
        String logJson = ShortJson.toJson(log);
        ShortFilm patch = new ShortFilm();
        patch.setMediaId(ingested.mediaId());
        patch.setMediaPlayUrl(ingested.playUrl());
        patch.setMediaPublishStatus(ingested.status());
        patch.setMediaPublishedAt(publishedAt);
        patch.setMediaLastError("");
        patch.setMediaPublishLog(logJson);
        patch.setUpdatedTime(publishedAt);
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);
        film.setMediaId(ingested.mediaId());
        film.setMediaPlayUrl(ingested.playUrl());
        film.setMediaPublishStatus(ingested.status());
        film.setMediaPublishedAt(publishedAt);
        film.setMediaLastError("");
        film.setMediaPublishLog(logJson);
    }

    private void persistFailure(ShortFilm film, String idempotencyKey, Integer httpStatus, int attempts,
            String error) {
        String msg = error == null ? "media publish failed" : error;
        if (msg.length() > 1000) {
            msg = msg.substring(0, 1000);
        }
        Date at = new Date();
        Map<String, Object> log = richPublishLog(idempotencyKey, film.getMediaId(), film.getMediaPlayUrl(),
                "FAILED", httpStatus, msg, attempts, at);
        String logJson = ShortJson.toJson(log);
        ShortFilm patch = new ShortFilm();
        patch.setMediaPublishStatus("FAILED");
        patch.setMediaLastError(msg);
        patch.setMediaPublishLog(logJson);
        patch.setUpdatedTime(at);
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);
        film.setMediaPublishStatus("FAILED");
        film.setMediaLastError(msg);
        film.setMediaPublishLog(logJson);
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
        result.put("http_status", ingested.httpStatus());
        result.put("attempts", ingested.attempts());
        result.put("publish_log", buildPublishLog(film));
        return result;
    }
}
