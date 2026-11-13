package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.CommercialOutcomeGuard;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmPublishBatchToMediaReq;
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
    private ShortEpisodeMapper shortEpisodeMapper;
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

        String idempotencyKey = MediaIngestContract.idempotencyKey(
                film.getWorkId(), film.getEpisodeId(), film.getFilmId());
        if (isOpenHandoff(film.getMediaPublishStatus())) {
            ApiResponse recovered = recoverOpenHandoff(film, idempotencyKey);
            if (recovered != null) {
                return recovered;
            }
        }
        if (!Boolean.TRUE.equals(req.getForce()) && isReplayable(film, idempotencyKey)) {
            return Responses.ok(toReplayResult(film, idempotencyKey));
        }

        String videoUrl = assetService.resolveUrl(film.getFilmAssetId());
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "film video URL unavailable");
        }
        CommercialOutcomeGuard.rejectFakeUrl(videoUrl, "film video_url");
        if (!MediaIngestContract.isHttpUrl(videoUrl)) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "film video_url must be http(s) for Tube ingest");
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
            String classified = CommercialOutcomeGuard.classifyMediaStatus(ingested.status());
            if ("FAILED".equals(classified)) {
                throw new MediaPublishException(CommonErrorCode.UPSTREAM_ERROR,
                        "Tube handoff FAILED (status=" + ingested.status() + ")",
                        ingested.httpStatus(), ingested.attempts());
            }
            Date publishedAt = new Date();
            if ("UNKNOWN".equals(classified) || "NOT_FOUND".equals(classified)) {
                ingested = new MediaIngestClient.IngestResult(
                        ingested.mediaId(), ingested.playUrl(), "UNKNOWN",
                        ingested.httpStatus(), ingested.attempts(),
                        ingested.message() == null || ingested.message().isBlank()
                                ? "Tube 结果未知，未当成成功或失败" : ingested.message());
                persistUnknown(film, idempotencyKey, ingested.httpStatus(), ingested.attempts(),
                        ingested.message(), publishedAt);
                return Responses.ok(toUnknownResult(film, idempotencyKey, publishedAt, ingested.message()));
            }
            if ("PENDING".equals(classified)) {
                String playUrl = ingested.playUrl() == null ? "" : ingested.playUrl().trim();
                if (!playUrl.isBlank()) {
                    CommercialOutcomeGuard.rejectFakeUrl(playUrl, "tube play_url");
                }
                ingested = new MediaIngestClient.IngestResult(
                        ingested.mediaId(), playUrl, "PENDING",
                        ingested.httpStatus(), ingested.attempts(), ingested.message());
                persistPending(film, ingested, idempotencyKey, publishedAt);
                return Responses.ok(toResult(film, ingested, idempotencyKey, publishedAt, false));
            }
            CommercialOutcomeGuard.rejectFakeUrl(ingested.playUrl(), "tube play_url");
            ingested = new MediaIngestClient.IngestResult(
                    ingested.mediaId(), ingested.playUrl(), classified,
                    ingested.httpStatus(), ingested.attempts(), ingested.message());
            persistSuccess(film, ingested, idempotencyKey, publishedAt);
            return Responses.ok(toResult(film, ingested, idempotencyKey, publishedAt, false));
        } catch (BizException ex) {
            Integer httpStatus = null;
            int attempts = 1;
            if (ex instanceof MediaPublishException mpe) {
                httpStatus = mpe.getHttpStatus();
                attempts = mpe.getAttempts();
            }
            if (isIndeterminate(ex)) {
                Date at = new Date();
                persistUnknown(film, idempotencyKey, httpStatus, attempts, ex.getMessage(), at);
                return Responses.ok(toUnknownResult(film, idempotencyKey, at, ex.getMessage()));
            }
            persistFailure(film, idempotencyKey, httpStatus, attempts, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Dev-friendly preflight: config + film gates + last publish_log snapshot.
     * Does not call tube.
     */
    public ApiResponse preflight(String episodeId, String filmId, String workId) {
        Map<String, Object> result = new LinkedHashMap<>();
        RestMediaIngestClient.ConfigStatus config = resolveConfigStatus();
        result.put("base_url_configured", config.baseUrlConfigured());
        result.put("token_configured", config.tokenConfigured());
        result.put("config_ready", config.ready());

        List<String> warnings = new ArrayList<>();
        if (!config.baseUrlConfigured()) {
            warnings.add("Tube 主机未配置：发布会失败，不会返回假的作品号。"
                    + "请联系管理员完成接入后再发布");
        }
        if (!config.tokenConfigured()) {
            warnings.add("未配置 Tube 令牌：请在环境配置或管理端写入摄入令牌后再发布"
                    + "（勿提交真实密钥，不要编造令牌）");
        }

        boolean filmReady = false;
        boolean reviewApproved = false;
        boolean publishable = false;
        boolean filmPresent = false;
        boolean filmRecordMissing = false;
        String gateReason = "";
        Map<String, Object> publishLog = null;

        if ((episodeId != null && !episodeId.isBlank()) || (filmId != null && !filmId.isBlank())) {
            try {
                ShortFilm film = findFilm(episodeId, filmId);
                if (film == null) {
                    AbsentFilm absent = describeAbsentFilm(episodeId, filmId);
                    gateReason = absent.reason();
                    filmRecordMissing = absent.recordMissing();
                    if (absent.episode() != null) {
                        reviewApproved = "APPROVED".equals(absent.episode().getReviewStatus());
                        result.put("episode_id", absent.episode().getEpisodeId());
                        result.put("work_id", absent.episode().getWorkId());
                        result.put("compose_status", absent.episode().getComposeStatus());
                        result.put("review_status", absent.episode().getReviewStatus());
                    }
                } else {
                    filmPresent = true;
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
                }
            } catch (BizException ex) {
                gateReason = ex.getMessage();
                publishable = false;
            }
        }

        if (workId != null && !workId.isBlank()) {
            ShortWork work = workAccessService.requireWork(workId);
            List<ShortFilm> candidates = listBatchCandidates(work.getWorkId());
            result.put("batch_work_id", work.getWorkId());
            result.put("batch_ready_count", candidates.size());
            List<Map<String, Object>> preview = new ArrayList<>();
            for (ShortFilm film : candidates) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("film_id", film.getFilmId());
                row.put("episode_id", film.getEpisodeId());
                row.put("media_id", film.getMediaId());
                row.put("media_publish_status", film.getMediaPublishStatus());
                preview.add(row);
            }
            result.put("batch_candidate_list", preview);
        }

        result.put("film_present", filmPresent);
        result.put("film_record_missing", filmRecordMissing);
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

    /**
     * Batch-publish all READY + APPROVED films under a work. Sequential with progress fields.
     */
    public ApiResponse publishBatch(FilmPublishBatchToMediaReq req) {
        if (req == null || req.getWorkId() == null || req.getWorkId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "work_id required");
        }
        ShortWork work = workAccessService.requireWork(req.getWorkId());
        List<ShortFilm> candidates = listBatchCandidates(work.getWorkId());
        List<Map<String, Object>> items = new ArrayList<>();
        int succeeded = 0;
        int pending = 0;
        int unknown = 0;
        int failed = 0;
        int total = candidates.size();
        int index = 0;
        for (ShortFilm film : candidates) {
            index += 1;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("progress_index", index);
            item.put("progress_total", total);
            item.put("film_id", film.getFilmId());
            item.put("episode_id", film.getEpisodeId());
            item.put("work_id", film.getWorkId());
            try {
                FilmPublishToMediaReq one = new FilmPublishToMediaReq();
                one.setFilmId(film.getFilmId());
                one.setEpisodeId(film.getEpisodeId());
                one.setForce(req.getForce());
                ApiResponse oneResp = publish(one);
                @SuppressWarnings("unchecked")
                Map<String, Object> oneResult = (Map<String, Object>) oneResp.getResult();
                item.put("ok", true);
                item.put("media_id", oneResult.get("media_id"));
                item.put("play_url", oneResult.get("play_url"));
                item.put("status", oneResult.get("status"));
                item.put("commercial_ready", oneResult.get("commercial_ready"));
                item.put("idempotency_key", oneResult.get("idempotency_key"));
                item.put("replayed", oneResult.get("replayed"));
                item.put("error", "");
                String oneStatus = String.valueOf(oneResult.get("status"));
                if (Boolean.TRUE.equals(oneResult.get("commercial_ready"))) {
                    succeeded += 1;
                } else if ("UNKNOWN".equals(oneStatus)) {
                    unknown += 1;
                    item.put("ok", false);
                    item.put("outcome", "UNKNOWN");
                } else {
                    pending += 1;
                }
            } catch (BizException ex) {
                item.put("ok", false);
                item.put("media_id", film.getMediaId());
                item.put("play_url", film.getMediaPlayUrl());
                item.put("status", "FAILED");
                item.put("error", ex.getMessage());
                failed += 1;
            }
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("work_id", work.getWorkId());
        result.put("total", total);
        result.put("succeeded", succeeded);
        result.put("pending", pending);
        result.put("unknown", unknown);
        result.put("failed", failed);
        result.put("skipped", 0);
        result.put("progress_percent", total == 0 ? 100
                : (int) Math.round(100.0 * (succeeded + pending + unknown + failed) / total));
        result.put("item_list", items);
        return Responses.ok(result);
    }

    /**
     * READY films whose episode is APPROVED (latest film per episode).
     */
    List<ShortFilm> listBatchCandidates(String workId) {
        ShortEpisodeExample epEx = new ShortEpisodeExample();
        epEx.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        epEx.setOrderByClause("episode_no asc, id asc");
        List<ShortEpisode> episodes = shortEpisodeMapper.selectByExample(epEx);
        List<ShortFilm> out = new ArrayList<>();
        for (ShortEpisode episode : episodes) {
            if (!"APPROVED".equals(episode.getReviewStatus())) {
                continue;
            }
            ShortFilmExample filmEx = new ShortFilmExample();
            filmEx.createCriteria().andEpisodeIdEqualTo(episode.getEpisodeId()).andDeletedFlagEqualTo(0)
                    .andComposeStatusEqualTo("READY");
            filmEx.setOrderByClause("id desc");
            ShortFilm film = shortFilmMapper.selectOneByExample(filmEx);
            if (film == null) {
                continue;
            }
            if (film.getExpiredFlag() != null && film.getExpiredFlag() == 1) {
                continue;
            }
            if (film.getFilmAssetId() == null || film.getFilmAssetId().isBlank()) {
                continue;
            }
            out.add(film);
        }
        return out;
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

    static boolean isReplayable(ShortFilm film, String idempotencyKey) {
        if (film == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        if (!CommercialOutcomeGuard.isReadyStatus(film.getMediaPublishStatus())) {
            return false;
        }
        if (film.getMediaId() == null || film.getMediaId().isBlank()) {
            return false;
        }
        if (film.getMediaPlayUrl() == null || film.getMediaPlayUrl().isBlank()) {
            return false;
        }
        try {
            CommercialOutcomeGuard.rejectFakeUrl(film.getMediaPlayUrl(), "tube play_url");
        } catch (BizException ex) {
            return false;
        }
        String existingKey = existingIdempotencyKey(film);
        return existingKey == null || existingKey.isBlank() || idempotencyKey.equals(existingKey);
    }

    static String existingIdempotencyKey(ShortFilm film) {
        Map<String, Object> fromJson = ShortJson.map(film.getMediaPublishLog());
        Object key = fromJson.get("idempotency_key");
        if (key != null && !String.valueOf(key).isBlank()) {
            return String.valueOf(key).trim();
        }
        if (film.getWorkId() == null || film.getEpisodeId() == null || film.getFilmId() == null) {
            return "";
        }
        return MediaIngestContract.idempotencyKey(film.getWorkId(), film.getEpisodeId(), film.getFilmId());
    }

    static Map<String, Object> richPublishLog(String idempotencyKey, String mediaId, String playUrl,
            String status, Integer httpStatus, String error, int attempts, Date at) {
        return richPublishLog(idempotencyKey, mediaId, playUrl, status, httpStatus, error, attempts, at, false);
    }

    static Map<String, Object> richPublishLog(String idempotencyKey, String mediaId, String playUrl,
            String status, Integer httpStatus, String error, int attempts, Date at, boolean replayed) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("idempotency_key", idempotencyKey);
        log.put("media_id", mediaId);
        log.put("play_url", playUrl);
        log.put("status", status);
        log.put("http_status", httpStatus);
        log.put("error", error == null ? "" : error);
        log.put("attempts", attempts);
        log.put("published_at", at);
        log.put("replayed", replayed);
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
            throw new BizException(CommonErrorCode.RULE_REJECT, "成片不存在，无法发布到 Tube");
        }
        if (!"READY".equals(film.getComposeStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "成片未就绪（compose_status=" + nullToDash(film.getComposeStatus())
                            + "，需 READY）后才能发布到 Tube");
        }
        if (film.getExpiredFlag() != null && film.getExpiredFlag() == 1) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "成片已过期，请重新合成后再发布到 Tube");
        }
        if (film.getFilmAssetId() == null || film.getFilmAssetId().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "成片素材缺失，无法发布到 Tube");
        }
        if (film.getFilmDurationSec() != null && film.getFilmDurationSec() <= 0) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "成片时长无效（film_duration_sec=" + film.getFilmDurationSec() + "），无法发布到 Tube");
        }
        if (!"APPROVED".equals(episode.getReviewStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "剧集审核未通过（review_status=" + nullToDash(episode.getReviewStatus())
                            + "，需 APPROVED）后才能发布到 Tube");
        }
    }

    public static boolean isValidAspectRatio(String aspectRatio) {
        String normalized = com.bgssai.shortai.production.support.AspectRatios.normalize(aspectRatio);
        return "9:16".equals(normalized)
                || "16:9".equals(normalized)
                || "1:1".equals(normalized)
                || "3:4".equals(normalized);
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
        ShortFilm film = findFilm(episodeId, filmId);
        if (film == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, absentFilmPublishMessage(episodeId, filmId));
        }
        return film;
    }

    /**
     * Lookup only. A missing row is null; preflight turns that into a publish gate
     * and does not append it to config warnings.
     */
    private ShortFilm findFilm(String episodeId, String filmId) {
        if ((episodeId == null || episodeId.isBlank()) && (filmId == null || filmId.isBlank())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_id or film_id required");
        }
        if (filmId != null && !filmId.isBlank()) {
            ShortFilmExample ex = new ShortFilmExample();
            ex.createCriteria().andFilmIdEqualTo(filmId.trim()).andDeletedFlagEqualTo(0);
            ShortFilm film = shortFilmMapper.selectOneByExample(ex);
            if (film == null) {
                return null;
            }
            if (episodeId != null && !episodeId.isBlank() && !episodeId.trim().equals(film.getEpisodeId())) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_id does not match film");
            }
            return film;
        }
        ShortFilmExample ex = new ShortFilmExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId.trim()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        return shortFilmMapper.selectOneByExample(ex);
    }

    /**
     * A composable episode has no film row until compose succeeds. That is not a
     * missing-record bug and must not be reported as "film not found".
     * A row is missing only when the episode already claims a finished asset.
     */
    private AbsentFilm describeAbsentFilm(String episodeId, String filmId) {
        boolean byFilmId = filmId != null && !filmId.isBlank();
        if (byFilmId && (episodeId == null || episodeId.isBlank())) {
            return new AbsentFilm("未找到该成片", true, null);
        }
        if (episodeId == null || episodeId.isBlank()) {
            return new AbsentFilm("未找到该成片", true, null);
        }
        ShortEpisode episode = workAccessService.requireEpisode(episodeId.trim());
        if (expectsPersistedFilm(episode)) {
            return new AbsentFilm("成片记录缺失，请重新合成后再发布", true, episode);
        }
        return new AbsentFilm("本集尚未合成，合成完成后再发布", false, episode);
    }

    static boolean expectsPersistedFilm(ShortEpisode episode) {
        if (episode == null) {
            return false;
        }
        if (episode.getFilmAssetId() != null && !episode.getFilmAssetId().isBlank()) {
            return true;
        }
        return "READY".equals(episode.getComposeStatus());
    }

    private static String absentFilmPublishMessage(String episodeId, String filmId) {
        boolean byFilmId = filmId != null && !filmId.isBlank();
        if (byFilmId) {
            return "未找到该成片";
        }
        if (episodeId == null || episodeId.isBlank()) {
            return "未找到该成片";
        }
        return "本集尚未合成，没有可发布的成片";
    }

    private record AbsentFilm(String reason, boolean recordMissing, ShortEpisode episode) {
    }

    static boolean mediaIdConflicts(ShortFilm film, MediaIngestClient.IngestResult looked) {
        if (film == null || looked == null) {
            return false;
        }
        String localId = film.getMediaId() == null ? "" : film.getMediaId().trim();
        String remoteId = looked.mediaId() == null ? "" : looked.mediaId().trim();
        return !localId.isEmpty() && !remoteId.isEmpty() && !localId.equals(remoteId);
    }

    static boolean isOpenHandoff(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
        return "PENDING".equals(normalized) || "UNKNOWN".equals(normalized)
                || "PROCESSING".equals(normalized) || "QUEUED".equals(normalized);
    }

    static boolean isIndeterminate(BizException ex) {
        if (ex == null) {
            return false;
        }
        if (CommonErrorCode.UPSTREAM_TIMEOUT.code().equals(ex.getCode())) {
            return true;
        }
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        return message.contains("结果未知") || message.contains("响应丢失");
    }

    private ApiResponse recoverOpenHandoff(ShortFilm film, String idempotencyKey) {
        MediaIngestClient.IngestResult looked;
        try {
            looked = mediaIngestClient.lookup(idempotencyKey);
        } catch (BizException ex) {
            if (!isIndeterminate(ex) && !CommonErrorCode.RULE_REJECT.code().equals(ex.getCode())) {
                persistFailure(film, idempotencyKey, null, 1, ex.getMessage());
                throw ex;
            }
            if (CommonErrorCode.RULE_REJECT.code().equals(ex.getCode())) {
                persistFailure(film, idempotencyKey, null, 1, ex.getMessage());
                throw ex;
            }
            Date at = new Date();
            persistUnknown(film, idempotencyKey, null, 1, ex.getMessage(), at);
            return Responses.ok(toUnknownResult(film, idempotencyKey, at, ex.getMessage()));
        }
        if (looked == null) {
            Date at = new Date();
            String note = "接收端状态未知，未再次发布";
            persistUnknown(film, idempotencyKey, null, 0, note, at);
            return Responses.ok(toUnknownResult(film, idempotencyKey, at, note));
        }
        if (looked.status() == null || looked.status().isBlank()) {
            Date at = new Date();
            String note = "接收端状态未知，未再次发布";
            persistUnknown(film, idempotencyKey, null, 0, note, at);
            return Responses.ok(toUnknownResult(film, idempotencyKey, at, note));
        }
        String raw = looked.status().trim().toUpperCase(java.util.Locale.ROOT);
        if ("NOT_FOUND".equals(raw)) {
            if (film.getMediaId() != null && !film.getMediaId().isBlank()) {
                Date at = new Date();
                String note = "已有作品号但接收端查无此键，未另建作品";
                persistUnknown(film, idempotencyKey, looked.httpStatus(), 0, note, at);
                return Responses.ok(toUnknownResult(film, idempotencyKey, at, note));
            }
            return null;
        }
        String classified = CommercialOutcomeGuard.classifyMediaStatus(raw);
        Date at = new Date();
        if (mediaIdConflicts(film, looked) && ("READY".equals(classified) || "PENDING".equals(classified))) {
            String note = "接收端作品号与已记录的不一致，未另建作品";
            persistUnknown(film, idempotencyKey, looked.httpStatus(), 0, note, at);
            return Responses.ok(toUnknownResult(film, idempotencyKey, at, note));
        }
        if ("READY".equals(classified)) {
            CommercialOutcomeGuard.rejectFakeUrl(looked.playUrl(), "tube play_url");
            if (looked.mediaId() == null || looked.mediaId().isBlank()
                    || looked.playUrl() == null || looked.playUrl().isBlank()) {
                persistUnknown(film, idempotencyKey, looked.httpStatus(), 0, "接收端已就绪但缺少作品号或播放链接", at);
                return Responses.ok(toUnknownResult(film, idempotencyKey, at, "接收端已就绪但缺少作品号或播放链接"));
            }
            MediaIngestClient.IngestResult adopted = new MediaIngestClient.IngestResult(
                    looked.mediaId(), looked.playUrl(), "READY", looked.httpStatus(), 0, looked.message());
            persistSuccess(film, adopted, idempotencyKey, at);
            Map<String, Object> result = toResult(film, adopted, idempotencyKey, at, true);
            result.put("recovered", true);
            return Responses.ok(result);
        }
        if ("FAILED".equals(classified)) {
            persistFailure(film, idempotencyKey, looked.httpStatus(), 0,
                    looked.message() == null || looked.message().isBlank() ? "Tube 返回失败" : looked.message());
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    looked.message() == null || looked.message().isBlank() ? "Tube 返回失败" : looked.message());
        }
        if ("PENDING".equals(classified)) {
            persistPending(film, new MediaIngestClient.IngestResult(
                    looked.mediaId(), looked.playUrl(), "PENDING", looked.httpStatus(), 0, looked.message()),
                    idempotencyKey, at);
            Map<String, Object> result = toResult(film, new MediaIngestClient.IngestResult(
                    looked.mediaId(), looked.playUrl(), "PENDING", looked.httpStatus(), 0, looked.message()),
                    idempotencyKey, at, true);
            result.put("recovered", true);
            return Responses.ok(result);
        }
        String note = looked.message() == null || looked.message().isBlank()
                ? "接收端结果未知，未再次发布" : looked.message();
        persistUnknown(film, idempotencyKey, looked.httpStatus(), 0, note, at);
        return Responses.ok(toUnknownResult(film, idempotencyKey, at, note));
    }

    private void persistUnknown(ShortFilm film, String idempotencyKey, Integer httpStatus, int attempts,
            String error, Date at) {
        String msg = error == null || error.isBlank() ? "发布结果未知" : error;
        if (msg.length() > 1000) {
            msg = msg.substring(0, 1000);
        }
        Map<String, Object> log = richPublishLog(idempotencyKey, film.getMediaId(), film.getMediaPlayUrl(),
                "UNKNOWN", httpStatus, msg, attempts, at);
        String logJson = ShortJson.toJson(log);
        ShortFilm patch = new ShortFilm();
        patch.setMediaPublishStatus("UNKNOWN");
        patch.setMediaLastError(msg);
        patch.setMediaPublishLog(logJson);
        patch.setUpdatedTime(at);
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);
        film.setMediaPublishStatus("UNKNOWN");
        film.setMediaLastError(msg);
        film.setMediaPublishLog(logJson);
    }

    private static Map<String, Object> toUnknownResult(ShortFilm film, String idempotencyKey, Date at, String note) {
        MediaIngestClient.IngestResult ingested = new MediaIngestClient.IngestResult(
                film.getMediaId(), film.getMediaPlayUrl(), "UNKNOWN", null, 0, note);
        Map<String, Object> result = toResult(film, ingested, idempotencyKey, at, false);
        result.put("commercial_ready", false);
        result.put("status", "UNKNOWN");
        return result;
    }

    private void persistPending(ShortFilm film, MediaIngestClient.IngestResult ingested,
            String idempotencyKey, Date publishedAt) {
        String note = ingested.message() == null || ingested.message().isBlank()
                ? "Tube 已受理，尚未可播放"
                : ingested.message();
        if (note.length() > 1000) {
            note = note.substring(0, 1000);
        }
        String playUrl = ingested.playUrl() == null ? "" : ingested.playUrl();
        Map<String, Object> log = richPublishLog(idempotencyKey, blankToNull(ingested.mediaId()),
                playUrl.isBlank() ? null : playUrl, "PENDING", ingested.httpStatus(), note,
                ingested.attempts(), publishedAt);
        String logJson = ShortJson.toJson(log);
        ShortFilm patch = new ShortFilm();
        if (ingested.mediaId() != null && !ingested.mediaId().isBlank()) {
            patch.setMediaId(ingested.mediaId());
        }
        if (!playUrl.isBlank()) {
            patch.setMediaPlayUrl(playUrl);
        }
        patch.setMediaPublishStatus("PENDING");
        patch.setMediaPublishedAt(publishedAt);
        patch.setMediaLastError(note);
        patch.setMediaPublishLog(logJson);
        patch.setUpdatedTime(publishedAt);
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);
        if (ingested.mediaId() != null && !ingested.mediaId().isBlank()) {
            film.setMediaId(ingested.mediaId());
        }
        if (!playUrl.isBlank()) {
            film.setMediaPlayUrl(playUrl);
        }
        film.setMediaPublishStatus("PENDING");
        film.setMediaPublishedAt(publishedAt);
        film.setMediaLastError(note);
        film.setMediaPublishLog(logJson);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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
        String msg = error == null ? "tube publish failed" : error;
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
            Object idempotencyKey, Date publishedAt, boolean replayed) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("film_id", film.getFilmId());
        result.put("episode_id", film.getEpisodeId());
        result.put("work_id", film.getWorkId());
        result.put("media_id", ingested.mediaId());
        result.put("play_url", ingested.playUrl());
        result.put("status", ingested.status());
        result.put("commercial_ready", CommercialOutcomeGuard.isReadyStatus(ingested.status()));
        result.put("idempotency_key", idempotencyKey);
        result.put("replayed", replayed);
        result.put("published_at", publishedAt);
        result.put("http_status", ingested.httpStatus());
        result.put("attempts", ingested.attempts());
        result.put("publish_log", buildPublishLog(film));
        return result;
    }

    private static Map<String, Object> toReplayResult(ShortFilm film, String idempotencyKey) {
        MediaIngestClient.IngestResult ingested = new MediaIngestClient.IngestResult(
                film.getMediaId(), film.getMediaPlayUrl(),
                CommercialOutcomeGuard.classifyMediaStatus(film.getMediaPublishStatus()),
                200, 0);
        Date publishedAt = film.getMediaPublishedAt() == null ? new Date() : film.getMediaPublishedAt();
        Map<String, Object> result = toResult(film, ingested, idempotencyKey, publishedAt, true);
        Map<String, Object> log = richPublishLog(idempotencyKey, ingested.mediaId(), ingested.playUrl(),
                ingested.status(), 200, "", 0, publishedAt, true);
        result.put("publish_log", log);
        return result;
    }
}
