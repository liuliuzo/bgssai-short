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
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortDubbing;
import com.bgssai.shortai.model.ShortDubbingExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmExportReq;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmRebakeReq;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmSettingsSaveReq;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmReq;
import com.bgssai.shortai.production.support.ComplianceLexicon;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class FilmService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private AssetService assetService;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortFilmMapper shortFilmMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortDubbingMapper shortDubbingMapper;
    @Autowired
    private ShortWorkMapper shortWorkMapper;

    public ApiResponse precheck(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        List<ShortShot> shots = shortShotMapper.selectByExample(ex);
        List<Map<String, Object>> unlocked = new ArrayList<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        for (ShortShot shot : shots) {
            if (!"LOCKED".equals(shot.getShotStatus()) && !"VIDEO_READY".equals(shot.getShotStatus())
                    && !"IMAGE_READY".equals(shot.getShotStatus())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("shot_id", shot.getShotId());
                item.put("shot_no", shot.getShotNo());
                item.put("shot_status", shot.getShotStatus());
                unlocked.add(item);
            }
            parts.add(ComplianceLexicon.scan(shot.getDialogueText(), shot.getSceneNo(), null, shot.getShotNo()));
        }
        Map<String, Object> scan = ComplianceLexicon.merge(parts);
        ShortDubbingExample dex = new ShortDubbingExample();
        dex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0)
                .andAlignStatusEqualTo("MISALIGNED");
        List<ShortDubbing> misaligned = shortDubbingMapper.selectByExample(dex);
        List<Map<String, Object>> blockers = new ArrayList<>();
        if (!misaligned.isEmpty()) {
            Map<String, Object> blocker = new LinkedHashMap<>();
            blocker.put("blocker_code", "DUBBING_MISALIGNED");
            blocker.put("blocker_desc", "存在未对齐配音，需处理后再合成");
            blocker.put("count", misaligned.size());
            blockers.add(blocker);
        }
        boolean ready = unlocked.isEmpty() && !"BLOCK".equals(scan.get("risk_level")) && blockers.isEmpty();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", ready);
        result.put("unlocked_shots", unlocked);
        result.put("risk_level", scan.get("risk_level"));
        result.put("hit_list", scan.get("hit_list"));
        result.put("blocker_list", blockers);
        result.put("misaligned_count", misaligned.size());
        return Responses.ok(result);
    }

    public ApiResponse timeline(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("shot_no asc");
        List<Map<String, Object>> clips = new ArrayList<>();
        List<Map<String, Object>> audioTrack = new ArrayList<>();
        List<Map<String, Object>> subtitleTrack = new ArrayList<>();
        int t = 0;
        for (ShortShot shot : shortShotMapper.selectByExample(ex)) {
            int dur = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
            Map<String, Object> clip = new LinkedHashMap<>();
            clip.put("shot_id", shot.getShotId());
            clip.put("shot_no", shot.getShotNo());
            clip.put("start_sec", t);
            clip.put("duration_sec", dur);
            clip.put("shot_desc", shot.getShotDesc());
            clip.put("first_frame_asset_id", shot.getFirstFrameAssetId());
            clip.put("video_asset_id", shot.getVideoAssetId());
            clips.add(clip);
            ShortDubbingExample dex = new ShortDubbingExample();
            dex.createCriteria().andShotIdEqualTo(shot.getShotId()).andDeletedFlagEqualTo(0);
            for (ShortDubbing d : shortDubbingMapper.selectByExample(dex)) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("shot_id", shot.getShotId());
                a.put("dubbing_id", d.getDubbingId());
                a.put("start_sec", t);
                a.put("duration_sec", d.getAudioDurationSec());
                a.put("audio_asset_id", d.getAudioAssetId());
                a.put("align_status", d.getAlignStatus());
                audioTrack.add(a);
            }
            t += dur;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clip_list", clips);
        result.put("audio_track", audioTrack);
        result.put("subtitle_track", subtitleTrack);
        result.put("total_sec", t);
        return Responses.ok(result);
    }

    public ApiResponse compose(FilmReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        Map<String, Object> check = precheckMap(ep.getEpisodeId());
        if (!Boolean.TRUE.equals(check.get("ready"))) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "precheck not ready");
        }
        patchCompose(ep, "COMPOSING");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("episode_id", ep.getEpisodeId());
        request.put("compose_mode", "FULL");
        ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "COMPOSE", "COMPOSE",
                ep.getEpisodeId(), request, null, 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    public ApiResponse rebakeSubtitle(FilmRebakeReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        if (!"READY".equals(ep.getComposeStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "film not ready for subtitle rebake");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("episode_id", ep.getEpisodeId());
        request.put("compose_mode", "SUBTITLE_ONLY");
        ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "COMPOSE", "COMPOSE",
                ep.getEpisodeId(), request, null, 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        result.put("compose_mode", "SUBTITLE_ONLY");
        return Responses.ok(result);
    }

    public ApiResponse settingsSave(FilmSettingsSaveReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        Map<String, Object> setting = req.getComposeSetting() == null ? new LinkedHashMap<>()
                : new LinkedHashMap<>(req.getComposeSetting());
        if (req.getBurnSubtitleFlag() != null) {
            setting.put("burn_subtitle_flag", req.getBurnSubtitleFlag());
        }
        if (req.getAspectRatio() != null) {
            setting.put("aspect_ratio", req.getAspectRatio());
        }
        ShortWork patch = new ShortWork();
        patch.setComposeSettingJson(ShortJson.toJson(setting));
        if (req.getAspectRatio() != null) {
            patch.setAspectRatio(req.getAspectRatio());
        }
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("compose_setting", setting);
        return Responses.ok(result);
    }

    public ApiResponse export(FilmExportReq req) {
        if (req == null || req.getEpisodeIdList() == null || req.getEpisodeIdList().isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_id_list required");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        List<String> taskIds = new ArrayList<>();
        for (String episodeId : req.getEpisodeIdList()) {
            ShortEpisode ep = workAccessService.requireEpisode(episodeId);
            ShortFilmExample fx = new ShortFilmExample();
            fx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
            ShortFilm film = shortFilmMapper.selectOneByExample(fx);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("episode_id", ep.getEpisodeId());
            if (film != null && film.getFilmAssetId() != null) {
                ApiResponse urlResp = assetService.getDownload(film.getFilmAssetId());
                @SuppressWarnings("unchecked")
                Map<String, Object> url = (Map<String, Object>) urlResp.getResult();
                item.put("film_asset_id", film.getFilmAssetId());
                item.put("download_url", url.get("download_url"));
                item.put("expire_at", url.get("expire_at"));
            } else {
                Map<String, Object> request = new LinkedHashMap<>();
                request.put("episode_id", ep.getEpisodeId());
                request.put("compose_mode", "EXPORT");
                ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "COMPOSE",
                        "COMPOSE", ep.getEpisodeId(), request, null, 0);
                item.put("task_id", task.getTaskId());
                taskIds.add(task.getTaskId());
            }
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("export_list", items);
        result.put("task_id_list", taskIds);
        return Responses.ok(result);
    }

    public ApiResponse detail(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortFilmExample ex = new ShortFilmExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ShortFilm film = shortFilmMapper.selectOneByExample(ex);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("compose_status", ep.getComposeStatus());
        if (film != null) {
            result.put("film_id", film.getFilmId());
            result.put("film_asset_id", film.getFilmAssetId());
            result.put("film_duration_sec", film.getFilmDurationSec());
            result.put("download_url", film.getFilmAssetId() == null ? null
                    : "/bgssai/user/films/download-url?asset_id=" + film.getFilmAssetId());
        }
        return Responses.ok(result);
    }

    public ApiResponse downloadUrl(String assetId) {
        return assetService.getDownload(assetId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> precheckMap(String episodeId) {
        return (Map<String, Object>) precheck(episodeId).getResult();
    }

    private void patchCompose(ShortEpisode ep, String status) {
        ShortEpisode patch = new ShortEpisode();
        patch.setComposeStatus(status);
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
    }
}
