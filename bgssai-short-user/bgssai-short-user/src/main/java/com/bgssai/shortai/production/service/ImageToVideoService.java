package com.bgssai.shortai.production.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bgssai.shortai.aimodel.ProductModelAlias;
import com.bgssai.shortai.aiprovider.TokenHubClientSupport;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.aiprovider.ImageBytesValidator;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoClient;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.ContentDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.PollDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.PollKind;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.SubmitDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.SubmitKind;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProvider;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;
import com.bgssai.shortai.production.dto.ImageToVideoDtos.SubmitReq;
import com.bgssai.shortai.production.support.ImageToVideoPolicy;
import com.bgssai.shortai.production.support.MediaMetadata;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.storage.ObjectStorage;

/**
 * Upload or select a still, then queue a real image-conditioned video job.
 * Text-to-video is a different task and is never used as a fallback.
 */
@Service
public class ImageToVideoService {

    private static final Logger log = LoggerFactory.getLogger(ImageToVideoService.class);
    private static final Pattern CLIENT_ID = Pattern.compile("[A-Za-z0-9_-]{8,64}");
    private static final Set<Integer> FRAMES = Set.of(9, 17, 25, 33);
    private static final long AMBIGUOUS_AFTER_MS = 180_000L;

    private final Set<String> inflight = ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.atomic.AtomicBoolean pollWarned = new java.util.concurrent.atomic.AtomicBoolean();
    private MediaCheck mediaCheck = MediaMetadata::verify;

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ImageConditionedVideoClient videoClient;
    @Autowired
    private AiModelConfigMapper aiModelConfigMapper;
    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private ShortAssetMapper shortAssetMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ObjectStorage objectStorage;
    @Autowired
    private ShortTaskSettlementService settlement;
    @Autowired
    private QuotaConsumeService quotaConsumeService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private Environment environment;
    @Autowired(required = false)
    private PaidOrderGrantService paidOrderGrantService;
    @Autowired(required = false)
    private ShotGenerateOutcome shotGenerateOutcome;
    @Autowired(required = false)
    private FinishedFilmLifecycle finishedFilmLifecycle;

    void setMediaCheck(MediaCheck mediaCheck) {
        this.mediaCheck = mediaCheck == null ? MediaMetadata::verify : mediaCheck;
    }

    public boolean readyForOfficial() {
        AiModelConfig model = loadModel();
        return model != null && videoClient.ready(model);
    }

    /**
     * Queue one official shot clip on the CPU image-to-video lane.
     * Does not clear the shot's current video. The caller sets VIDEO_GENERATING.
     */
    public ShortAiTask enqueueForShot(ShortShot shot, String assetId, String prompt, int costCent) {
        return enqueueForShot(shot, assetId, prompt, costCent, null);
    }

    public ShortAiTask enqueueForShot(ShortShot shot, String assetId, String prompt, int costCent, String batchId) {
        if (shot == null || assetId == null || assetId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "图生视频需要一张真实照片。");
        }
        OwnedImage image = loadStored(shot.getUserId(), assetId);
        AiModelConfig model = loadModel();
        if (model == null || !videoClient.ready(model)) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, ImageConditionedVideoProtocol.UNAVAILABLE);
        }
        String text = prompt == null ? "" : prompt.trim();
        if (text.isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "提示词需要 1 到 2000 个字符。");
        }
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }
        Params params = new Params(text, 384, 256, 25, 8, 20, 42);
        ShortAiTask task = new ShortAiTask();
        task.setTaskId(ShortIds.id("t-"));
        task.setBatchId(batchId == null || batchId.isBlank() ? ShortIds.id("b-") : batchId.trim());
        task.setUserId(shot.getUserId());
        task.setWorkId(shot.getWorkId());
        task.setEpisodeId(shot.getEpisodeId());
        task.setTaskType("VIDEO");
        task.setTaskStatus("QUEUED");
        task.setTargetType(ImageToVideoPolicy.TARGET);
        task.setTargetId(shot.getShotId());
        task.setModelCode(model.getModelCode());
        Map<String, Object> body = requestBody(shot.getShotId(), fingerprint(image.asset().getAssetId(), params),
                new OwnedImage(image.asset(), image.bytes(), shot.getWorkId(), shot.getEpisodeId(), shot), params);
        body.put("official_shot", 1);
        task.setRequestJson(ShortJson.toJson(body));
        task.setProgressPercent(0);
        task.setRetryCount(0);
        task.setAutoNextFlag(0);
        task.setCostCent(Math.max(0, costCent));
        Date now = new Date();
        task.setQueuedTime(now);
        task.setCreatedTime(now);
        task.setUpdatedTime(now);
        shortAiTaskMapper.insertSelective(task);
        return task;
    }

    public ApiResponse capability() {
        workAccessService.requireUser();
        AiModelConfig model = loadModel();
        boolean available = model != null && videoClient.ready(model);
        Map<String, Object> result = limits(model);
        result.put("available", available ? 1 : 0);
        result.put("reason", available ? "" : ImageConditionedVideoProtocol.UNAVAILABLE);
        return Responses.ok(result);
    }

    public ApiResponse upload(MultipartFile file) {
        String userId = workAccessService.requireUser();
        if (file == null || file.isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请上传 PNG 或 JPEG 图片。");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException ex) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "图片无法读取。");
        }
        ImageBytesValidator.ImageInfo info = ImageBytesValidator.inspect(bytes);
        ShortAsset asset = storeImage(userId, null, null, null, info, bytes);
        return Responses.ok(imageView(asset));
    }

    public ApiResponse submit(SubmitReq req) {
        String userId = workAccessService.requireUser();
        if (paidOrderGrantService != null) {
            paidOrderGrantService.ensureFreeTier(userId);
        }
        Params params = params(req);
        String clientId = req == null ? "" : ShortJson.text(req.getClientRequestId()).trim();
        if (!CLIENT_ID.matcher(clientId).matches()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请提供 8 到 64 位的请求号。");
        }
        OwnedImage image = resolveImage(userId, req);
        String fingerprint = fingerprint(image.asset().getAssetId(), params);
        ShortAiTask existing = findByClient(userId, clientId);
        if (existing != null) {
            String previous = ShortJson.text(ShortJson.map(existing.getRequestJson()).get("fingerprint"));
            if (!fingerprint.equals(previous)) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "同一请求号的图片或参数不一致。");
            }
            return Responses.ok(view(existing));
        }
        AiModelConfig model = loadModel();
        if (model == null || !videoClient.ready(model)) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, ImageConditionedVideoProtocol.UNAVAILABLE);
        }
        if (activeCount() > 0) {
            throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED, ImageConditionedVideoProtocol.BUSY);
        }
        int cost = costOf(model, params);
        if (quotaConsumeService != null) {
            quotaConsumeService.assertAffordable(userId, cost);
        }
        ShortAiTask task = new ShortAiTask();
        task.setTaskId(ShortIds.id("t-"));
        task.setBatchId(ShortIds.id("b-"));
        task.setUserId(userId);
        task.setWorkId(image.workId());
        task.setEpisodeId(image.episodeId());
        task.setTaskType("VIDEO");
        task.setTaskStatus("QUEUED");
        task.setTargetType(ImageToVideoPolicy.TARGET);
        task.setTargetId(clientId);
        task.setModelCode(model.getModelCode());
        task.setRequestJson(ShortJson.toJson(requestBody(clientId, fingerprint, image, params)));
        task.setProgressPercent(0);
        task.setRetryCount(0);
        task.setAutoNextFlag(0);
        task.setCostCent(cost);
        Date now = new Date();
        task.setQueuedTime(now);
        task.setCreatedTime(now);
        task.setUpdatedTime(now);
        shortAiTaskMapper.insertSelective(task);
        return Responses.ok(view(task));
    }

    public ApiResponse status(String taskId) {
        return Responses.ok(view(requireOwn(taskId)));
    }

    public void kickoff(ShortAiTask queued) {
        if (queued == null || !inflight.add(queued.getTaskId())) {
            return;
        }
        try {
            ShortAiTask task = reload(queued.getId());
            if (task == null || !"RUNNING".equals(task.getTaskStatus())) {
                return;
            }
            Map<String, Object> req = ShortJson.map(task.getRequestJson());
            if (!ShortJson.text(req.get("provider_job_id")).isBlank()) {
                return;
            }
            AiModelConfig model = loadModel();
            if (model == null || !videoClient.ready(model)) {
                fail(task, "I2V_UNAVAILABLE", ImageConditionedVideoProtocol.UNAVAILABLE);
                return;
            }
            OwnedImage image = loadStored(task.getUserId(), ShortJson.text(req.get("asset_id")));
            req.put("submit_started_at_ms", System.currentTimeMillis());
            writeReq(task, req, null);
            SubmitDecision decision = videoClient.submit(model, image.bytes(), image.asset().getMimeType(),
                    ShortJson.text(req.get("prompt")),
                    ShortJson.integer(req.get("width"), 384),
                    ShortJson.integer(req.get("height"), 256),
                    ShortJson.integer(req.get("num_frames"), 25),
                    ShortJson.integer(req.get("fps"), 8),
                    ShortJson.integer(req.get("steps"), 20),
                    ShortJson.integer(req.get("seed"), 42));
            if (decision.kind() == SubmitKind.ACCEPTED) {
                markAccepted(task, req, decision.jobId());
                return;
            }
            fail(reload(task.getId()), decision.taskCode(), decision.userMessage());
        } catch (BizException ex) {
            ShortAiTask fresh = reload(queued.getId());
            if (fresh != null && "RUNNING".equals(fresh.getTaskStatus())
                    && ShortJson.text(ShortJson.map(fresh.getRequestJson()).get("provider_job_id")).isBlank()) {
                fail(fresh, imageFailCode(ex), safeFailure(ex));
            }
        } finally {
            inflight.remove(queued.getTaskId());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void pollImageToVideo() {
        try {
            if (shortAiTaskMapper == null) {
                return;
            }
            ShortAiTaskExample ex = new ShortAiTaskExample();
            ex.createCriteria().andTaskStatusEqualTo("RUNNING").andTargetTypeEqualTo(ImageToVideoPolicy.TARGET);
            ex.setOrderByClause("id asc");
            List<ShortAiTask> rows = shortAiTaskMapper.selectByExample(ex);
            if (rows == null) {
                return;
            }
            pollWarned.set(false);
            long now = System.currentTimeMillis();
            for (ShortAiTask task : rows) {
                pollOne(task, now);
            }
        } catch (RuntimeException ex) {
            if (pollWarned.compareAndSet(false, true)) {
                log.warn("image-to-video poll skipped type={}", ex.getClass().getSimpleName());
            }
        }
    }

    private void pollOne(ShortAiTask task, long now) {
        if (task == null || !inflight.add(task.getTaskId())) {
            return;
        }
        try {
            ShortAiTask fresh = reload(task.getId());
            if (fresh == null || !"RUNNING".equals(fresh.getTaskStatus())) {
                return;
            }
            Map<String, Object> req = ShortJson.map(fresh.getRequestJson());
            String jobId = ShortJson.text(req.get("provider_job_id")).trim();
            if (jobId.isBlank()) {
                long started = longValue(req.get("submit_started_at_ms"));
                if (started <= 0 && fresh.getStartedTime() != null) {
                    started = fresh.getStartedTime().getTime();
                }
                if (started > 0 && now - started > AMBIGUOUS_AFTER_MS) {
                    fail(fresh, "I2V_AMBIGUOUS", ImageConditionedVideoProtocol.AMBIGUOUS);
                }
                return;
            }
            long nextPoll = longValue(req.get("next_poll_at_ms"));
            if (nextPoll > now) {
                return;
            }
            long accepted = longValue(req.get("accepted_at_ms"));
            if (accepted <= 0) {
                accepted = fresh.getStartedTime() == null ? now : fresh.getStartedTime().getTime();
            }
            if (now - accepted > ImageConditionedVideoProtocol.DEADLINE_SEC * 1000L) {
                fail(fresh, "I2V_TIMEOUT", ImageConditionedVideoProtocol.TIMEOUT);
                return;
            }
            AiModelConfig model = loadModel();
            if (model == null || !videoClient.callable(model)) {
                fail(fresh, "I2V_UNAVAILABLE", ImageConditionedVideoProtocol.UNAVAILABLE);
                return;
            }
            PollDecision polled = videoClient.poll(model, jobId);
            int attempt = ShortJson.integer(req.get("poll_attempt"), 0) + 1;
            if (polled.kind() == PollKind.RUNNING || polled.kind() == PollKind.TRANSIENT) {
                req.put("poll_attempt", attempt);
                req.put("next_poll_at_ms", now + ImageConditionedVideoProtocol.pollDelayMs(attempt));
                writeReq(fresh, req, Math.min(90, 40 + attempt));
                return;
            }
            if (polled.kind() != PollKind.COMPLETED) {
                fail(fresh, polled.taskCode(), polled.userMessage());
                return;
            }
            ContentDecision content = videoClient.fetch(model, jobId);
            if (content.kind() != ImageConditionedVideoProtocol.ContentKind.READY) {
                if (content.kind() == ImageConditionedVideoProtocol.ContentKind.FAILED) {
                    fail(fresh, content.taskCode(), content.userMessage());
                    return;
                }
                req.put("poll_attempt", attempt);
                req.put("next_poll_at_ms", now + ImageConditionedVideoProtocol.pollDelayMs(attempt));
                writeReq(fresh, req, Math.min(90, 40 + attempt));
                return;
            }
            complete(fresh, model, req, content.body());
        } catch (BizException ex) {
            ShortAiTask fresh = reload(task.getId());
            if (fresh != null && "RUNNING".equals(fresh.getTaskStatus())) {
                fail(fresh, "I2V_FAILED", safeFailure(ex));
            }
        } catch (RuntimeException ex) {
            log.warn("image-to-video poll failed taskId={} type={}", task.getTaskId(), ex.getClass().getSimpleName());
        } finally {
            inflight.remove(task.getTaskId());
        }
    }

    private void complete(ShortAiTask task, AiModelConfig model, Map<String, Object> req, byte[] bytes) {
        ShortAsset existing = findResult(task.getTaskId());
        ShortAsset video = existing;
        if (video == null) {
            AiProviderResult probed = AiProviderResult.binary(bytes, "video/mp4");
            mediaCheck.verify(probed, "VIDEO");
            if (probed.getDurationSec() == null || probed.getDurationSec() <= 0
                    || probed.getWidth() == null || probed.getHeight() == null) {
                throw new BizException(CommonErrorCode.UPSTREAM_ERROR, ImageConditionedVideoProtocol.BAD_RESULT);
            }
            video = storeVideo(task, probed);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("source_asset_id", ShortJson.text(req.get("asset_id")));
            result.put("video_asset_id", video.getAssetId());
            result.put("ai_generated", true);
            result.put("width", probed.getWidth());
            result.put("height", probed.getHeight());
            result.put("num_frames", ShortJson.integer(req.get("num_frames"), 0));
            result.put("fps", ShortJson.integer(req.get("fps"), 0));
            result.put("duration_sec", probed.getDurationSec());
            ShortAiTask patch = new ShortAiTask();
            patch.setResultJson(ShortJson.toJson(result));
            patch.setUpdatedTime(new Date());
            ShortAiTaskExample up = new ShortAiTaskExample();
            up.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("RUNNING");
            shortAiTaskMapper.updateByExampleSelective(patch, up);
            Integer playableSec = com.bgssai.shortai.production.support.PlayableVideoDuration.floorSeconds(bytes);
            attachShot(task, req, video.getAssetId(), playableSec != null ? playableSec : probed.getDurationSec());
        }
        if (settlement != null) {
            settlement.settle(task, model, 1);
        }
    }

    private void markAccepted(ShortAiTask task, Map<String, Object> req, String jobId) {
        req.put("provider_job_id", jobId);
        req.put("accepted_at_ms", System.currentTimeMillis());
        req.put("next_poll_at_ms", System.currentTimeMillis() + ImageConditionedVideoProtocol.pollDelayMs(0));
        req.put("poll_attempt", 0);
        ShortAiTask patch = new ShortAiTask();
        patch.setRequestJson(ShortJson.toJson(req));
        patch.setTaskStatus("RUNNING");
        patch.setFailReason("");
        patch.setFailReasonCode("");
        patch.setProgressPercent(40);
        patch.setUpdatedTime(new Date());
        ShortAiTaskExample running = new ShortAiTaskExample();
        running.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("RUNNING");
        if (shortAiTaskMapper.updateByExampleSelective(patch, running) == 1) {
            return;
        }
        ShortAiTaskExample ambiguous = new ShortAiTaskExample();
        ambiguous.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("FAILED")
                .andFailReasonCodeEqualTo("I2V_AMBIGUOUS");
        if (shortAiTaskMapper.updateByExampleSelective(patch, ambiguous) == 1) {
            return;
        }
        ShortAiTask keep = new ShortAiTask();
        keep.setRequestJson(ShortJson.toJson(req));
        keep.setUpdatedTime(new Date());
        ShortAiTaskExample any = new ShortAiTaskExample();
        any.createCriteria().andIdEqualTo(task.getId());
        shortAiTaskMapper.updateByExampleSelective(keep, any);
    }

    private void fail(ShortAiTask task, String code, String message) {
        if (task == null) {
            return;
        }
        ShortAiTask patch = new ShortAiTask();
        patch.setTaskStatus("FAILED");
        patch.setFailReasonCode(code == null || code.isBlank() ? "I2V_FAILED" : code);
        patch.setFailReason(cleanReason(message));
        patch.setFinishedTime(new Date());
        patch.setUpdatedTime(new Date());
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("RUNNING");
        if (shortAiTaskMapper.updateByExampleSelective(patch, up) == 1 && shotGenerateOutcome != null) {
            shotGenerateOutcome.restoreBoundMedia(task);
        }
    }

    private Map<String, Object> view(ShortAiTask task) {
        Map<String, Object> req = ShortJson.map(task.getRequestJson());
        Map<String, Object> result = ImageToVideoPolicy.scrubResult(ShortJson.map(task.getResultJson()));
        ProductModelAlias.Face face = ProductModelAlias.forStored("VIDEO", task.getModelCode());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("task_id", task.getTaskId());
        item.put("task_type", "VIDEO");
        item.put("task_status", task.getTaskStatus());
        item.put("task_status_name", com.bgssai.shortai.production.support.GenerationTaskStatus.statusName(task.getTaskStatus()));
        item.put("target_type", task.getTargetType());
        item.put("model_code", face.code());
        item.put("display_name", face.name());
        item.put("ai_generated", 1);
        item.put("progress_percent", task.getProgressPercent() == null ? 0 : task.getProgressPercent());
        item.put("fail_reason_code", task.getFailReasonCode());
        item.put("fail_reason", "FAILED".equals(task.getTaskStatus()) ? task.getFailReason() : "");
        item.put("source_asset_id", firstText(result.get("source_asset_id"), req.get("asset_id")));
        item.put("video_asset_id", ShortJson.text(result.get("video_asset_id")));
        item.put("prompt", ShortJson.text(req.get("prompt")));
        item.put("width", ShortJson.integer(first(result.get("width"), req.get("width")), 0));
        item.put("height", ShortJson.integer(first(result.get("height"), req.get("height")), 0));
        item.put("num_frames", ShortJson.integer(first(result.get("num_frames"), req.get("num_frames")), 0));
        item.put("fps", ShortJson.integer(first(result.get("fps"), req.get("fps")), 0));
        item.put("duration_sec", ShortJson.integer(result.get("duration_sec"), 0));
        item.put("cost_cent", task.getCostCent() == null ? 0 : task.getCostCent());
        item.put("cancellable_flag", ImageToVideoPolicy.hasProviderJob(task) || !"QUEUED".equals(task.getTaskStatus()) ? 0 : 1);
        boolean retryable = ("FAILED".equals(task.getTaskStatus()) || "CANCELED".equals(task.getTaskStatus()))
                && !ImageToVideoPolicy.hasProviderJob(task) && !"I2V_AMBIGUOUS".equals(task.getFailReasonCode());
        item.put("retryable_flag", retryable ? 1 : 0);
        String sourceId = ShortJson.text(item.get("source_asset_id"));
        String videoId = ShortJson.text(item.get("video_asset_id"));
        if (!sourceId.isBlank()) {
            item.put("source_preview_url", preview(sourceId));
        }
        if (!videoId.isBlank()) {
            item.put("video_preview_url", preview(videoId));
            item.put("video_download_url", preview(videoId));
        }
        return item;
    }

    private Map<String, Object> limits(AiModelConfig model) {
        int price = model == null || model.getUnitPriceCent() == null ? 0 : model.getUnitPriceCent();
        String unit = model == null || model.getPriceUnit() == null ? "PER_CALL" : model.getPriceUnit();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model_code", "bgssai-video");
        result.put("display_name", "BGSSAI Video");
        result.put("ai_generated", 1);
        result.put("max_bytes", ImageBytesValidator.MAX_BYTES);
        result.put("max_pixels", ImageBytesValidator.MAX_PIXELS);
        result.put("width_min", 128);
        result.put("width_max", 384);
        result.put("width_step", 32);
        result.put("width_default", 384);
        result.put("height_min", 128);
        result.put("height_max", 256);
        result.put("height_step", 32);
        result.put("height_default", 256);
        result.put("frame_list", List.of(9, 17, 25, 33));
        result.put("frames_default", 25);
        result.put("fps_min", 1);
        result.put("fps_max", 24);
        result.put("fps_default", 8);
        result.put("steps_min", 10);
        result.put("steps_max", 30);
        result.put("steps_default", 20);
        result.put("prompt_max", 2000);
        result.put("stage_timeout_sec", ImageConditionedVideoProtocol.STAGE_TIMEOUT_SEC);
        result.put("deadline_sec", ImageConditionedVideoProtocol.DEADLINE_SEC);
        result.put("observed_run_sec", ImageConditionedVideoProtocol.OBSERVED_RUN_SEC);
        result.put("price_cent", price);
        result.put("price_unit", unit);
        return result;
    }

    private OwnedImage resolveImage(String userId, SubmitReq req) {
        String shotId = req == null ? "" : ShortJson.text(req.getShotId()).trim();
        String assetId = req == null ? "" : ShortJson.text(req.getAssetId()).trim();
        ShortShot shot = shotId.isBlank() ? null : requireShot(userId, shotId);
        if (assetId.isBlank() && shot != null) {
            assetId = shot.getFirstFrameAssetId() == null ? "" : shot.getFirstFrameAssetId();
        }
        if (assetId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请上传或选择一张图片。");
        }
        OwnedImage image = loadStored(userId, assetId);
        String workId = req == null ? "" : ShortJson.text(req.getWorkId()).trim();
        String episodeId = req == null ? "" : ShortJson.text(req.getEpisodeId()).trim();
        if (shot != null) {
            if (!workId.isBlank() && !workId.equals(shot.getWorkId())) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "镜头与作品不一致。");
            }
            if (!episodeId.isBlank() && !episodeId.equals(shot.getEpisodeId())) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "镜头与分集不一致。");
            }
            workId = shot.getWorkId();
            episodeId = shot.getEpisodeId();
        } else {
            if (!workId.isBlank()) {
                workAccessService.requireWork(workId);
            }
            if (!episodeId.isBlank()) {
                workAccessService.requireEpisode(episodeId);
            }
        }
        return new OwnedImage(image.asset(), image.bytes(), blankToNull(workId), blankToNull(episodeId), shot);
    }

    private OwnedImage loadStored(String userId, String assetId) {
        ShortAsset asset = requireAsset(userId, assetId);
        if (!"IMAGE".equals(asset.getAssetType())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请选择图片素材。");
        }
        String key = asset.getStorageKey();
        if (externalImageKey(key)) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "不能从外部地址读取图片。");
        }
        byte[] bytes = objectStorage.get(key);
        ImageBytesValidator.ImageInfo info = ImageBytesValidator.inspect(bytes);
        asset.setMimeType(info.mime());
        asset.setWidth(info.width());
        asset.setHeight(info.height());
        return new OwnedImage(asset, bytes, asset.getWorkId(), asset.getEpisodeId(), null);
    }

    private ShortAsset storeImage(String userId, String workId, String episodeId, String shotId,
            ImageBytesValidator.ImageInfo info, byte[] bytes) {
        ShortAsset asset = new ShortAsset();
        asset.setAssetId(ShortIds.id("a-"));
        asset.setUserId(userId);
        asset.setWorkId(workId);
        asset.setEpisodeId(episodeId);
        asset.setShotId(shotId);
        asset.setAssetType("IMAGE");
        asset.setMimeType(info.mime());
        asset.setWidth(info.width());
        asset.setHeight(info.height());
        String key = "short/" + env() + "/" + userId + "/" + (workId == null ? "library" : workId)
                + "/" + asset.getAssetId() + "." + info.extension();
        objectStorage.put(key, bytes, info.mime());
        asset.setStorageKey(key);
        asset.setFileUrl("/bgssai/user/assets/" + asset.getAssetId());
        asset.setFileSizeByte(bytes.length);
        asset.setSelectedFlag(0);
        asset.setDraftFlag(0);
        asset.setDeletedFlag(0);
        asset.setCreatedTime(new Date());
        shortAssetMapper.insertSelective(asset);
        return asset;
    }

    private ShortAsset storeVideo(ShortAiTask task, AiProviderResult probed) {
        ShortAsset asset = new ShortAsset();
        asset.setAssetId(ShortIds.id("a-"));
        asset.setUserId(task.getUserId());
        asset.setWorkId(task.getWorkId());
        asset.setEpisodeId(task.getEpisodeId());
        asset.setShotId(ShortJson.text(ShortJson.map(task.getRequestJson()).get("shot_id")));
        if (asset.getShotId() != null && asset.getShotId().isBlank()) {
            asset.setShotId(null);
        }
        asset.setAssetType("VIDEO");
        asset.setMimeType("video/mp4");
        asset.setWidth(probed.getWidth());
        asset.setHeight(probed.getHeight());
        asset.setDurationSec(probed.getDurationSec());
        String work = task.getWorkId() == null ? "library" : task.getWorkId();
        String key = "short/" + env() + "/" + task.getUserId() + "/" + work + "/" + asset.getAssetId() + ".mp4";
        objectStorage.put(key, probed.getBinaryContent(), "video/mp4");
        asset.setStorageKey(key);
        asset.setFileUrl("/bgssai/user/assets/" + asset.getAssetId());
        asset.setFileSizeByte(probed.getBinaryContent().length);
        asset.setSourceTaskId(task.getTaskId());
        asset.setSelectedFlag(1);
        asset.setDraftFlag(0);
        asset.setDeletedFlag(0);
        asset.setCreatedTime(new Date());
        shortAssetMapper.insertSelective(asset);
        return asset;
    }

    private void attachShot(ShortAiTask task, Map<String, Object> req, String videoAssetId, Integer actualDurationSec) {
        String shotId = ShortJson.text(req.get("shot_id"));
        if (shotId.isBlank() || shortShotMapper == null) {
            return;
        }
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(shotId).andUserIdEqualTo(task.getUserId()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null || "LOCKED".equals(shot.getShotStatus())) {
            return;
        }
        ShortShot patch = new ShortShot();
        patch.setVideoAssetId(videoAssetId);
        if (actualDurationSec != null && actualDurationSec > 0
                && (shot.getShotDurationSec() == null || shot.getShotDurationSec() > actualDurationSec)) {
            patch.setShotDurationSec(actualDurationSec);
        }
        patch.setShotStatus("VIDEO_READY");
        patch.setStaleFlag(0);
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId()).andShotStatusNotEqualTo("LOCKED");
        int updated = shortShotMapper.updateByExampleSelective(patch, up);
        if (updated > 0 && finishedFilmLifecycle != null) {
            finishedFilmLifecycle.expireAfterUpstreamChange(shot.getEpisodeId());
        }
    }

    private ShortAiTask requireOwn(String taskId) {
        String userId = workAccessService.requireUser();
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTaskIdEqualTo(taskId == null ? "" : taskId.trim());
        ShortAiTask task = shortAiTaskMapper.selectOneByExample(ex);
        if (task == null || !userId.equals(task.getUserId()) || !ImageToVideoPolicy.TARGET.equals(task.getTargetType())) {
            throw new ForbiddenException("task not found");
        }
        return task;
    }

    private ShortAsset requireAsset(String userId, String assetId) {
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ex);
        if (asset == null || !userId.equals(asset.getUserId())) {
            throw new ForbiddenException("asset not found");
        }
        return asset;
    }

    private ShortShot requireShot(String userId, String shotId) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(shotId).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null || !userId.equals(shot.getUserId())) {
            throw new ForbiddenException("shot not found");
        }
        return shot;
    }

    private ShortAiTask findByClient(String userId, String clientId) {
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andUserIdEqualTo(userId).andTargetTypeEqualTo(ImageToVideoPolicy.TARGET)
                .andTargetIdEqualTo(clientId);
        ex.setOrderByClause("id desc");
        return shortAiTaskMapper.selectOneByExample(ex);
    }

    private long activeCount() {
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTargetTypeEqualTo(ImageToVideoPolicy.TARGET).andTaskStatusIn(List.of("QUEUED", "RUNNING"));
        return shortAiTaskMapper.countByExample(ex);
    }

    private ShortAsset findResult(String taskId) {
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andSourceTaskIdEqualTo(taskId).andAssetTypeEqualTo("VIDEO").andDeletedFlagEqualTo(0);
        return shortAssetMapper.selectOneByExample(ex);
    }

    private AiModelConfig loadModel() {
        if (aiModelConfigMapper == null) {
            return null;
        }
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria().andModelIdEqualTo(ImageConditionedVideoProvider.MODEL_ID).andDeletedFlagEqualTo(0);
        return aiModelConfigMapper.selectOneByExample(ex);
    }

    private ShortAiTask reload(Long id) {
        return id == null ? null : shortAiTaskMapper.selectByPrimaryKey(id);
    }

    private void writeReq(ShortAiTask task, Map<String, Object> req, Integer progress) {
        ShortAiTask patch = new ShortAiTask();
        patch.setRequestJson(ShortJson.toJson(req));
        patch.setUpdatedTime(new Date());
        if (progress != null) {
            patch.setProgressPercent(progress);
        }
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("RUNNING");
        shortAiTaskMapper.updateByExampleSelective(patch, up);
    }

    private Map<String, Object> requestBody(String clientId, String fingerprint, OwnedImage image, Params params) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_request_id", clientId);
        body.put("fingerprint", fingerprint);
        body.put("asset_id", image.asset().getAssetId());
        if (image.shot() != null) {
            body.put("shot_id", image.shot().getShotId());
        }
        body.put("prompt", params.prompt());
        body.put("width", params.width());
        body.put("height", params.height());
        body.put("num_frames", params.frames());
        body.put("fps", params.fps());
        body.put("steps", params.steps());
        body.put("seed", params.seed());
        return body;
    }

    private Map<String, Object> imageView(ShortAsset asset) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("asset_id", asset.getAssetId());
        item.put("mime_type", asset.getMimeType());
        item.put("width", asset.getWidth());
        item.put("height", asset.getHeight());
        item.put("file_size_byte", asset.getFileSizeByte());
        item.put("file_url", asset.getFileUrl());
        item.put("preview_url", preview(asset.getAssetId()));
        return item;
    }

    private String preview(String assetId) {
        if (assetService == null || assetId == null || assetId.isBlank()) {
            return "";
        }
        try {
            String url = assetService.resolveUrl(assetId);
            return url == null ? "" : url;
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private Params params(SubmitReq req) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "body required");
        }
        String prompt = req.getPrompt() == null ? "" : req.getPrompt().trim();
        if (prompt.isEmpty() || prompt.length() > 2000) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "提示词需要 1 到 2000 个字符。");
        }
        int width = req.getWidth() == null ? 384 : req.getWidth();
        int height = req.getHeight() == null ? 256 : req.getHeight();
        int frames = req.getNumFrames() == null ? 25 : req.getNumFrames();
        int fps = req.getFps() == null ? 8 : req.getFps();
        int steps = req.getSteps() == null ? 20 : req.getSteps();
        int seed = req.getSeed() == null ? 42 : req.getSeed();
        if (!aligned(width, 128, 384) || !aligned(height, 128, 256) || !FRAMES.contains(frames)
                || fps < 1 || fps > 24 || steps < 10 || steps > 30) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "图生视频只接受宽 128-384、高 128-256（32 的倍数），帧数 9/17/25/33，帧率 1-24，步数 10-30。");
        }
        return new Params(prompt, width, height, frames, fps, steps, seed);
    }

    private static boolean externalImageKey(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }
        String folded = key.trim().toLowerCase(java.util.Locale.ROOT);
        return folded.startsWith("http://") || folded.startsWith("https://") || folded.startsWith("//")
                || folded.contains("://") || folded.startsWith("<svg") || folded.startsWith("data:");
    }

    private static boolean aligned(int value, int min, int max) {
        return value >= min && value <= max && value % 32 == 0;
    }

    private static int costOf(AiModelConfig model, Params params) {
        int price = model.getUnitPriceCent() == null ? 0 : Math.max(0, model.getUnitPriceCent());
        String unit = model.getPriceUnit() == null ? "PER_CALL" : model.getPriceUnit();
        int usage = 1;
        if ("PER_SECOND".equals(unit)) {
            usage = Math.max(1, (params.frames() + params.fps() - 1) / params.fps());
        } else if ("PER_MINUTE".equals(unit)) {
            int seconds = Math.max(1, (params.frames() + params.fps() - 1) / params.fps());
            usage = Math.max(1, (seconds + 59) / 60);
        }
        return price * usage;
    }

    private static String fingerprint(String assetId, Params params) {
        String raw = assetId + "\n" + params.prompt() + "\n" + params.width() + "\n" + params.height() + "\n"
                + params.frames() + "\n" + params.fps() + "\n" + params.steps() + "\n" + params.seed();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String env() {
        if (environment == null) {
            return "local";
        }
        String[] profiles = environment.getActiveProfiles();
        if (profiles != null && profiles.length > 0 && profiles[0] != null && !profiles[0].isBlank()) {
            return profiles[0];
        }
        return "local";
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static Object first(Object preferred, Object fallback) {
        return preferred == null || ShortJson.text(preferred).isBlank() ? fallback : preferred;
    }

    private static String firstText(Object preferred, Object fallback) {
        String first = ShortJson.text(preferred).trim();
        return first.isBlank() ? ShortJson.text(fallback).trim() : first;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String cleanReason(String message) {
        String safe = TokenHubClientSupport.userFacingMessage(message);
        if (safe != null && safe.contains("图生视频") && !ProductModelAlias.leaks(safe)
                && !safe.contains("127.0.0.1") && !safe.contains("124.70.197.39") && !safe.contains("172.31.10.199")) {
            return safe.length() > 200 ? safe.substring(0, 200) : safe;
        }
        return ImageConditionedVideoProtocol.FAILED;
    }

    private static String imageFailCode(BizException ex) {
        String code = ex.getCode();
        if (CommonErrorCode.ABILITY_UNAVAILABLE.code().equals(code)) {
            return "I2V_UNAVAILABLE";
        }
        if (CommonErrorCode.REQUEST_TOO_LARGE.code().equals(code)
                || CommonErrorCode.INVALID_PARAM.code().equals(code)) {
            return "I2V_BAD_IMAGE";
        }
        return "I2V_FAILED";
    }

    private static String safeFailure(BizException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank() || ProductModelAlias.leaks(message)) {
            return ImageConditionedVideoProtocol.FAILED;
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    interface MediaCheck {
        void verify(AiProviderResult result, String ability);
    }

    private record Params(String prompt, int width, int height, int frames, int fps, int steps, int seed) {
    }

    private record OwnedImage(ShortAsset asset, byte[] bytes, String workId, String episodeId, ShortShot shot) {
    }
}
