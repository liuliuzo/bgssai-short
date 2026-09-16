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
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortSubtitleMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortDubbing;
import com.bgssai.shortai.model.ShortDubbingExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortSubtitle;
import com.bgssai.shortai.model.ShortSubtitleExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmExportReq;
import com.bgssai.shortai.production.dto.FeatureDtos.BgmGenerateReq;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmRebakeReq;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmSettingsSaveReq;
import com.bgssai.shortai.production.dto.ProductionDtos.AiTaskReq;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmReq;
import com.bgssai.shortai.production.support.ComplianceLexicon;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.production.support.SubtitleSrtBuilder;

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
    @Autowired
    private ShortAssetMapper shortAssetMapper;
    @Autowired
    private ShortSubtitleMapper shortSubtitleMapper;
    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private AiTaskService aiTaskService;

    public ApiResponse precheck(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        List<ShortShot> shots = shortShotMapper.selectByExample(ex);
        List<Map<String, Object>> unlocked = new ArrayList<>();
        List<Map<String, Object>> missingVideo = new ArrayList<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        for (ShortShot shot : shots) {
            boolean statusOk = "LOCKED".equals(shot.getShotStatus()) || "VIDEO_READY".equals(shot.getShotStatus());
            boolean hasVideo = shot.getVideoAssetId() != null && !shot.getVideoAssetId().isBlank();
            if (!statusOk) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("shot_id", shot.getShotId());
                item.put("shot_no", shot.getShotNo());
                item.put("shot_status", shot.getShotStatus());
                unlocked.add(item);
            }
            if (!hasVideo) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("shot_id", shot.getShotId());
                item.put("shot_no", shot.getShotNo());
                item.put("shot_status", shot.getShotStatus());
                missingVideo.add(item);
            }
            parts.add(ComplianceLexicon.scan(shot.getDialogueText(), shot.getSceneNo(), null, shot.getShotNo()));
        }
        Map<String, Object> scan = ComplianceLexicon.merge(parts);
        ShortDubbingExample dex = new ShortDubbingExample();
        dex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0)
                .andAlignStatusEqualTo("MISALIGNED");
        List<ShortDubbing> misaligned = shortDubbingMapper.selectByExample(dex);
        List<Map<String, Object>> blockers = new ArrayList<>();
        if (!unlocked.isEmpty()) {
            Map<String, Object> blocker = new LinkedHashMap<>();
            blocker.put("blocker_code", "SHOT_NOT_READY");
            blocker.put("blocker_desc", "存在未完成视频的镜头，需全部 VIDEO_READY 或 LOCKED 后再合成");
            blocker.put("count", unlocked.size());
            blockers.add(blocker);
        }
        if (!missingVideo.isEmpty()) {
            Map<String, Object> blocker = new LinkedHashMap<>();
            blocker.put("blocker_code", "SHOT_MISSING_VIDEO");
            blocker.put("blocker_desc", "以下镜头缺少可用视频素材");
            blocker.put("count", missingVideo.size());
            blockers.add(blocker);
        }
        if (!misaligned.isEmpty()) {
            Map<String, Object> blocker = new LinkedHashMap<>();
            blocker.put("blocker_code", "DUBBING_MISALIGNED");
            blocker.put("blocker_desc", "存在未对齐配音，需处理后再合成");
            blocker.put("count", misaligned.size());
            blockers.add(blocker);
        }
        boolean ready = unlocked.isEmpty() && missingVideo.isEmpty()
                && !"BLOCK".equals(scan.get("risk_level")) && misaligned.isEmpty();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", ready);
        result.put("unlocked_shots", unlocked);
        result.put("missing_video_shots", missingVideo);
        result.put("risk_level", scan.get("risk_level"));
        result.put("hit_list", scan.get("hit_list"));
        result.put("blocker_list", blockers);
        result.put("misaligned_count", misaligned.size());
        result.put("shot_count", shots.size());
        // SHORT-01 联调验收要求真实模型至少 3 镜；不足时仅提示，不阻断合成入口。
        result.put("acceptance_min_shot_count", 3);
        result.put("acceptance_shot_count_ok", shots.size() >= 3);
        return Responses.ok(result);
    }

    public ApiResponse timeline(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("shot_no asc");
        List<ShortShot> shots = shortShotMapper.selectByExample(ex);
        List<Map<String, Object>> clips = new ArrayList<>();
        List<Map<String, Object>> audioTrack = new ArrayList<>();
        int t = 0;
        for (ShortShot shot : shots) {
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
        List<Map<String, Object>> subtitleTrack = loadSubtitleTrack(ep, shots);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clip_list", clips);
        result.put("audio_track", audioTrack);
        result.put("subtitle_track", subtitleTrack);
        ShortWork work = workAccessService.requireWork(ep.getWorkId());
        Map<String, Object> composeSetting = ShortJson.map(work.getComposeSettingJson());
        String bgmAssetId = ShortJson.text(composeSetting.get("bgm_asset_id"));
        result.put("bgm_asset_id", bgmAssetId.isBlank() ? null : bgmAssetId);
        result.put("bgm_url", bgmAssetId.isBlank() ? null : assetService.resolveUrl(bgmAssetId));
        result.put("total_sec", t);
        return Responses.ok(result);
    }

    public ApiResponse estimateCompose(FilmReq req) {
        AiTaskReq estimateReq = new AiTaskReq();
        estimateReq.setEpisodeId(req == null ? null : req.getEpisodeId());
        estimateReq.setTaskType("COMPOSE");
        return aiTaskService.estimate(estimateReq);
    }

    public ApiResponse compose(FilmReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        Map<String, Object> check = precheckMap(ep.getEpisodeId());
        if (!Boolean.TRUE.equals(check.get("ready"))) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "precheck not ready");
        }
        AiTaskReq estimateReq = new AiTaskReq();
        estimateReq.setEpisodeId(ep.getEpisodeId());
        estimateReq.setTaskType("COMPOSE");
        @SuppressWarnings("unchecked")
        Map<String, Object> estimate = (Map<String, Object>) aiTaskService.estimate(estimateReq).getResult();
        int durationSec = ShortJson.integer(estimate.get("duration_sec"), 1);
        patchCompose(ep, "COMPOSING");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("episode_id", ep.getEpisodeId());
        request.put("compose_mode", "FULL");
        request.put("duration_sec", durationSec);
        ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "COMPOSE", "COMPOSE",
                ep.getEpisodeId(), request, null, 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        result.put("estimated_cost_cent", estimate.get("estimated_cost_cent"));
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
        Map<String, Object> setting = new LinkedHashMap<>(ShortJson.map(work.getComposeSettingJson()));
        if (req.getComposeSetting() != null) {
            setting.putAll(req.getComposeSetting());
        }
        if (req.getBurnSubtitleFlag() != null) {
            setting.put("burn_subtitle_flag", req.getBurnSubtitleFlag());
        }
        if (req.getAspectRatio() != null) {
            setting.put("aspect_ratio", req.getAspectRatio());
        }
        String bgmAssetId = ShortJson.text(setting.get("bgm_asset_id"));
        if (!bgmAssetId.isBlank()) {
            validateBgmAsset(work, req.getEpisodeId(), bgmAssetId);
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

    public ApiResponse generateBgm(BgmGenerateReq req) {
        if (req == null || req.getPrompt() == null || req.getPrompt().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "prompt required");
        }
        ShortWork work = workAccessService.requireWork(req.getWorkId());
        ShortEpisode ep = workAccessService.requireEpisode(req.getEpisodeId());
        if (!work.getWorkId().equals(ep.getWorkId())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "episode does not belong to work");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("prompt", req.getPrompt().trim());
        request.put("lyrics", req.getLyrics());
        request.put("is_instrumental", req.getIsInstrumental() == null ? 1 : req.getIsInstrumental());
        request.put("model_code", req.getModelCode());
        ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "MUSIC", "BGM",
                ep.getEpisodeId(), request, null, 0, req.getModelCode());
        return Responses.ok(Map.of("task_id", task.getTaskId()));
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
        ShortWork work = workAccessService.requireWork(ep.getWorkId());
        ShortFilmExample ex = new ShortFilmExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ShortFilm film = shortFilmMapper.selectOneByExample(ex);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("compose_status", ep.getComposeStatus());
        Map<String, Object> composeSetting = ShortJson.map(work.getComposeSettingJson());
        String selectedBgm = ShortJson.text(composeSetting.get("bgm_asset_id"));
        result.put("bgm_asset_id", selectedBgm.isBlank() ? null : selectedBgm);
        String bgmUrl = selectedBgm.isBlank() ? null : assetService.resolveUrl(selectedBgm);
        result.put("bgm_url", CommercialOutcomeGuard.safeCommercialUrl(bgmUrl));
        result.put("bgm_candidate_list", bgmCandidates(ep, selectedBgm));
        if (film != null) {
            result.put("film_id", film.getFilmId());
            result.put("film_asset_id", film.getFilmAssetId());
            result.put("film_duration_sec", film.getFilmDurationSec());
            result.put("compose_task_id", film.getComposeTaskId());
            result.put("composed_time", film.getComposedTime());
            // Return a playable/presigned URL — the download-url JSON endpoint is not a media src.
            // Placeholder / stub schemes must never surface as commercial playable URLs.
            String downloadUrl = film.getFilmAssetId() == null ? null
                    : assetService.resolveUrl(film.getFilmAssetId());
            result.put("download_url", CommercialOutcomeGuard.safeCommercialUrl(downloadUrl));
            result.put("media_id", film.getMediaId());
            result.put("media_play_url", CommercialOutcomeGuard.safeCommercialUrl(film.getMediaPlayUrl()));
            result.put("media_publish_status", film.getMediaPublishStatus());
            result.put("media_published_at", film.getMediaPublishedAt());
            result.put("media_last_error", film.getMediaLastError());
            Map<String, Object> evidence = filmEvidence(film);
            result.put("production_evidence", evidence);
            result.put("commercial_ready", Boolean.TRUE.equals(evidence.get("commercial_ready")));
            Map<String, Object> publishLog = FilmPublishToMediaService.buildPublishLog(film);
            if (publishLog != null) {
                result.put("publish_log", publishLog);
            }
            if (film.getMediaPublishLog() != null && !film.getMediaPublishLog().isBlank()) {
                result.put("media_publish_log", film.getMediaPublishLog());
            }
        }
        return Responses.ok(result);
    }

    /**
     * SHORT-01：成片必须能回溯到合成任务、模型、耗用与对象存储键；占位 URL 不得标 commercial_ready。
     */
    private Map<String, Object> filmEvidence(ShortFilm film) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("compose_task_id", film.getComposeTaskId());
        evidence.put("film_asset_id", film.getFilmAssetId());
        evidence.put("compose_status", film.getComposeStatus());
        ShortAsset asset = null;
        if (film.getFilmAssetId() != null && !film.getFilmAssetId().isBlank()) {
            ShortAssetExample ax = new ShortAssetExample();
            ax.createCriteria().andAssetIdEqualTo(film.getFilmAssetId()).andDeletedFlagEqualTo(0);
            asset = shortAssetMapper.selectOneByExample(ax);
        }
        if (asset != null) {
            evidence.put("source_task_id", asset.getSourceTaskId());
            evidence.put("storage_key", asset.getStorageKey());
            evidence.put("file_size_byte", asset.getFileSizeByte());
            evidence.put("mime_type", asset.getMimeType());
            evidence.put("duration_sec", asset.getDurationSec());
        }
        ShortAiTask task = null;
        String taskId = film.getComposeTaskId();
        if ((taskId == null || taskId.isBlank()) && asset != null) {
            taskId = asset.getSourceTaskId();
        }
        if (taskId != null && !taskId.isBlank()) {
            ShortAiTaskExample tx = new ShortAiTaskExample();
            tx.createCriteria().andTaskIdEqualTo(taskId);
            task = shortAiTaskMapper.selectOneByExample(tx);
        }
        if (task != null) {
            evidence.put("model_code", task.getModelCode());
            evidence.put("cost_cent", task.getCostCent());
            evidence.put("task_status", task.getTaskStatus());
            evidence.put("task_type", task.getTaskType());
        }
        boolean storageOk = asset != null && asset.getStorageKey() != null && !asset.getStorageKey().isBlank()
                && !asset.getStorageKey().startsWith("<svg")
                && asset.getFileSizeByte() != null && asset.getFileSizeByte() > 0;
        boolean taskOk = film.getComposeTaskId() != null && !film.getComposeTaskId().isBlank();
        boolean statusOk = "READY".equals(film.getComposeStatus());
        // 成片商业就绪只看合成产物与证据链；Media play_url 另行校验，占位不得冒充已发布。
        evidence.put("media_play_url_ok", film.getMediaPlayUrl() == null || film.getMediaPlayUrl().isBlank()
                || CommercialOutcomeGuard.safeCommercialUrl(film.getMediaPlayUrl()) != null);
        evidence.put("commercial_ready", statusOk && storageOk && taskOk);
        return evidence;
    }

    private List<Map<String, Object>> loadSubtitleTrack(ShortEpisode ep, List<ShortShot> shots) {
        ShortSubtitleExample sx = new ShortSubtitleExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("subtitle_no asc");
        List<ShortSubtitle> rows = shortSubtitleMapper.selectByExample(sx);
        return SubtitleSrtBuilder.cuesForEpisode(rows, shots);
    }

    private List<Map<String, Object>> bgmCandidates(ShortEpisode ep, String selectedBgm) {
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andAssetTypeEqualTo("AUDIO")
                .andDeletedFlagEqualTo(0);
        ex.setOrderByClause("created_time desc, id desc");
        List<Map<String, Object>> result = new ArrayList<>();
        for (ShortAsset asset : shortAssetMapper.selectByExample(ex)) {
            if (asset.getShotId() != null && !asset.getShotId().isBlank()) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("asset_id", asset.getAssetId());
            item.put("file_url",
                    CommercialOutcomeGuard.safeCommercialUrl(assetService.resolveUrl(asset.getAssetId())));
            item.put("duration_sec", asset.getDurationSec());
            item.put("created_time", asset.getCreatedTime());
            item.put("selected_flag", asset.getAssetId().equals(selectedBgm) ? 1 : 0);
            result.add(item);
        }
        return result;
    }

    private void validateBgmAsset(ShortWork work, String episodeId, String assetId) {
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andAssetIdEqualTo(assetId).andUserIdEqualTo(work.getUserId())
                .andWorkIdEqualTo(work.getWorkId()).andAssetTypeEqualTo("AUDIO").andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ex);
        if (asset == null || (asset.getShotId() != null && !asset.getShotId().isBlank())
                || (episodeId != null && !episodeId.isBlank() && !episodeId.equals(asset.getEpisodeId()))) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "BGM asset not found in this episode");
        }
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
