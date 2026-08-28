package com.bgssai.shortai.production.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.AiProviderRequest;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.aiprovider.FfmpegComposeProvider;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.AiUsageRecordMapper;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiUsageRecord;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortDubbing;
import com.bgssai.shortai.model.ShortDubbingExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.storage.ObjectStorage;

@Component
public class ShortAiTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(ShortAiTaskWorker.class);

    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortSceneCardMapper shortSceneCardMapper;
    @Autowired
    private ShortAssetMapper shortAssetMapper;
    @Autowired
    private ShortFilmMapper shortFilmMapper;
    @Autowired
    private ShortDubbingMapper shortDubbingMapper;
    @Autowired
    private AiUsageRecordMapper aiUsageRecordMapper;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private WorkStatusService workStatusService;
    @Autowired
    private DubbingService dubbingService;
    @Autowired
    private QuotaConsumeService quotaConsumeService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private AiModelResolveService aiModelResolveService;
    @Autowired
    private AiProviderRegistry aiProviderRegistry;
    @Autowired
    private ObjectStorage objectStorage;
    @Autowired
    private Environment environment;
    @Autowired
    @Qualifier("aiTaskExecutor")
    private TaskExecutor aiTaskExecutor;

    /** 判定 RUNNING 卡死的宽限秒数：模型 timeout_sec 之上再留这么久仍无结果就判超时。 */
    private static final int STALE_GRACE_SEC = 120;

    /** 模型没配 timeout_sec 时的兜底上限。 */
    private static final int DEFAULT_TIMEOUT_SEC = 600;

    @Scheduled(fixedDelay = 800)
    public void tick() {
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTaskStatusEqualTo("QUEUED");
        ex.setOrderByClause("id asc");
        ShortAiTask task = shortAiTaskMapper.selectOneByExample(ex);
        if (task == null) {
            return;
        }
        ShortAiTask running = new ShortAiTask();
        running.setTaskStatus("RUNNING");
        running.setStartedTime(new Date());
        running.setProgressPercent(30);
        running.setUpdatedTime(new Date());
        ShortAiTaskExample cas = new ShortAiTaskExample();
        cas.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("QUEUED");
        int n = shortAiTaskMapper.updateByExampleSelective(running, cas);
        if (n != 1) {
            return;
        }
        try {
            aiTaskExecutor.execute(() -> run(task));
        } catch (RejectedExecutionException rejected) {
            // 池子满了就把任务退回排队，交给下一轮 tick，不在内存里堆积。
            requeue(task);
        }
    }

    /**
     * 回收卡死的任务。上游调用即便挂住，任务也不会永远停在 RUNNING：
     * 超过「模型 timeout_sec + 宽限」仍未结束的行会被判 MODEL_TIMEOUT。
     * 进程重启后遗留的 RUNNING 行同样由这里兜底。
     */
    @Scheduled(fixedDelay = 30_000)
    public void reapStaleTasks() {
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTaskStatusEqualTo("RUNNING");
        for (ShortAiTask task : shortAiTaskMapper.selectByExample(ex)) {
            Date started = task.getStartedTime() == null ? task.getQueuedTime() : task.getStartedTime();
            if (started == null) {
                continue;
            }
            int limit = timeoutSecOf(task) + STALE_GRACE_SEC;
            long ageSec = (System.currentTimeMillis() - started.getTime()) / 1000L;
            if (ageSec < limit) {
                continue;
            }
            log.warn("ai task stale taskId={} type={} model={} ageSec={} limitSec={}",
                    task.getTaskId(), task.getTaskType(), task.getModelCode(), ageSec, limit);
            failIfRunning(task, "MODEL_TIMEOUT",
                    "任务超过 " + limit + " 秒仍未返回，已判定超时（上游未响应或对象存储不可达）");
        }
    }

    private int timeoutSecOf(ShortAiTask task) {
        try {
            AiModelConfig model = aiModelResolveService.resolve(task.getTaskType(), task.getModelCode());
            Integer t = model.getTimeoutSec();
            return t == null || t <= 0 ? DEFAULT_TIMEOUT_SEC : t;
        } catch (RuntimeException ex) {
            // 模型行被停用 / 删除时仍要能回收，走兜底上限。
            return DEFAULT_TIMEOUT_SEC;
        }
    }

    private void run(ShortAiTask task) {
        try {
            AiModelConfig model = aiModelResolveService.resolve(task.getTaskType(), task.getModelCode());
            complete(task, model);
            ShortAiTask ok = new ShortAiTask();
            ok.setTaskStatus("SUCCEEDED");
            ok.setProgressPercent(100);
            ok.setFinishedTime(new Date());
            ok.setUpdatedTime(new Date());
            ShortAiTaskExample up = new ShortAiTaskExample();
            up.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("RUNNING");
            if (shortAiTaskMapper.updateByExampleSelective(ok, up) != 1) {
                // 已被回收器判超时或被用户取消：不再记账，避免为作废的任务扣费。
                log.warn("ai task finished after reap/cancel, skip billing taskId={}", task.getTaskId());
                return;
            }
            insertUsage(task, model, 1);
            quotaConsumeService.consume(task.getUserId(), task.getCostCent() == null ? 0 : task.getCostCent());
            addSpent(task);
        } catch (BizException exBiz) {
            failIfRunning(task, exBiz.getCode(), exBiz.getMessage());
        } catch (Exception exAll) {
            log.warn("ai task failed taskId={}", task.getTaskId(), exAll);
            failIfRunning(task, "MODEL_TIMEOUT", exAll.getMessage());
        }
    }

    private void requeue(ShortAiTask task) {
        ShortAiTask back = new ShortAiTask();
        back.setTaskStatus("QUEUED");
        back.setProgressPercent(0);
        back.setUpdatedTime(new Date());
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("RUNNING");
        shortAiTaskMapper.updateByExampleSelective(back, up);
    }

    private void complete(ShortAiTask task, AiModelConfig model) {
        Map<String, Object> req = ShortJson.map(task.getRequestJson());
        if ("OUTLINE".equals(task.getTargetType())) {
            finishOutline(task, model);
            return;
        }
        if ("SCRIPT".equals(task.getTargetType())) {
            finishScript(task, req, model);
            return;
        }
        if ("REWRITE".equals(task.getTargetType())) {
            finishRewrite(task, req, model);
            return;
        }
        if ("PORTRAIT".equals(task.getTargetType())) {
            finishPortrait(task, req, model);
            return;
        }
        if ("SCENE_PORTRAIT".equals(task.getTargetType())) {
            finishScenePortrait(task, req, model);
            return;
        }
        if ("SHOT".equals(task.getTargetType())) {
            finishShot(task, model);
            return;
        }
        if ("COMPOSE".equals(task.getTargetType())) {
            finishCompose(task, model);
            return;
        }
        if ("DUBBING".equals(task.getTargetType())) {
            finishDubbing(task, model);
        }
    }

    private void finishDubbing(ShortAiTask task, AiModelConfig model) {
        ShortDubbingExample ex = new ShortDubbingExample();
        ex.createCriteria().andDubbingIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortDubbing row = shortDubbingMapper.selectOneByExample(ex);
        if (row == null) {
            return;
        }
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andShotIdEqualTo(row.getShotId()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(sx);
        if (shot == null) {
            return;
        }
        AiProviderRequest request = baseRequest(model, "VOICE", "DUBBING");
        request.getParams().put("title", row.getLineText());
        request.getParams().put("voice_id", row.getVoiceId());
        request.getParams().put("emotion", row.getEmotion());
        request.setPrompt(row.getLineText());
        AiProviderResult result = execute(model, request);
        byte[] bytes = result.getBinaryContent();
        String mime = result.getMimeType();
        if (bytes == null || bytes.length == 0) {
            String placeholder = "VOICE:" + (row.getLineText() == null ? "" : row.getLineText());
            bytes = placeholder.getBytes(StandardCharsets.UTF_8);
            mime = "audio/mpeg";
        }
        if (mime == null || mime.isBlank()) {
            mime = "audio/mpeg";
        }
        AiProviderResult packed = AiProviderResult.binary(bytes, mime);
        ShortAsset asset = saveBinary(row.getUserId(), row.getWorkId(), row.getEpisodeId(), row.getShotId(), "AUDIO",
                packed, task.getTaskId(), true);
        int approx = Math.max(1, (row.getLineText() == null ? 4 : row.getLineText().length()) / 4);
        if (shot.getShotDurationSec() != null && approx > shot.getShotDurationSec() * 2) {
            approx = shot.getShotDurationSec() + 1;
        }
        asset.setDurationSec(approx);
        shortAssetMapper.updateByPrimaryKeySelective(asset);
        dubbingService.finishDubbingAlign(row, shot, asset.getAssetId(), approx);
    }

    private void finishOutline(ShortAiTask task, AiModelConfig model) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(task.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        if (work == null) {
            return;
        }
        AiProviderRequest request = baseRequest(model, "TEXT", "OUTLINE");
        Map<String, Object> params = request.getParams();
        params.put("title", work.getWorkTitle());
        params.put("pitch", work.getOneLinePitch());
        params.put("episode_count", work.getEpisodeCount() == null ? 24 : work.getEpisodeCount());
        params.put("genre", work.getGenre());
        request.setPrompt("Generate outline for " + work.getWorkTitle());
        AiProviderResult result = execute(model, request);
        List<Map<String, Object>> outline = ShortJson.list(result.getTextContent());
        ShortWork patch = new ShortWork();
        patch.setOutlineJson(ShortJson.toJson(outline));
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        scriptService.applyOutline(work.getWorkId(), outline);
        workStatusService.refresh(work.getWorkId());
    }

    private void finishScript(ShortAiTask task, Map<String, Object> req, AiModelConfig model) {
        ShortEpisode ep = episode(task.getEpisodeId());
        if (ep == null) {
            return;
        }
        AiProviderRequest request = baseRequest(model, "TEXT", "SCRIPT");
        request.getParams().put("episode_title", ep.getEpisodeTitle());
        request.getParams().put("synopsis", ep.getEpisodeSynopsis());
        request.getParams().put("extra_prompt", ShortJson.text(req.get("extra_prompt")));
        request.setPrompt("Generate script for " + ep.getEpisodeTitle());
        AiProviderResult result = execute(model, request);
        scriptService.persistGeneratedScript(ep, ShortJson.list(result.getTextContent()));
    }

    private void finishRewrite(ShortAiTask task, Map<String, Object> req, AiModelConfig model) {
        ShortEpisode ep = episode(task.getEpisodeId());
        if (ep == null) {
            return;
        }
        String instruction = ShortJson.text(req.get("instruction"));
        scriptService.persistRewrite(ep, ShortJson.integer(req.get("scene_no"), 0),
                req.get("line_no") == null ? null : ShortJson.integer(req.get("line_no"), 0),
                instruction,
                (original, instr) -> {
                    AiProviderRequest request = baseRequest(model, "TEXT", "REWRITE");
                    request.getParams().put("original", original);
                    request.getParams().put("instruction", instr);
                    request.setPrompt(original + "\n" + instr);
                    return execute(model, request).getTextContent();
                });
    }

    private void finishPortrait(ShortAiTask task, Map<String, Object> req, AiModelConfig model) {
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andCharacterIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortCharacter row = shortCharacterMapper.selectOneByExample(ex);
        if (row == null) {
            return;
        }
        int count = ShortJson.integer(req.get("candidate_count"), 4);
        String appearance = row.getAppearancePrompt() == null ? "" : row.getAppearancePrompt().trim();
        String prompt = row.getCharacterName()
                + (appearance.isBlank() ? "" : "，" + appearance)
                + "，竖屏 9:16 电影感角色定妆照，真人写实，短剧人物卡";
        for (int i = 0; i < count; i++) {
            AiProviderRequest request = baseRequest(model, "IMAGE", "PORTRAIT");
            request.getParams().put("title", row.getCharacterName());
            request.getParams().put("subtitle", "候选 " + (i + 1));
            request.getParams().put("color_index", i);
            request.getParams().put("aspect_ratio", "9:16");
            request.setPrompt(prompt);
            AiProviderResult result = execute(model, request);
            saveBinary(row.getUserId(), row.getWorkId(), null, row.getCharacterId(), "IMAGE", result,
                    task.getTaskId(), i == 0);
        }
        ShortCharacter patch = new ShortCharacter();
        patch.setCardStatus("GENERATING");
        patch.setUpdatedTime(new Date());
        ShortCharacterExample up = new ShortCharacterExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortCharacterMapper.updateByExampleSelective(patch, up);
    }

    private void finishScenePortrait(ShortAiTask task, Map<String, Object> req, AiModelConfig model) {
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andSceneCardIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortSceneCard row = shortSceneCardMapper.selectOneByExample(ex);
        if (row == null) {
            return;
        }
        int count = ShortJson.integer(req.get("candidate_count"), 4);
        String appearance = row.getAppearancePrompt() == null ? "" : row.getAppearancePrompt().trim();
        String prompt = row.getCardName()
                + (appearance.isBlank() ? "" : "，" + appearance)
                + "，竖屏 9:16 电影感场景空镜，短剧场景卡";
        for (int i = 0; i < count; i++) {
            AiProviderRequest request = baseRequest(model, "IMAGE", "SCENE_PORTRAIT");
            request.getParams().put("title", row.getCardName());
            request.getParams().put("subtitle", "场景候选 " + (i + 1));
            request.getParams().put("color_index", i);
            request.getParams().put("aspect_ratio", "9:16");
            request.setPrompt(prompt);
            AiProviderResult result = execute(model, request);
            saveBinary(row.getUserId(), row.getWorkId(), null, row.getSceneCardId(), "IMAGE", result,
                    task.getTaskId(), i == 0);
        }
    }

    private void finishShot(ShortAiTask task, AiModelConfig model) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null) {
            return;
        }
        String ability = "VIDEO".equals(task.getTaskType()) ? "VIDEO" : "IMAGE";
        AiProviderRequest request = baseRequest(model, ability, "SHOT");
        request.getParams().put("title", "镜 " + shot.getShotNo());
        request.getParams().put("subtitle", shot.getShotDesc());
        request.getParams().put("color_index", Math.abs(shot.getShotNo() == null ? 0 : shot.getShotNo()));
        request.getParams().put("aspect_ratio", "9:16");
        if (shot.getShotDurationSec() != null) {
            request.getParams().put("duration_sec", shot.getShotDurationSec());
        }
        request.setPrompt(shotPrompt(shot, ability));
        if ("VIDEO".equals(ability)) {
            attachFirstFrame(shot, request);
        }
        AiProviderResult result = execute(model, request);
        ShortAsset asset = saveBinary(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(), shot.getShotId(),
                ability, result, task.getTaskId(), true);
        ShortShot patch = new ShortShot();
        if ("VIDEO".equals(task.getTaskType())) {
            patch.setVideoAssetId(asset.getAssetId());
            patch.setShotStatus("VIDEO_READY");
            if (shot.getShotDurationSec() != null) {
                asset.setDurationSec(shot.getShotDurationSec());
                shortAssetMapper.updateByPrimaryKeySelective(asset);
            }
        } else {
            patch.setFirstFrameAssetId(asset.getAssetId());
            patch.setShotStatus("IMAGE_READY");
            if (Integer.valueOf(1).equals(task.getAutoNextFlag())) {
                Map<String, Object> origin = ShortJson.map(task.getRequestJson());
                String nextModel = ShortJson.text(origin.get("next_model_code"));
                Map<String, Object> nextReq = new LinkedHashMap<>();
                nextReq.put("shot_id", shot.getShotId());
                if (!nextModel.isBlank()) {
                    nextReq.put("model_code", nextModel);
                }
                aiTaskQueue.enqueue(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(), "VIDEO", "SHOT",
                        shot.getShotId(), nextReq, task.getBatchId(), 0,
                        nextModel.isBlank() ? null : nextModel);
            }
        }
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
    }

    private void finishCompose(ShortAiTask task, AiModelConfig model) {
        ShortEpisode ep = episode(task.getEpisodeId());
        if (ep == null) {
            return;
        }
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("shot_no asc");
        List<ShortShot> shots = shortShotMapper.selectByExample(sx);
        Map<String, Object> timelineMap = new LinkedHashMap<>();
        timelineMap.put("episode_id", ep.getEpisodeId());
        List<Map<String, Object>> shotMeta = new ArrayList<>();
        int duration = 0;
        for (ShortShot shot : shots) {
            int dur = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
            duration += dur;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("shot_no", shot.getShotNo());
            m.put("shot_id", shot.getShotId());
            m.put("desc", shot.getShotDesc());
            m.put("first_frame_asset_id", shot.getFirstFrameAssetId());
            m.put("video_asset_id", shot.getVideoAssetId());
            m.put("duration_sec", dur);
            shotMeta.add(m);
        }
        timelineMap.put("shots", shotMeta);
        timelineMap.put("duration_sec", duration);
        String timeline = ShortJson.toJson(timelineMap);

        AiProviderRequest request = baseRequest(model, "COMPOSE", "COMPOSE");
        request.getParams().put("title", ep.getEpisodeTitle());
        request.getParams().put("duration_sec", duration);
        request.getParams().put(FfmpegComposeProvider.PARAM_CLIPS, loadClips(shots));
        request.setPrompt(timeline);
        AiProviderResult result = execute(model, request);
        ShortAsset asset = saveBinary(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), ep.getEpisodeId(), "FILM",
                result, task.getTaskId(), true);
        asset.setDurationSec(duration);
        shortAssetMapper.updateByPrimaryKeySelective(asset);
        ShortFilmExample fx = new ShortFilmExample();
        fx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ShortFilm film = shortFilmMapper.selectOneByExample(fx);
        if (film == null) {
            film = new ShortFilm();
            film.setFilmId(ShortIds.id("f-"));
            film.setEpisodeId(ep.getEpisodeId());
            film.setWorkId(ep.getWorkId());
            film.setUserId(ep.getUserId());
            film.setDeletedFlag(0);
            film.setCreatedTime(new Date());
            shortFilmMapper.insertSelective(film);
            film = shortFilmMapper.selectOneByExample(fx);
        }
        ShortFilm patch = new ShortFilm();
        patch.setFilmAssetId(asset.getAssetId());
        patch.setFilmDurationSec(duration);
        patch.setComposeStatus("READY");
        patch.setComposeTaskId(task.getTaskId());
        patch.setComposedTime(new Date());
        patch.setExportJson(timeline);
        patch.setUpdatedTime(new Date());
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);
        ShortEpisode epPatch = new ShortEpisode();
        epPatch.setComposeStatus("READY");
        epPatch.setFilmAssetId(asset.getAssetId());
        epPatch.setFilmDurationSec(duration);
        epPatch.setUpdatedTime(new Date());
        ShortEpisodeExample eup = new ShortEpisodeExample();
        eup.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(epPatch, eup);
        workStatusService.refresh(ep.getWorkId());
    }

    /**
     * 取每个镜头已生成的视频字节。缺片就直接失败并点名是哪几镜——以前这里静默退回假成片，
     * 结果是「明明没出视频却提示合成成功」。
     */
    private List<byte[]> loadClips(List<ShortShot> shots) {
        List<byte[]> clips = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (ShortShot shot : shots) {
            byte[] bytes = clipBytes(shot);
            if (bytes == null) {
                missing.add("镜" + shot.getShotNo());
                continue;
            }
            clips.add(bytes);
        }
        if (!missing.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "以下镜头还没有可用视频，无法合成：" + String.join("、", missing));
        }
        return clips;
    }

    private byte[] clipBytes(ShortShot shot) {
        String assetId = shot.getVideoAssetId();
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        ShortAssetExample ax = new ShortAssetExample();
        ax.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ax);
        String key = asset == null ? null : asset.getStorageKey();
        // 内联 SVG 是演示种子占位，不是真视频，不能拿去拼接。
        if (key == null || key.isBlank() || key.startsWith("<svg")) {
            return null;
        }
        try {
            byte[] bytes = objectStorage.get(key);
            return bytes == null || bytes.length == 0 ? null : bytes;
        } catch (RuntimeException ex) {
            log.warn("compose clip fetch failed shotId={} assetId={}", shot.getShotId(), assetId, ex);
            return null;
        }
    }

    private ShortAsset saveBinary(String userId, String workId, String episodeId, String shotId, String type,
            AiProviderResult result, String taskId, boolean selected) {
        ShortAsset asset = new ShortAsset();
        asset.setAssetId(ShortIds.id("a-"));
        asset.setUserId(userId);
        asset.setWorkId(workId);
        asset.setEpisodeId(episodeId);
        asset.setShotId(shotId);
        asset.setAssetType(type);
        String mime = result.getMimeType() == null ? "application/octet-stream" : result.getMimeType();
        asset.setMimeType(mime);
        byte[] bytes = result.getBinaryContent();
        if (bytes == null && result.getTextContent() != null) {
            bytes = result.getTextContent().getBytes(StandardCharsets.UTF_8);
        }
        if (bytes == null) {
            bytes = new byte[0];
        }
        String ext = extOf(mime);
        String env = activeEnv();
        String objectKey = "short/" + env + "/" + userId + "/" + workId + "/" + asset.getAssetId() + "." + ext;
        objectStorage.put(objectKey, bytes, mime);
        asset.setStorageKey(objectKey);
        asset.setFileUrl("/bgssai/user/assets/" + asset.getAssetId());
        asset.setFileSizeByte(bytes.length);
        asset.setWidth(result.getWidth() == null ? 1080 : result.getWidth());
        asset.setHeight(result.getHeight() == null ? 1920 : result.getHeight());
        if (result.getDurationSec() != null) {
            asset.setDurationSec(result.getDurationSec());
        }
        asset.setSourceTaskId(taskId);
        asset.setSelectedFlag(selected ? 1 : 0);
        asset.setDeletedFlag(0);
        asset.setCreatedTime(new Date());
        shortAssetMapper.insertSelective(asset);
        return asset;
    }

    private void insertUsage(ShortAiTask task, AiModelConfig model, int usageAmount) {
        AiUsageRecord row = new AiUsageRecord();
        row.setRecordId(ShortIds.id("ur-"));
        row.setUserId(task.getUserId());
        row.setWorkId(task.getWorkId());
        row.setEpisodeId(task.getEpisodeId());
        row.setShotId("SHOT".equals(task.getTargetType()) ? task.getTargetId() : null);
        row.setTaskId(task.getTaskId());
        row.setAbilityType(task.getTaskType());
        row.setModelCode(model.getModelCode());
        row.setQualityLevel(model.getQualityLevel());
        row.setPriceUnit(model.getPriceUnit());
        row.setUsageAmount(usageAmount);
        row.setUnitPriceCent(model.getUnitPriceCent());
        row.setCostCent(task.getCostCent() == null
                ? aiModelResolveService.costCent(model, usageAmount)
                : task.getCostCent());
        row.setCreatedTime(new Date());
        aiUsageRecordMapper.insertSelective(row);
    }

    private AiProviderRequest baseRequest(AiModelConfig model, String ability, String operation) {
        AiProviderRequest request = new AiProviderRequest();
        request.setAbilityType(ability);
        request.setOperation(operation);
        request.setModel(model);
        request.setParams(new LinkedHashMap<>());
        return request;
    }

    private AiProviderResult execute(AiModelConfig model, AiProviderRequest request) {
        AiProvider provider = aiProviderRegistry.get(model.getProviderCode());
        return provider.execute(request);
    }

    private static String shotPrompt(ShortShot shot, String ability) {
        String imagePrompt = shot.getImagePrompt() == null ? "" : shot.getImagePrompt().trim();
        String desc = shot.getShotDesc() == null ? "" : shot.getShotDesc().trim();
        String extra = shot.getExtraPrompt() == null ? "" : shot.getExtraPrompt().trim();
        String camera = shot.getCameraMove() == null ? "" : shot.getCameraMove().trim();
        if ("VIDEO".equals(ability)) {
            StringBuilder sb = new StringBuilder();
            String motion = extra.isBlank() ? desc : extra;
            if (motion.isBlank()) {
                motion = imagePrompt;
            }
            if (!motion.isBlank()) {
                sb.append(motion);
            }
            if (!camera.isBlank()) {
                if (sb.length() > 0) {
                    sb.append("，");
                }
                sb.append("运镜：").append(camera);
            }
            if (sb.length() == 0) {
                sb.append("镜头缓慢推进，人物自然动作，竖屏短剧");
            }
            return sb.toString();
        }
        if (!imagePrompt.isBlank()) {
            return imagePrompt;
        }
        if (!desc.isBlank()) {
            return desc + "，竖屏 9:16 电影感首帧，短剧画面";
        }
        return "竖屏 9:16 电影感短剧首帧";
    }

    private void attachFirstFrame(ShortShot shot, AiProviderRequest request) {
        if (shot.getFirstFrameAssetId() == null || shot.getFirstFrameAssetId().isBlank()) {
            return;
        }
        ShortAssetExample ax = new ShortAssetExample();
        ax.createCriteria().andAssetIdEqualTo(shot.getFirstFrameAssetId()).andDeletedFlagEqualTo(0);
        ShortAsset frame = shortAssetMapper.selectOneByExample(ax);
        if (frame == null || frame.getStorageKey() == null || frame.getStorageKey().isBlank()) {
            return;
        }
        String mime = frame.getMimeType() == null ? "" : frame.getMimeType().toLowerCase();
        if (mime.contains("svg") || mime.contains("json")) {
            return;
        }
        if (!(mime.contains("png") || mime.contains("jpeg") || mime.contains("jpg") || mime.contains("webp")
                || mime.contains("image/"))) {
            return;
        }
        byte[] bytes = objectStorage.get(frame.getStorageKey());
        if (bytes == null || bytes.length == 0) {
            return;
        }
        request.getParams().put("image_base64", Base64.getEncoder().encodeToString(bytes));
        request.getParams().put("image_mime", frame.getMimeType());
    }

    private ShortEpisode episode(String episodeId) {
        if (episodeId == null) {
            return null;
        }
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        return shortEpisodeMapper.selectOneByExample(ex);
    }

    private void addSpent(ShortAiTask task) {
        if (task.getWorkId() == null || task.getCostCent() == null) {
            return;
        }
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(task.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        if (work == null) {
            return;
        }
        ShortWork patch = new ShortWork();
        patch.setCostSpentCent((work.getCostSpentCent() == null ? 0 : work.getCostSpentCent()) + task.getCostCent());
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
    }

    private void fail(ShortAiTask task, String code, String message) {
        ShortAiTask patch = new ShortAiTask();
        patch.setTaskStatus("FAILED");
        patch.setFailReasonCode(code);
        patch.setFailReason(message);
        patch.setFinishedTime(new Date());
        patch.setUpdatedTime(new Date());
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId());
        shortAiTaskMapper.updateByExampleSelective(patch, up);
    }

    /** 只有仍在 RUNNING 的任务才置失败，避免覆盖已取消 / 已回收的终态。 */
    private void failIfRunning(ShortAiTask task, String code, String message) {
        ShortAiTask patch = new ShortAiTask();
        patch.setTaskStatus("FAILED");
        patch.setFailReasonCode(code);
        patch.setFailReason(message == null ? "" : message);
        patch.setFinishedTime(new Date());
        patch.setUpdatedTime(new Date());
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("RUNNING");
        shortAiTaskMapper.updateByExampleSelective(patch, up);
    }

    private String activeEnv() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles != null && profiles.length > 0) {
            return profiles[0];
        }
        return "local";
    }

    private static String extOf(String mime) {
        if (mime == null) {
            return "bin";
        }
        if (mime.contains("svg")) {
            return "svg";
        }
        if (mime.contains("png")) {
            return "png";
        }
        if (mime.contains("jpeg") || mime.contains("jpg")) {
            return "jpg";
        }
        if (mime.contains("mp4")) {
            return "mp4";
        }
        if (mime.contains("mpeg") || mime.contains("mp3")) {
            return "mp3";
        }
        if (mime.contains("json")) {
            return "json";
        }
        return "bin";
    }
}
