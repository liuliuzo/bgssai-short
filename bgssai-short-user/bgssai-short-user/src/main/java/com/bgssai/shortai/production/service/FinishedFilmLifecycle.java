package com.bgssai.shortai.production.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.CommercialOutcomeGuard;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;
import com.bgssai.shortai.production.media.MediaIngestContract;
import com.bgssai.shortai.production.support.ShortJson;

/**
 * Finished-film state after generate / compose.
 * <p>
 * Shot video and dubbing changes expire the previous cut so publish cannot replay an old Tube
 * catalog row. A successful compose clears that expiry and drops the short-side READY replay.
 * A failed compose must not leave the episode stuck in COMPOSING.
 */
@Service
public class FinishedFilmLifecycle {

    static final String SUPERSEDED =
            "成片已重新合成，上次 Tube 目录不再回放；下次发布用同一幂等键更新素材";

    @Autowired
    private ShortFilmMapper shortFilmMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private WorkStatusService workStatusService;

    /**
     * Full-level upstream change (new shot video or new dubbing). No-op when this episode has
     * never produced a film asset.
     */
    public void expireAfterUpstreamChange(String episodeId) {
        if (episodeId == null || episodeId.isBlank()) {
            return;
        }
        ShortEpisode episode = episode(episodeId);
        ShortFilm film = latestFilm(episodeId);
        if (film == null || episode == null) {
            return;
        }
        boolean hasAsset = film.getFilmAssetId() != null && !film.getFilmAssetId().isBlank();
        boolean active = "READY".equals(film.getComposeStatus())
                || "COMPOSING".equals(film.getComposeStatus())
                || "READY".equals(episode.getComposeStatus())
                || "COMPOSING".equals(episode.getComposeStatus());
        if (!hasAsset && !active) {
            return;
        }
        Date now = new Date();
        ShortFilm patch = new ShortFilm();
        patch.setComposeStatus("NONE");
        patch.setExpiredFlag(1);
        patch.setUpdatedTime(now);
        if (replayable(film)) {
            patch.setMediaPublishStatus("NONE");
            patch.setMediaPlayUrl("");
            patch.setMediaLastError("成片已因上游画面或配音变更过期，需重新合成后再发布");
            patch.setMediaPublishLog(ShortJson.toJson(supersedeLog(film, patch.getMediaLastError(), now)));
        }
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);

        ShortEpisode epPatch = new ShortEpisode();
        epPatch.setComposeStatus("NONE");
        if ("APPROVED".equals(episode.getReviewStatus()) || "PENDING".equals(episode.getReviewStatus())) {
            epPatch.setReviewStatus("NOT_SUBMITTED");
        }
        epPatch.setUpdatedTime(now);
        ShortEpisodeExample epUp = new ShortEpisodeExample();
        epUp.createCriteria().andIdEqualTo(episode.getId());
        shortEpisodeMapper.updateByExampleSelective(epPatch, epUp);
        workStatusService.refresh(episode.getWorkId());
    }

    /**
     * A new mp4 replaced the previous cut. Clear expiry and stop replaying the old Tube row.
     */
    public void applyFreshCompose(ShortFilm patch, ShortFilm previous) {
        patch.setExpiredFlag(0);
        patch.setSubtitleStaleFlag(0);
        if (previous == null) {
            return;
        }
        if (!replayable(previous) && (previous.getMediaId() == null || previous.getMediaId().isBlank())) {
            return;
        }
        if (previous.getMediaPublishStatus() == null || previous.getMediaPublishStatus().isBlank()
                || "NONE".equals(previous.getMediaPublishStatus())) {
            if (previous.getMediaId() == null || previous.getMediaId().isBlank()) {
                return;
            }
        }
        patch.setMediaPublishStatus("NONE");
        patch.setMediaPlayUrl("");
        patch.setMediaLastError("");
        patch.setMediaPublishLog(ShortJson.toJson(supersedeLog(previous, SUPERSEDED, new Date())));
    }

    /**
     * Compose task failed. If a previous unexpired READY cut is still on the film row, restore
     * the episode to READY. Otherwise record FAILED instead of leaving COMPOSING.
     */
    public void onComposeTaskFailed(String episodeId) {
        if (episodeId == null || episodeId.isBlank()) {
            return;
        }
        ShortEpisode episode = episode(episodeId);
        if (episode == null || !"COMPOSING".equals(episode.getComposeStatus())) {
            return;
        }
        ShortFilm film = latestFilm(episodeId);
        Date now = new Date();
        boolean previousCutStillGood = film != null
                && "READY".equals(film.getComposeStatus())
                && (film.getExpiredFlag() == null || film.getExpiredFlag() == 0);
        ShortEpisode epPatch = new ShortEpisode();
        epPatch.setComposeStatus(previousCutStillGood ? "READY" : "FAILED");
        epPatch.setUpdatedTime(now);
        ShortEpisodeExample epUp = new ShortEpisodeExample();
        epUp.createCriteria().andIdEqualTo(episode.getId());
        shortEpisodeMapper.updateByExampleSelective(epPatch, epUp);
        if (!previousCutStillGood && film != null && !"READY".equals(film.getComposeStatus())) {
            ShortFilm patch = new ShortFilm();
            patch.setComposeStatus("FAILED");
            patch.setUpdatedTime(now);
            ShortFilmExample up = new ShortFilmExample();
            up.createCriteria().andIdEqualTo(film.getId());
            shortFilmMapper.updateByExampleSelective(patch, up);
        }
        workStatusService.refresh(episode.getWorkId());
    }

    private static boolean replayable(ShortFilm film) {
        if (film == null) {
            return false;
        }
        return CommercialOutcomeGuard.isReadyStatus(film.getMediaPublishStatus())
                || CommercialOutcomeGuard.isPendingStatus(film.getMediaPublishStatus());
    }

    private static Map<String, Object> supersedeLog(ShortFilm film, String error, Date at) {
        Map<String, Object> log = new LinkedHashMap<>();
        String key = MediaIngestContract.idempotencyKey(film.getWorkId(), film.getEpisodeId(), film.getFilmId());
        log.put("idempotency_key", key);
        log.put("media_id", film.getMediaId());
        log.put("play_url", "");
        log.put("status", "NONE");
        log.put("http_status", null);
        log.put("error", error);
        log.put("attempts", 0);
        log.put("published_at", at);
        log.put("replayed", false);
        log.put("superseded", true);
        return log;
    }

    private ShortFilm latestFilm(String episodeId) {
        ShortFilmExample ex = new ShortFilmExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        return shortFilmMapper.selectOneByExample(ex);
    }

    private ShortEpisode episode(String episodeId) {
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        return shortEpisodeMapper.selectOneByExample(ex);
    }
}
