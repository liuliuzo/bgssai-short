package com.bgssai.shortai.production.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.AiProviderRequest;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.aiprovider.FfmpegComposeProvider;
import com.bgssai.shortai.aiprovider.TencentTokenHubAiProvider;
import com.bgssai.shortai.aiprovider.TokenHubClientSupport;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortSourceChapterMapper;
import com.bgssai.shortai.mapper.ShortSourceDocMapper;
import com.bgssai.shortai.mapper.ShortSubtitleMapper;
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
import com.bgssai.shortai.model.ShortSourceChapter;
import com.bgssai.shortai.model.ShortSourceChapterExample;
import com.bgssai.shortai.model.ShortSourceDoc;
import com.bgssai.shortai.model.ShortSourceDocExample;
import com.bgssai.shortai.model.ShortSubtitle;
import com.bgssai.shortai.model.ShortSubtitleExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.support.EditBaselineCards;
import com.bgssai.shortai.production.support.LastFrameExtractor;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.production.support.ShotConsistency;
import com.bgssai.shortai.production.support.SubtitleSrtBuilder;
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
    private ShortSubtitleMapper shortSubtitleMapper;
    @Autowired
    private ShortSourceDocMapper shortSourceDocMapper;
    @Autowired
    private ShortSourceChapterMapper shortSourceChapterMapper;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private WorkStatusService workStatusService;
    @Autowired
    private DubbingService dubbingService;
    @Autowired
    private FrameLinkService frameLinkService;
    @Autowired
    private FinishedFilmLifecycle finishedFilmLifecycle;
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

    @Autowired
    private ShortTaskSettlementService settlement;
    @Autowired
    private QuotaConsumeService quotaConsumeService;

    private final ThreadLocal<Integer> executionUsage = ThreadLocal.withInitial(() -> 0);

    /** 判定 RUNNING 卡死的宽限秒数：模型 timeout_sec 之上再留这么久仍无结果就判超时。 */
    private static final int STALE_GRACE_SEC = 120;

    /** 模型没配 timeout_sec 时的兜底上限。 */
    private static final int DEFAULT_TIMEOUT_SEC = 600;

    /** 改编提示词里原文素材的字符预算：再多就是花钱让模型读铺垫，且容易超上下文。 */
    private static final int ADAPT_PROMPT_CHAR_BUDGET = 60000;

    /** 库/表缺失等 SQL 失败时的初始退避（毫秒）。 */
    private static final long POLL_BACKOFF_INITIAL_MS = 5_000L;

    /** SQL 风暴防护：单次退避上限（毫秒）。 */
    private static final long POLL_BACKOFF_MAX_MS = 300_000L;

    private final AtomicInteger pollFailureStreak = new AtomicInteger();
    private volatile long pollPauseUntilEpochMs;

    @Scheduled(fixedDelay = 800)
    public void tick() {
        if (isPollPaused()) {
            return;
        }
        try {
            ShortAiTaskExample ex = new ShortAiTaskExample();
            ex.createCriteria().andTaskStatusEqualTo("QUEUED");
            ex.setOrderByClause("id asc");
            ShortAiTask task = shortAiTaskMapper.selectOneByExample(ex);
            if (task == null) {
                onPollSuccess();
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
                onPollSuccess();
                return;
            }
            try {
                aiTaskExecutor.execute(() -> run(task));
            } catch (RejectedExecutionException rejected) {
                // 池子满了就把任务退回排队，交给下一轮 tick，不在内存里堆积。
                requeue(task);
            }
            onPollSuccess();
        } catch (DataAccessException ex) {
            onPollFailure("tick", ex);
        } catch (RuntimeException ex) {
            if (looksLikeSchemaOrDatasourceFailure(ex)) {
                onPollFailure("tick", ex);
            } else {
                log.error("ai task tick unexpected failure", ex);
            }
        }
    }

    /**
     * 回收卡死的任务。上游调用即便挂住，任务也不会永远停在 RUNNING：
     * 超过「模型 timeout_sec + 宽限」仍未结束的行会被判 MODEL_TIMEOUT。
     * 进程重启后遗留的 RUNNING 行同样由这里兜底。
     */
    @Scheduled(fixedDelay = 30_000)
    public void reapStaleTasks() {
        if (isPollPaused()) {
            return;
        }
        try {
            ShortAiTaskExample ex = new ShortAiTaskExample();
            ex.createCriteria().andTaskStatusEqualTo("RUNNING");
            for (ShortAiTask task : shortAiTaskMapper.selectByExample(ex)) {
                Date started = task.getStartedTime() == null ? task.getQueuedTime() : task.getStartedTime();
                if (started == null) {
                    failIfRunning(task, "ORPHAN_RUNNING",
                            "任务处于 RUNNING 但没有开始/入队时间，已判定为进程重启遗留孤儿任务");
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
            onPollSuccess();
        } catch (DataAccessException ex) {
            onPollFailure("reapStaleTasks", ex);
        } catch (RuntimeException ex) {
            if (looksLikeSchemaOrDatasourceFailure(ex)) {
                onPollFailure("reapStaleTasks", ex);
            } else {
                log.error("ai task reap unexpected failure", ex);
            }
        }
    }

    private boolean isPollPaused() {
        return System.currentTimeMillis() < pollPauseUntilEpochMs;
    }

    private void onPollSuccess() {
        if (pollFailureStreak.get() != 0 || pollPauseUntilEpochMs != 0L) {
            pollFailureStreak.set(0);
            pollPauseUntilEpochMs = 0L;
        }
    }

    /**
     * 库不存在 / 表不存在时，fixedDelay=800 会把 ERROR 打满磁盘。失败后指数退避，
     * 同一窗口只打一条 ERROR（含建议执行 sql/DDL.sql）。
     */
    private void onPollFailure(String job, Exception ex) {
        int streak = pollFailureStreak.incrementAndGet();
        long shift = Math.min(16, Math.max(0, streak - 1));
        long backoffMs = Math.min(POLL_BACKOFF_MAX_MS, POLL_BACKOFF_INITIAL_MS << shift);
        pollPauseUntilEpochMs = System.currentTimeMillis() + backoffMs;
        String hint = looksLikeSchemaOrDatasourceFailure(ex)
                ? " check datasource DB name and apply sql/DDL.sql (short_ai_task)"
                : "";
        log.error("ai task {} failed (streak={}); pause {}ms.{} cause={}",
                job, streak, backoffMs, hint, summarize(ex));
    }

    private static boolean looksLikeSchemaOrDatasourceFailure(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof DataAccessException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("SQLSyntaxErrorException")
                    || name.contains("SQLException")
                    || name.contains("CannotGetJdbcConnectionException")
                    || name.contains("BadSqlGrammarException")) {
                return true;
            }
            String msg = cur.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("unknown database")
                        || lower.contains("doesn't exist")
                        || lower.contains("does not exist")
                        || lower.contains("short_ai_task")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String summarize(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = ex;
        int depth = 0;
        while (cur != null && depth < 4) {
            if (depth > 0) {
                sb.append(" | ");
            }
            sb.append(cur.getClass().getSimpleName());
            if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
                sb.append(": ").append(cur.getMessage());
            }
            cur = cur.getCause();
            depth++;
        }
        return sb.toString();
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
        executionUsage.set(0);
        com.bgssai.shortai.billing.UsageScope.set(task.getUserId(),task.getWorkId(),task.getTaskId());
        try {
            AiModelConfig model = aiModelResolveService.resolve(task.getTaskType(), task.getModelCode());
            if (quotaConsumeService != null) {
                int cost = task.getCostCent() == null ? 0 : task.getCostCent();
                quotaConsumeService.assertAffordable(task.getUserId(), cost);
            }
            complete(task, model);
            boolean settled = settlement.settle(task, model, Math.max(1, executionUsage.get()));
            if (!settled) {
                failIfRunning(task, "SETTLE_RACE",
                        "任务结果未能落账（可能已被超时回收），请在任务中心重试");
            }
        } catch (BizException exBiz) {
            failIfRunning(task, exBiz.getCode(), exBiz.getMessage());
            noteComposeFailed(task);
        } catch (Exception exAll) {
            log.warn("ai task failed taskId={}", task.getTaskId(), exAll);
            failIfRunning(task, "MODEL_TIMEOUT", exAll.getMessage());
            noteComposeFailed(task);
        } finally {
            executionUsage.remove();
            com.bgssai.shortai.billing.UsageScope.clear();
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
        if ("ADAPT_OUTLINE".equals(task.getTargetType())) {
            finishAdaptOutline(task, req, model);
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
        if ("LAST_FRAME".equals(task.getTargetType())) {
            finishLastFrame(task, req, model);
            return;
        }
        if ("COMPOSE".equals(task.getTargetType())) {
            finishCompose(task, model);
            return;
        }
        if ("DUBBING".equals(task.getTargetType())) {
            finishDubbing(task, model);
            return;
        }
        if ("SUBTITLE_TRANSCRIBE".equals(task.getTargetType())) {
            finishSubtitleTranscribe(task, model);
            return;
        }
        if ("BGM".equals(task.getTargetType())) {
            finishBgm(task, req, model);
            return;
        }
        throw new BizException(CommonErrorCode.RULE_REJECT, "unsupported task target");
    }

    private void finishDubbing(ShortAiTask task, AiModelConfig model) {
        ShortDubbingExample ex = new ShortDubbingExample();
        ex.createCriteria().andDubbingIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortDubbing row = shortDubbingMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
        }
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andShotIdEqualTo(row.getShotId()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(sx);
        if (shot == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
        }
        AiProviderRequest request = baseRequest(model, "VOICE", "DUBBING");
        request.getParams().put("title", row.getLineText());
        request.getParams().put("voice_id", row.getVoiceId());
        request.getParams().put("speed", row.getVoiceSpeed());
        request.getParams().put("emotion", row.getEmotion());
        request.setPrompt(row.getLineText());
        AiProviderResult result = execute(model, request);
        byte[] bytes = result.getBinaryContent();
        String mime = result.getMimeType();
        if (bytes == null || bytes.length == 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "voice provider returned empty audio");
        }
        if (mime == null || mime.isBlank()) {
            mime = "audio/mpeg";
        }
        int durationSec = result.getDurationSec() == null ? 0 : result.getDurationSec();
        if (durationSec <= 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "配音文件缺少实际时长，任务未完成");
        }
        AiProviderResult packed = AiProviderResult.binary(bytes, mime);
        packed.setDurationSec(durationSec);
        ShortAsset asset = saveBinary(row.getUserId(), row.getWorkId(), row.getEpisodeId(), row.getShotId(), "AUDIO",
                packed, task.getTaskId(), true);
        asset.setDurationSec(durationSec);
        shortAssetMapper.updateByPrimaryKeySelective(asset);
        dubbingService.finishDubbingAlign(row, shot, asset.getAssetId(), durationSec);
        if (finishedFilmLifecycle != null) {
            finishedFilmLifecycle.expireAfterUpstreamChange(row.getEpisodeId());
        }
    }

    private void finishSubtitleTranscribe(ShortAiTask task, AiModelConfig model) {
        ShortDubbingExample ex = new ShortDubbingExample();
        ex.createCriteria().andDubbingIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortDubbing dubbing = shortDubbingMapper.selectOneByExample(ex);
        if (dubbing == null || dubbing.getAudioAssetId() == null || dubbing.getAudioAssetId().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "dubbing audio not found");
        }
        ShortAssetExample ax = new ShortAssetExample();
        ax.createCriteria().andAssetIdEqualTo(dubbing.getAudioAssetId()).andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ax);
        if (asset == null || asset.getStorageKey() == null || asset.getStorageKey().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "dubbing audio asset missing");
        }
        String inputUrl = objectStorage.presignGet(asset.getStorageKey(), 3600).downloadUrl();
        AiProviderRequest request = baseRequest(model, "ASR", "SUBTITLE_TRANSCRIBE");
        request.getParams().put("input_url", inputUrl);
        request.setPrompt("Transcribe dubbing " + dubbing.getDubbingId());
        AiProviderResult result = execute(model, request);
        Map<String, Object> output = ShortJson.map(result.getTextContent());
        dubbingService.finishTranscription(dubbing, output);
        Map<String, Object> taskResult = new LinkedHashMap<>();
        taskResult.put("dubbing_id", dubbing.getDubbingId());
        taskResult.put("text", output.get("text"));
        taskResult.put("duration_sec", result.getDurationSec());
        patchTaskResult(task, taskResult);
    }

    private void finishBgm(ShortAiTask task, Map<String, Object> req, AiModelConfig model) {
        ShortEpisode ep = episode(task.getEpisodeId());
        if (ep == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "episode not found");
        }
        AiProviderRequest request = baseRequest(model, "MUSIC", "BGM");
        request.setPrompt(ShortJson.text(req.get("prompt")));
        request.getParams().put("lyrics", req.get("lyrics"));
        request.getParams().put("is_instrumental", req.get("is_instrumental"));
        AiProviderResult result = execute(model, request);
        if (result.getBinaryContent() == null || result.getBinaryContent().length == 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "music provider returned empty audio");
        }
        ShortAsset asset = saveBinary(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), null, "AUDIO",
                result, task.getTaskId(), false);
        patchTaskResult(task, Map.of("bgm_asset_id", asset.getAssetId()));
    }

    /**
     * 按原著章节改编出分集大纲。与 finishOutline 的区别只在素材来源：一个是一句话梗概，
     * 一个是原文章节；产出与落库口径完全一致，因此后续的节奏体检、拆镜、定稿全都照旧走
     * ——改编稿不走单独的通道，也就没有「有原著就少一道检查」这种口子可开。
     */
    private void finishAdaptOutline(ShortAiTask task, Map<String, Object> req, AiModelConfig model) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(task.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        if (work == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "work not found");
        }
        ShortSourceDocExample sx = new ShortSourceDocExample();
        sx.createCriteria().andSourceIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortSourceDoc doc = shortSourceDocMapper.selectOneByExample(sx);
        if (doc == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "source not found or deleted");
        }
        int from = ShortJson.integer(req.get("chapter_no_from"), 1);
        int to = ShortJson.integer(req.get("chapter_no_to"), Integer.MAX_VALUE);
        int targetCount = ShortJson.integer(req.get("target_episode_count"), 0);
        if (targetCount < 1) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "target_episode_count invalid");
        }
        ShortSourceChapterExample cx = new ShortSourceChapterExample();
        cx.createCriteria().andSourceIdEqualTo(doc.getSourceId()).andDeletedFlagEqualTo(0)
                .andChapterNoGreaterThanOrEqualTo(from).andChapterNoLessThanOrEqualTo(to);
        cx.setOrderByClause("chapter_no asc");
        List<ShortSourceChapter> chapters = shortSourceChapterMapper.selectByExample(cx);
        if (chapters.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "no chapter in range");
        }

        // 原文可能几十万字，直接全塞会超上下文，而且大部分钱花在模型读铺垫上。
        // 按章均分预算截断，保证每一章都有内容进入提示词，而不是前几章吃掉全部额度、后面的章直接消失。
        int budgetPerChapter = Math.max(600, ADAPT_PROMPT_CHAR_BUDGET / chapters.size());
        StringBuilder material = new StringBuilder();
        List<Integer> chapterNos = new ArrayList<>();
        for (ShortSourceChapter chapter : chapters) {
            chapterNos.add(chapter.getChapterNo());
            String text = chapter.getChapterText() == null ? "" : chapter.getChapterText();
            if (text.length() > budgetPerChapter) {
                text = text.substring(0, budgetPerChapter) + "（本章后续省略）";
            }
            material.append("【第").append(chapter.getChapterNo()).append("章 ")
                    .append(chapter.getChapterTitle() == null ? "" : chapter.getChapterTitle())
                    .append("】\n").append(text).append("\n\n");
        }

        AiProviderRequest request = baseRequest(model, "TEXT", "ADAPT_OUTLINE");
        Map<String, Object> params = request.getParams();
        params.put("title", work.getWorkTitle());
        params.put("genre", work.getGenre());
        params.put("episode_count", targetCount);
        params.put("source_type", doc.getSourceType());
        request.setPrompt("把下面的原著片段改编成 " + targetCount + " 集竖屏短剧大纲。"
                + "短剧的节奏与小说不同：每集前 3 秒必须有冲突或悬念，每 15 秒给一个信息点，"
                + "结尾留下未解决的问题；不要保留小说式的铺垫与心理描写。"
                + "只返回 JSON 数组，不要 Markdown。每项字段必须为 episode_no、episode_title、"
                + "episode_synopsis、ending_hook、chapter_no_list（该集取自的原文章号数组）。"
                + "原著片段如下：\n" + material);
        AiProviderResult result = execute(model, request);
        List<Map<String, Object>> outline = jsonList(result.getTextContent());
        if (outline.isEmpty()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "text model returned invalid outline JSON");
        }

        // 目标集数可能大于作品现有集数，缺的先补出来，否则 applyOutline 会静默丢掉后几集
        ShortEpisodeExample allEx = new ShortEpisodeExample();
        allEx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        allEx.setOrderByClause("episode_no desc");
        List<ShortEpisode> existing = shortEpisodeMapper.selectByExample(allEx);
        int maxNo = existing.isEmpty() ? 0
                : (existing.get(0).getEpisodeNo() == null ? 0 : existing.get(0).getEpisodeNo());
        Date now = new Date();
        for (int no = maxNo + 1; no <= targetCount; no++) {
            ShortEpisode ep = new ShortEpisode();
            ep.setEpisodeId(ShortIds.id("e-"));
            ep.setWorkId(work.getWorkId());
            ep.setUserId(work.getUserId());
            ep.setEpisodeNo(no);
            ep.setEpisodeTitle("第" + no + "集");
            ep.setScriptStatus("NONE");
            ep.setStoryboardStatus("NONE");
            ep.setDubbingStatus("NONE");
            ep.setComposeStatus("NONE");
            ep.setReviewStatus("NOT_SUBMITTED");
            ep.setDeletedFlag(0);
            ep.setCreatedTime(now);
            ep.setUpdatedTime(now);
            shortEpisodeMapper.insertSelective(ep);
        }

        ShortWork patch = new ShortWork();
        patch.setOutlineJson(ShortJson.toJson(outline));
        patch.setUpdatedTime(now);
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        // 只更新大纲层，已定稿集的正文逐字保留（与 script-studio 规则 8 同一口径）
        scriptService.applyOutline(work.getWorkId(), outline);

        for (Map<String, Object> item : outline) {
            int no = ShortJson.integer(item.get("episode_no"), 0);
            ShortEpisodeExample ex = new ShortEpisodeExample();
            ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andEpisodeNoEqualTo(no)
                    .andDeletedFlagEqualTo(0);
            ShortEpisode ep = shortEpisodeMapper.selectOneByExample(ex);
            if (ep == null) {
                continue;
            }
            Object refs = item.get("chapter_no_list");
            Map<String, Object> sourceRef = new LinkedHashMap<>();
            sourceRef.put("source_id", doc.getSourceId());
            sourceRef.put("chapter_no_list", refs instanceof List ? refs : chapterNos);
            ShortEpisode epPatch = new ShortEpisode();
            epPatch.setSourceRefJson(ShortJson.toJson(sourceRef));
            epPatch.setUpdatedTime(now);
            ShortEpisodeExample epUp = new ShortEpisodeExample();
            epUp.createCriteria().andIdEqualTo(ep.getId());
            shortEpisodeMapper.updateByExampleSelective(epPatch, epUp);
        }

        ShortSourceDoc docPatch = new ShortSourceDoc();
        docPatch.setAdaptedFlag(1);
        docPatch.setImportStatus("ADAPTED");
        docPatch.setUpdatedTime(now);
        ShortSourceDocExample docUp = new ShortSourceDocExample();
        docUp.createCriteria().andIdEqualTo(doc.getId());
        shortSourceDocMapper.updateByExampleSelective(docPatch, docUp);
        workStatusService.refresh(work.getWorkId());
        log.info("source adapted sourceId={} workId={} episodes={}", doc.getSourceId(),
                work.getWorkId(), outline.size());
    }

    private void finishOutline(ShortAiTask task, AiModelConfig model) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(task.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        if (work == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
        }
        AiProviderRequest request = baseRequest(model, "TEXT", "OUTLINE");
        Map<String, Object> params = request.getParams();
        params.put("title", work.getWorkTitle());
        params.put("pitch", work.getOneLinePitch());
        params.put("episode_count", work.getEpisodeCount() == null ? 24 : work.getEpisodeCount());
        params.put("genre", work.getGenre());
        request.setPrompt("请为短剧《" + work.getWorkTitle() + "》生成 "
                + (work.getEpisodeCount() == null ? 24 : work.getEpisodeCount())
                + " 集大纲。只返回 JSON 数组，不要 Markdown。每项字段必须为 episode_no、episode_title、"
                + "episode_synopsis、ending_hook。故事梗概：" + work.getOneLinePitch());
        AiProviderResult result = execute(model, request);
        List<Map<String, Object>> outline = jsonList(result.getTextContent());
        if (outline.isEmpty()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "text model returned invalid outline JSON");
        }
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
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
        }
        AiProviderRequest request = baseRequest(model, "TEXT", "SCRIPT");
        request.getParams().put("episode_title", ep.getEpisodeTitle());
        request.getParams().put("synopsis", ep.getEpisodeSynopsis());
        request.getParams().put("extra_prompt", ShortJson.text(req.get("extra_prompt")));
        request.setPrompt("请为短剧本集生成可拍摄剧本。只返回 JSON 数组，不要 Markdown。每项是场景，字段为"
                + "scene_no、scene_location、scene_summary、line_list；line_list 每项字段为 line_no、"
                + "character_name、line_text、emotion、estimated_sec。本集标题：" + ep.getEpisodeTitle()
                + "。本集梗概：" + ep.getEpisodeSynopsis() + "。额外要求：" + ShortJson.text(req.get("extra_prompt")));
        AiProviderResult result = execute(model, request);
        List<Map<String, Object>> scenes = jsonList(result.getTextContent());
        if (scenes.isEmpty()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "text model returned invalid script JSON");
        }
        scriptService.persistGeneratedScript(ep, scenes);
    }

    private void finishRewrite(ShortAiTask task, Map<String, Object> req, AiModelConfig model) {
        ShortEpisode ep = episode(task.getEpisodeId());
        if (ep == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
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
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
        }
        int count = ShortJson.integer(req.get("candidate_count"), 4);
        String appearance = row.getAppearancePrompt() == null ? "" : row.getAppearancePrompt().trim();
        String prompt = appearance.isBlank() ? row.getCharacterName() : appearance;
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
        patch.setCardStatus(referenceCompletionStatus(task));
        patch.setUpdatedTime(new Date());
        ShortCharacterExample up = new ShortCharacterExample();
        up.createCriteria().andIdEqualTo(row.getId()).andCardStatusNotEqualTo("LOCKED");
        shortCharacterMapper.updateByExampleSelective(patch, up);
    }

    private void finishScenePortrait(ShortAiTask task, Map<String, Object> req, AiModelConfig model) {
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andSceneCardIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortSceneCard row = shortSceneCardMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
        }
        int count = ShortJson.integer(req.get("candidate_count"), 4);
        String appearance = row.getAppearancePrompt() == null ? "" : row.getAppearancePrompt().trim();
        String prompt = appearance.isBlank() ? row.getCardName() : appearance;
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
        ShortSceneCard patch = new ShortSceneCard();
        patch.setCardStatus(referenceCompletionStatus(task));
        patch.setUpdatedTime(new Date());
        ShortSceneCardExample up = new ShortSceneCardExample();
        up.createCriteria().andIdEqualTo(row.getId()).andCardStatusNotEqualTo("LOCKED");
        shortSceneCardMapper.updateByExampleSelective(patch, up);
    }

    private String referenceCompletionStatus(ShortAiTask task) {
        ShortAiTaskExample other = new ShortAiTaskExample();
        other.createCriteria().andTargetIdEqualTo(task.getTargetId()).andTaskTypeEqualTo("IMAGE")
                .andTargetTypeIn(List.of("PORTRAIT", "SCENE_PORTRAIT"))
                .andTaskStatusIn(List.of("QUEUED", "RUNNING")).andTaskIdNotEqualTo(task.getTaskId());
        return shortAiTaskMapper.countByExample(other) > 0 ? "GENERATING" : "DRAFT";
    }

    private void finishShot(ShortAiTask task, AiModelConfig model) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
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
        ConsistencyBundle consistency = loadConsistency(shot);
        String prompt = ShotConsistency.withConsistency(shotPrompt(shot, ability), consistency.fragment());
        request.setPrompt(prompt);
        if ("VIDEO".equals(ability)) {
            attachFrames(shot, request, model);
        } else {
            attachConsistencyRefs(consistency, request);
        }
        AiProviderResult result = execute(model, request);
        ShotVersionGuard.rejectConsistencyLimitation(result == null ? null : result.getTextContent());
        // SHORT-01：空字节不得落成 VIDEO_READY / IMAGE_READY，避免「无素材却成功」。
        if (result == null || result.getBinaryContent() == null || result.getBinaryContent().length == 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    ("VIDEO".equals(ability) ? "video" : "image")
                            + " provider returned empty content");
        }
        boolean confirmedVideo = "VIDEO".equals(task.getTaskType())
                && ShotVersionGuard.keepConfirmedAsset(shot.getShotStatus(), shot.getVideoAssetId());
        boolean confirmedImage = !"VIDEO".equals(task.getTaskType())
                && ShotVersionGuard.keepConfirmedAsset(shot.getShotStatus(), shot.getFirstFrameAssetId());
        if (confirmedVideo || confirmedImage) {
            saveBinary(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(), shot.getShotId(),
                    ability, result, task.getTaskId(), false);
            return;
        }
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
            String lastFrameAssetId = extractLastFrame(shot, result, task);
            if (lastFrameAssetId != null) {
                patch.setLastFrameAssetId(lastFrameAssetId);
            }
        } else {
            patch.setFirstFrameAssetId(asset.getAssetId());
            patch.setShotStatus("IMAGE_READY");
            patch.setStaleFlag(0);
            if (Integer.valueOf(1).equals(task.getAutoNextFlag())) {
                Map<String, Object> origin = ShortJson.map(task.getRequestJson());
                String nextModel = ShortJson.text(origin.get("next_model_code"));
                Map<String, Object> nextReq = new LinkedHashMap<>();
                nextReq.put("shot_id", shot.getShotId());
                nextReq.put("duration_sec", shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec());
                if (!nextModel.isBlank()) {
                    nextReq.put("model_code", nextModel);
                }
                aiTaskQueue.enqueueContinuation(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(), "VIDEO", "SHOT",
                        shot.getShotId(), nextReq, task.getBatchId(), 0,
                        nextModel.isBlank() ? null : nextModel);
            }
        }
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
        if ("VIDEO".equals(task.getTaskType())) {
            if (finishedFilmLifecycle != null) {
                finishedFilmLifecycle.expireAfterUpstreamChange(shot.getEpisodeId());
            }
            if (patch.getLastFrameAssetId() != null) {
                frameLinkService.markNextStale(shot);
            }
        }
    }

    private ConsistencyBundle loadConsistency(ShortShot shot) {
        List<ShortCharacter> lockedChars = new ArrayList<>();
        for (String characterId : ShotConsistency.characterIds(shot.getCharacterRefJson())) {
            ShortCharacterExample cx = new ShortCharacterExample();
            cx.createCriteria().andCharacterIdEqualTo(characterId).andDeletedFlagEqualTo(0);
            ShortCharacter row = shortCharacterMapper.selectOneByExample(cx);
            ShotVersionGuard.requireReferencedAsset("角色", characterId, row != null,
                    row != null && "LOCKED".equals(row.getCardStatus()),
                    row != null && row.getRefImageAssetId() != null && !row.getRefImageAssetId().isBlank());
            lockedChars.add(row);
        }
        ShortSceneCard lockedScene = null;
        if (shot.getSceneCardId() != null && !shot.getSceneCardId().isBlank()) {
            ShortSceneCardExample sx = new ShortSceneCardExample();
            sx.createCriteria().andSceneCardIdEqualTo(shot.getSceneCardId()).andDeletedFlagEqualTo(0);
            ShortSceneCard row = shortSceneCardMapper.selectOneByExample(sx);
            ShotVersionGuard.requireReferencedAsset("场景", shot.getSceneCardId(), row != null,
                    row != null && "LOCKED".equals(row.getCardStatus()),
                    row != null && row.getRefImageAssetId() != null && !row.getRefImageAssetId().isBlank());
            lockedScene = row;
        }
        List<ShortSceneCard> lockedProps = new ArrayList<>();
        for (String propId : ShotConsistency.propIds(shot.getPropRefJson())) {
            ShortSceneCardExample px = new ShortSceneCardExample();
            px.createCriteria().andSceneCardIdEqualTo(propId).andDeletedFlagEqualTo(0);
            ShortSceneCard row = shortSceneCardMapper.selectOneByExample(px);
            ShotVersionGuard.requireReferencedAsset("道具", propId, row != null,
                    row != null && "LOCKED".equals(row.getCardStatus()),
                    row != null && row.getRefImageAssetId() != null && !row.getRefImageAssetId().isBlank());
            lockedProps.add(row);
        }
        return new ConsistencyBundle(lockedChars, lockedScene, lockedProps,
                ShotConsistency.appearanceFragment(lockedChars, lockedScene, lockedProps));
    }

    /**
     * IMAGE path: attach first locked character portrait as subject reference (Kling
     * image_reference=subject). Scene card appearance stays in prompt text.
     */
    private void attachConsistencyRefs(ConsistencyBundle consistency, AiProviderRequest request) {
        if (consistency.characters().isEmpty()) {
            return;
        }
        ShortCharacter primary = consistency.characters().get(0);
        attachFrameAsset(primary.getRefImageAssetId(), request, "image_url", "image_base64", "image_mime");
        if (request.param("image_base64") != null || request.param("image_url") != null) {
            request.getParams().put("image_reference", "subject");
            request.getParams().put("image_fidelity", 0.75);
            request.getParams().put("human_fidelity", 0.55);
        }
    }

    private record ConsistencyBundle(List<ShortCharacter> characters, ShortSceneCard scene,
            List<ShortSceneCard> props, String fragment) {
    }

    private String workAspectRatio(String workId) {
        if (shortWorkMapper == null || workId == null || workId.isBlank()) {
            return "9:16";
        }
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        if (work == null || work.getAspectRatio() == null || work.getAspectRatio().isBlank()) {
            return "9:16";
        }
        return work.getAspectRatio().trim();
    }

    private List<ShortCharacter> loadWorkCharacters(String workId) {
        if (shortCharacterMapper == null || workId == null || workId.isBlank()) {
            return List.of();
        }
        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        List<ShortCharacter> rows = shortCharacterMapper.selectByExample(cx);
        return rows == null ? List.of() : rows;
    }

    private void finishCompose(ShortAiTask task, AiModelConfig model) {
        ShortEpisode ep = episode(task.getEpisodeId());
        if (ep == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
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
        timelineMap.putAll(EditBaselineCards.filmRecord(shots, loadWorkCharacters(ep.getWorkId()),
                workAspectRatio(ep.getWorkId())));

        AiProviderRequest request = baseRequest(model, "COMPOSE", "COMPOSE");
        request.getParams().put("title", ep.getEpisodeTitle());
        request.getParams().put("duration_sec", duration);
        request.getParams().put(FfmpegComposeProvider.PARAM_ASPECT_RATIO, workAspectRatio(ep.getWorkId()));
        request.getParams().put(FfmpegComposeProvider.PARAM_CLIPS, loadClips(shots));
        List<Integer> clipDurations = new ArrayList<>();
        List<String> dubbingMissing = new ArrayList<>();
        List<byte[]> dubbingAudio = loadDubbingAudio(shots, clipDurations, dubbingMissing);
        request.getParams().put(FfmpegComposeProvider.PARAM_CLIP_DURATION_SEC_LIST, clipDurations);
        if (dubbingAudio.stream().anyMatch(b -> b != null && b.length > 0)) {
            request.getParams().put(FfmpegComposeProvider.PARAM_DUBBING_LIST, dubbingAudio);
        }
        byte[] bgm = loadSelectedBgm(ep);
        if (bgm != null) {
            request.getParams().put(FfmpegComposeProvider.PARAM_BGM, bgm);
        }
        String srt = loadSubtitleSrt(ep, shots);
        if (srt != null && !srt.isBlank()) {
            request.getParams().put(FfmpegComposeProvider.PARAM_SUBTITLE_SRT, srt);
        }
        if (!dubbingMissing.isEmpty()) {
            timelineMap.put("dubbing_missing_shot_nos", dubbingMissing);
            timelineMap.put("dubbing_warn",
                    "部分镜头无配音，已用静音占位：" + String.join("、", dubbingMissing));
        }
        int mixed = (int) dubbingAudio.stream().filter(b -> b != null && b.length > 0).count();
        timelineMap.put("dubbing_mixed_count", mixed);
        String timeline = ShortJson.toJson(timelineMap);
        request.setPrompt(timeline);
        AiProviderResult result = execute(model, request);
        if (result == null || result.getBinaryContent() == null || result.getBinaryContent().length == 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "compose provider returned empty film");
        }
        String filmMime = result.getMimeType() == null ? "" : result.getMimeType().toLowerCase();
        if (!filmMime.contains("video") && !filmMime.contains("mp4")) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    "compose provider did not return a video asset (mime="
                            + (result.getMimeType() == null ? "-" : result.getMimeType()) + ")");
        }
        ShortAsset asset = saveBinary(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), ep.getEpisodeId(), "FILM",
                result, task.getTaskId(), true);
        int actualDuration = result.getDurationSec();
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
        patch.setFilmDurationSec(actualDuration);
        patch.setComposeStatus("READY");
        patch.setComposeTaskId(task.getTaskId());
        patch.setComposedTime(new Date());
        patch.setExportJson(timeline);
        patch.setUpdatedTime(new Date());
        if (finishedFilmLifecycle != null) {
            finishedFilmLifecycle.applyFreshCompose(patch, film);
        } else {
            patch.setExpiredFlag(0);
            patch.setSubtitleStaleFlag(0);
        }
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);
        ShortEpisode epPatch = new ShortEpisode();
        epPatch.setComposeStatus("READY");
        epPatch.setFilmAssetId(asset.getAssetId());
        epPatch.setFilmDurationSec(actualDuration);
        epPatch.setUpdatedTime(new Date());
        ShortEpisodeExample eup = new ShortEpisodeExample();
        eup.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(epPatch, eup);
        workStatusService.refresh(ep.getWorkId());
    }

    private List<byte[]> loadDubbingAudio(List<ShortShot> shots, List<Integer> clipDurations,
            List<String> missingShotNos) {
        List<byte[]> audio = new ArrayList<>();
        for (ShortShot shot : shots) {
            int dur = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
            clipDurations.add(dur);
            ShortDubbingExample dex = new ShortDubbingExample();
            dex.createCriteria().andShotIdEqualTo(shot.getShotId()).andDeletedFlagEqualTo(0);
            dex.setOrderByClause("line_no asc");
            List<ShortDubbing> rows = shortDubbingMapper.selectByExample(dex);
            byte[] bytes = null;
            for (ShortDubbing row : rows) {
                if (row.getAudioAssetId() == null || row.getAudioAssetId().isBlank()) {
                    continue;
                }
                bytes = audioAssetBytes(row.getAudioAssetId());
                if (bytes != null) {
                    break;
                }
            }
            if (bytes == null) {
                if (shot.getDialogueText() != null && !shot.getDialogueText().isBlank()) {
                    missingShotNos.add("镜" + shot.getShotNo());
                }
                audio.add(new byte[0]);
            } else {
                audio.add(bytes);
            }
        }
        return audio;
    }

    private byte[] audioAssetBytes(String assetId) {
        ShortAssetExample ax = new ShortAssetExample();
        ax.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ax);
        if (asset == null || asset.getStorageKey() == null || asset.getStorageKey().isBlank()) {
            return null;
        }
        try {
            byte[] bytes = objectStorage.get(asset.getStorageKey());
            return bytes == null || bytes.length == 0 ? null : bytes;
        } catch (RuntimeException ex) {
            log.warn("compose dubbing fetch failed assetId={}", assetId, ex);
            return null;
        }
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

    private byte[] loadSelectedBgm(ShortEpisode ep) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(ep.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        String assetId = work == null ? ""
                : ShortJson.text(ShortJson.map(work.getComposeSettingJson()).get("bgm_asset_id"));
        if (assetId.isBlank()) {
            return null;
        }
        ShortAssetExample ax = new ShortAssetExample();
        ax.createCriteria().andAssetIdEqualTo(assetId).andEpisodeIdEqualTo(ep.getEpisodeId())
                .andAssetTypeEqualTo("AUDIO").andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ax);
        if (asset == null || asset.getStorageKey() == null || asset.getStorageKey().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "selected BGM asset is unavailable");
        }
        byte[] bytes = objectStorage.get(asset.getStorageKey());
        if (bytes == null || bytes.length == 0) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "selected BGM content is empty");
        }
        return bytes;
    }

    /**
     * MVP simple subs: burn dialogue (or short_subtitle rows) unless compose_setting
     * explicitly sets burn_subtitle_flag=0.
     */
    private String loadSubtitleSrt(ShortEpisode ep, List<ShortShot> shots) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(ep.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        Map<String, Object> setting = work == null ? Map.of() : ShortJson.map(work.getComposeSettingJson());
        Integer burnFlag = ShortJson.integer(setting.get("burn_subtitle_flag"), 1);
        if (burnFlag != null && burnFlag == 0) {
            return null;
        }
        ShortSubtitleExample sx = new ShortSubtitleExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("subtitle_no asc");
        List<ShortSubtitle> rows = shortSubtitleMapper.selectByExample(sx);
        return SubtitleSrtBuilder.fromCues(SubtitleSrtBuilder.cuesForEpisode(rows, shots));
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
        try {
            objectStorage.put(objectKey, bytes, mime);
        } catch (LinkageError ex) {
            // A storage SDK binary mismatch must not abandon RUNNING or look like
            // a slow model. Keep fatal VM errors outside this narrow boundary.
            log.error("storage dependency failure taskId={} errorType={}", taskId, ex.getClass().getName());
            throw new BizException("STORAGE_UNAVAILABLE", "素材存储服务不可用，任务未完成，请联系管理员", ex);
        }
        asset.setStorageKey(objectKey);
        asset.setFileUrl("/bgssai/user/assets/" + asset.getAssetId());
        asset.setFileSizeByte(bytes.length);
        if (result.getWidth() != null) {
            asset.setWidth(result.getWidth());
        }
        if (result.getHeight() != null) {
            asset.setHeight(result.getHeight());
        }
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

    private AiProviderRequest baseRequest(AiModelConfig model, String ability, String operation) {
        AiProviderRequest request = new AiProviderRequest();
        request.setAbilityType(ability);
        request.setOperation(operation);
        request.setModel(model);
        request.setParams(new LinkedHashMap<>());
        return request;
    }

    private AiProviderResult execute(AiModelConfig model, AiProviderRequest request) {
        AiProvider provider = aiProviderRegistry.resolve(model);
        AiProviderResult result = provider.execute(request);
        com.bgssai.shortai.production.support.MediaMetadata.verify(result, request.getAbilityType());
        if (result != null && result.getDurationSec() != null) {
            if ("PER_SECOND".equals(model.getPriceUnit())) result.setUsageAmount(result.getDurationSec());
            if ("PER_MINUTE".equals(model.getPriceUnit())) result.setUsageAmount(Math.max(1, (result.getDurationSec() + 59) / 60));
        }
        int usage = result == null || result.getUsageAmount() == null ? 1 : Math.max(1, result.getUsageAmount());
        executionUsage.set(executionUsage.get() + usage);
        return result;
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

    /**
     * 给视频请求挂参考帧。挂哪一张由镜头的 frame_link_mode 决定：
     * INHERIT_PREV 用上一镜的尾帧当本镜开头依据，BOOKEND 再把本镜尾帧一并送出去让模型补中间运动。
     * <p>
     * 两处刻意不做兜底：跨场次不接（换场本来就该跳），模型不支持首尾帧时直接失败而不是
     * 悄悄只送首帧——「以为按首尾帧生成、实际只用了首帧」出来的片子不报错、只是接不好，
     * 创作者会去怀疑提示词、反复重抽，唯独想不到尾帧根本没送出去。
     */
    private void attachFrames(ShortShot shot, AiProviderRequest request, AiModelConfig model) {
        String mode = shot.getFrameLinkMode() == null ? "NONE" : shot.getFrameLinkMode();
        String firstFrameAssetId = shot.getFirstFrameAssetId();
        if ("INHERIT_PREV".equals(mode)) {
            ShortShot prev = frameLinkService.previousLinkableShot(shot);
            if (prev != null && prev.getLastFrameAssetId() != null
                    && !prev.getLastFrameAssetId().isBlank()) {
                firstFrameAssetId = prev.getLastFrameAssetId();
                log.info("shot {} inherits last frame from shot {}", shot.getShotNo(), prev.getShotNo());
            }
        }
        attachFrameAsset(firstFrameAssetId, request, "image_url", "image_base64", "image_mime");
        if (!"BOOKEND".equals(mode)) {
            return;
        }
        if (!frameLinkService.supportsLastFrame(model)) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "所选视频模型不支持首尾帧（"
                            + com.bgssai.shortai.aimodel.ProductModelAlias.forStored("VIDEO", model.getModelCode()).name()
                            + "），请换模型或改用「接上一镜」");
        }
        attachFrameAsset(shot.getLastFrameAssetId(), request,
                "last_image_url", "last_image_base64", "last_image_mime");
    }

    private void attachFrameAsset(String assetId, AiProviderRequest request, String urlKey,
            String base64Key, String mimeKey) {
        if (assetId == null || assetId.isBlank()) {
            return;
        }
        ShortAssetExample ax = new ShortAssetExample();
        ax.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
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
        if (TencentTokenHubAiProvider.CODE.equals(request.getModel().getProviderCode())) {
            try {
                request.getParams().put(urlKey,
                        objectStorage.presignGet(frame.getStorageKey(), 3600).downloadUrl());
                return;
            } catch (RuntimeException ex) {
                log.warn("presign frame failed, falling back to base64 assetId={}", frame.getAssetId(), ex);
            }
        }
        byte[] bytes = objectStorage.get(frame.getStorageKey());
        if (bytes == null || bytes.length == 0) {
            return;
        }
        request.getParams().put(base64Key, Base64.getEncoder().encodeToString(bytes));
        request.getParams().put(mimeKey, frame.getMimeType());
    }

    /**
     * 视频出来后顺手抽最后一帧做尾帧。抽帧失败**不**让整个任务失败：视频已经生成、钱已经花了，
     * 判失败会触发重试，等于为一张抽不出来的帧再付一次视频钱。缺尾帧只是下一镜接不上，
     * 界面会照实说「上一镜尚无尾帧」。
     */
    private String extractLastFrame(ShortShot shot, AiProviderResult result, ShortAiTask task) {
        try {
            byte[] video = result.getBinaryContent();
            if (video == null || video.length == 0) {
                return null;
            }
            byte[] png = LastFrameExtractor.extract(video);
            ShortAsset asset = new ShortAsset();
            asset.setAssetId(ShortIds.id("a-"));
            asset.setUserId(shot.getUserId());
            asset.setWorkId(shot.getWorkId());
            asset.setEpisodeId(shot.getEpisodeId());
            asset.setShotId(shot.getShotId());
            asset.setAssetType("IMAGE");
            asset.setMimeType("image/png");
            String objectKey = "short/" + activeEnv() + "/" + shot.getUserId() + "/" + shot.getWorkId()
                    + "/" + asset.getAssetId() + ".png";
            objectStorage.put(objectKey, png, "image/png");
            asset.setStorageKey(objectKey);
            asset.setFileUrl("/bgssai/user/assets/" + asset.getAssetId());
            asset.setFileSizeByte(png.length);
            asset.setWidth(1080);
            asset.setHeight(1920);
            asset.setSourceTaskId(task.getTaskId());
            asset.setSelectedFlag(0);
            asset.setDraftFlag(0);
            asset.setDeletedFlag(0);
            asset.setCreatedTime(new Date());
            shortAssetMapper.insertSelective(asset);
            return asset.getAssetId();
        } catch (RuntimeException ex) {
            log.warn("last frame extract skipped shotId={} reason={}", shot.getShotId(), ex.getMessage());
            return null;
        }
    }

    /** 单独出一张尾帧图：本镜还没出视频、但想先定「往哪收」时用。 */
    private void finishLastFrame(ShortAiTask task, Map<String, Object> req, AiModelConfig model) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "task target no longer available");
        }
        AiProviderRequest request = baseRequest(model, "IMAGE", "LAST_FRAME");
        request.getParams().put("title", "镜 " + shot.getShotNo() + " 尾帧");
        request.getParams().put("aspect_ratio", "9:16");
        String prompt = ShortJson.text(req.get("last_frame_prompt"));
        request.setPrompt(prompt.isBlank() ? shotPrompt(shot, "IMAGE") : prompt);
        AiProviderResult result = execute(model, request);
        ShortAsset asset = saveBinary(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(),
                shot.getShotId(), "IMAGE", result, task.getTaskId(), false);
        ShortShot patch = new ShortShot();
        patch.setLastFrameAssetId(asset.getAssetId());
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
        frameLinkService.markNextStale(shot);
        patchTaskResult(task, Map.of("last_frame_asset_id", asset.getAssetId()));
    }

    private void patchTaskResult(ShortAiTask task, Map<String, Object> result) {
        ShortAiTask patch = new ShortAiTask();
        patch.setResultJson(ShortJson.toJson(result));
        patch.setUpdatedTime(new Date());
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("RUNNING");
        shortAiTaskMapper.updateByExampleSelective(patch, up);
    }

    private static List<Map<String, Object>> jsonList(String content) {
        if (content == null) {
            return new ArrayList<>();
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstLine = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                text = text.substring(firstLine + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return ShortJson.list(text);
    }

    private ShortEpisode episode(String episodeId) {
        if (episodeId == null) {
            return null;
        }
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        return shortEpisodeMapper.selectOneByExample(ex);
    }

    private void noteComposeFailed(ShortAiTask task) {
        if (finishedFilmLifecycle == null || task == null || !"COMPOSE".equals(task.getTargetType())) {
            return;
        }
        try {
            finishedFilmLifecycle.onComposeTaskFailed(task.getEpisodeId());
        } catch (RuntimeException ex) {
            log.warn("compose failure state update skipped episodeId={}", task.getEpisodeId(), ex);
        }
    }

    private void fail(ShortAiTask task, String code, String message) {
        ShortAiTask patch = new ShortAiTask();
        patch.setTaskStatus("FAILED");
        patch.setFailReasonCode(code);
        patch.setFailReason(TokenHubClientSupport.userFacingMessage(message));
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
        patch.setFailReason(TokenHubClientSupport.userFacingMessage(message));
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
