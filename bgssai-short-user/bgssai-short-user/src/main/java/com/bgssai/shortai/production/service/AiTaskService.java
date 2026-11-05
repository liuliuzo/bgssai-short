package com.bgssai.shortai.production.service;

import com.bgssai.shortai.aiprovider.TokenHubClientSupport;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aimodel.ProductModelAlias;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;
import com.bgssai.shortai.production.dto.ProductionDtos.AiTaskReq;
import com.bgssai.shortai.production.support.ComplianceLexicon;
import com.bgssai.shortai.production.support.GenerationTaskStatus;
import com.bgssai.shortai.production.support.ShotConsistency;
import com.bgssai.shortai.production.support.ShortLabels;
import com.bgssai.shortai.production.support.TaskPrices;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.quota.service.QuotaWalletService;
import com.github.pagehelper.PageHelper;

@Service
public class AiTaskService {

    private static final Logger log = LoggerFactory.getLogger(AiTaskService.class);

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortSceneCardMapper shortSceneCardMapper;
    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private QuotaWalletService quotaWalletService;
    @Autowired
    private QuotaConsumeService quotaConsumeService;
    @Autowired
    private PaidOrderGrantService paidOrderGrantService;
    @Autowired
    private AiModelResolveService aiModelResolveService;

    public ApiResponse estimate(AiTaskReq req) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "body required");
        }
        String type = req.getTaskType() == null ? "IMAGE" : req.getTaskType().trim();
        String userId = workAccessService.requireUser();
        ensureWallet(userId);
        if ("COMPOSE".equals(type)) {
            return Responses.ok(estimateCompose(req, userId));
        }
        if ("VOICE".equals(type)) {
            return Responses.ok(estimateVoice(req, userId));
        }
        List<ShortShot> shots = resolveShots(req);
        AiModelConfig model = aiModelResolveService.resolve(type, req.getModelCode());
        List<Map<String, Object>> shotCostList = new ArrayList<>();
        int estimated = 0;
        for (ShortShot shot : shots) {
            int cost = costForShot(model, type, shot);
            estimated += cost;
            shotCostList.add(shotCostItem(shot, type, model, cost));
        }
        AiModelConfig videoModel = null;
        if (Integer.valueOf(1).equals(req.getAutoNextFlag()) && "IMAGE".equals(type)) {
            videoModel = aiModelResolveService.resolve("VIDEO", req.getNextModelCode());
            for (ShortShot shot : shots) {
                int cost = costForShot(videoModel, "VIDEO", shot);
                estimated += cost;
                shotCostList.add(shotCostItem(shot, "VIDEO", videoModel, cost));
            }
        }
        Map<String, Object> result = walletEstimateFields(userId, estimated);
        result.put("task_type", type);
        result.put("task_count", shots.size());
        result.put("estimated_cost_cent", estimated);
        ProductModelAlias.hideStoredModel(result, type, model.getModelCode());
        result.put("unit_price_cent", effectiveUnitPrice(model, type));
        result.put("price_unit", model.getPriceUnit());
        if (videoModel != null) {
            ProductModelAlias.Face next = ProductModelAlias.forStored("VIDEO", videoModel.getModelCode());
            result.put("next_model_code", next.code());
            result.put("next_display_name", next.name());
            result.put("next_unit_price_cent", effectiveUnitPrice(videoModel, "VIDEO"));
        }
        result.put("shot_cost_list", shotCostList);
        quotaConsumeService.assertAffordable(userId, estimated);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse submit(AiTaskReq req) {
        List<ShortShot> shots = resolveShots(req);
        String type = req.getTaskType() == null ? "IMAGE" : req.getTaskType();
        String userId = workAccessService.requireUser();
        ensureWallet(userId);
        AiModelConfig model = aiModelResolveService.resolve(type, req.getModelCode());
        int estimated = shots.stream().mapToInt(shot -> costForShot(model, type, shot)).sum();
        if (Integer.valueOf(1).equals(req.getAutoNextFlag()) && "IMAGE".equals(type)) {
            AiModelConfig videoModel = aiModelResolveService.resolve("VIDEO", req.getNextModelCode());
            estimated += shots.stream().mapToInt(shot -> costForShot(videoModel, "VIDEO", shot)).sum();
        }
        quotaConsumeService.assertAffordable(userId, estimated);
        int autoNext = req.getAutoNextFlag() == null ? 0 : req.getAutoNextFlag();
        String batchId = com.bgssai.shortai.production.support.ShortIds.id("b-");
        List<String> ids = new ArrayList<>();
        for (ShortShot shot : shots) {
            assertShotReady(shot, type);
            Map<String, Object> scan = ComplianceLexicon.scan(
                    (shot.getDialogueText() == null ? "" : shot.getDialogueText()) + " "
                            + (shot.getImagePrompt() == null ? "" : shot.getImagePrompt()),
                    shot.getSceneNo(), null, shot.getShotNo());
            if ("BLOCK".equals(scan.get("risk_level"))) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "shot " + shot.getShotNo() + " BLOCK");
            }
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("shot_id", shot.getShotId());
            request.put("duration_sec", shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec());
            if (req.getModelCode() != null && !req.getModelCode().isBlank()) {
                request.put("model_code", req.getModelCode().trim());
            }
            if (req.getNextModelCode() != null && !req.getNextModelCode().isBlank()) {
                request.put("next_model_code", req.getNextModelCode().trim());
            }
            ShortAiTask task = aiTaskQueue.enqueue(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(), type, "SHOT",
                    shot.getShotId(), request, batchId, autoNext, req.getModelCode());
            ids.add(task.getTaskId());
            patchShotStatus(shot, "IMAGE".equals(type) ? "IMAGE_GENERATING" : "VIDEO_GENERATING");
        }
        log.info("submit batch={} count={}", batchId, ids.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch_id", batchId);
        result.put("task_id_list", ids);
        result.put("task_count", ids.size());
        result.put("estimated_cost_cent", estimated);
        return Responses.ok(result);
    }

    private Map<String, Object> estimateCompose(AiTaskReq req, String userId) {
        ShortEpisode ep = workAccessService.requireEpisode(req.getEpisodeId());
        AiModelConfig model = aiModelResolveService.resolve("COMPOSE", req.getModelCode());
        int durationSec = episodeDurationSec(ep.getEpisodeId());
        int usage = usageAmount(model, durationSec);
        int estimated = pricedCost(model, "COMPOSE", usage);
        Map<String, Object> result = walletEstimateFields(userId, estimated);
        result.put("task_type", "COMPOSE");
        result.put("task_count", 1);
        result.put("episode_id", ep.getEpisodeId());
        result.put("duration_sec", durationSec);
        result.put("estimated_cost_cent", estimated);
        ProductModelAlias.hideStoredModel(result, "COMPOSE", model.getModelCode());
        result.put("unit_price_cent", effectiveUnitPrice(model, "COMPOSE"));
        result.put("price_unit", model.getPriceUnit());
        result.put("shot_cost_list", List.of());
        quotaConsumeService.assertAffordable(userId, estimated);
        return result;
    }

    private Map<String, Object> estimateVoice(AiTaskReq req, String userId) {
        List<ShortShot> shots = resolveShots(req).stream()
                .filter(s -> s.getDialogueText() != null && !s.getDialogueText().isBlank())
                .toList();
        if (shots.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "no dialogue shots to dub");
        }
        AiModelConfig model = aiModelResolveService.resolve("VOICE", req.getModelCode());
        List<Map<String, Object>> shotCostList = new ArrayList<>();
        int estimated = 0;
        for (ShortShot shot : shots) {
            int cost = costForShot(model, "VOICE", shot);
            estimated += cost;
            shotCostList.add(shotCostItem(shot, "VOICE", model, cost));
        }
        Map<String, Object> result = walletEstimateFields(userId, estimated);
        result.put("task_type", "VOICE");
        result.put("task_count", shots.size());
        result.put("estimated_cost_cent", estimated);
        ProductModelAlias.hideStoredModel(result, "VOICE", model.getModelCode());
        result.put("unit_price_cent", effectiveUnitPrice(model, "VOICE"));
        result.put("price_unit", model.getPriceUnit());
        result.put("shot_cost_list", shotCostList);
        quotaConsumeService.assertAffordable(userId, estimated);
        return result;
    }

    private Map<String, Object> walletEstimateFields(String userId, int estimated) {
        UserQuota quota = quotaWalletService.requireWallet(userId);
        quotaWalletService.resetMonthIfNeeded(quota);
        int balance = quota.getBalanceCent() == null ? 0 : quota.getBalanceCent();
        int extra = quota.getExtraBalanceCent() == null ? 0 : quota.getExtraBalanceCent();
        int available = balance + extra;
        int affordable = estimated <= 0 ? 0 : available / estimated;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance_cent", balance);
        result.put("extra_balance_cent", extra);
        result.put("available_cent", available);
        result.put("month_spent_cent", quota.getMonthSpentCent() == null ? 0 : quota.getMonthSpentCent());
        result.put("monthly_limit_cent", quota.getMonthlyLimitCent() == null ? 0 : quota.getMonthlyLimitCent());
        result.put("affordable_flag", estimated <= 0 || available >= estimated ? 1 : 0);
        result.put("affordable_episode_count", affordable);
        return result;
    }

    private Map<String, Object> shotCostItem(ShortShot shot, String type, AiModelConfig model, int cost) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("shot_id", shot.getShotId());
        item.put("shot_no", shot.getShotNo());
        item.put("task_type", type);
        item.put("duration_sec", shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec());
        ProductModelAlias.hideStoredModel(item, type, model.getModelCode());
        item.put("unit_price_cent", effectiveUnitPrice(model, type));
        item.put("estimated_cost_cent", cost);
        return item;
    }

    private int episodeDurationSec(String episodeId) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        int total = 0;
        for (ShortShot shot : shortShotMapper.selectByExample(ex)) {
            total += shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
        }
        return Math.max(1, total);
    }

    private static int usageAmount(AiModelConfig model, int durationSec) {
        String unit = model.getPriceUnit();
        if ("PER_SECOND".equals(unit)) {
            return Math.max(1, durationSec);
        }
        if ("PER_MINUTE".equals(unit)) {
            return Math.max(1, (durationSec + 59) / 60);
        }
        return 1;
    }

    private int pricedCost(AiModelConfig model, String type, int usage) {
        int cost = aiModelResolveService.costCent(model, usage);
        return cost > 0 ? cost : TaskPrices.cost(type);
    }

    private int effectiveUnitPrice(AiModelConfig model, String type) {
        int price = model.getUnitPriceCent() == null ? 0 : model.getUnitPriceCent();
        return price > 0 ? price : TaskPrices.cost(type);
    }

    public PageResponse list(int pageNum, int pageSize, String taskType, String taskStatus, String batchId,
            String episodeId, String workId) {
        String userId = workAccessService.requireUser();
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ShortAiTaskExample.Criteria c = ex.createCriteria().andUserIdEqualTo(userId);
        if (taskType != null && !taskType.isBlank()) {
            c.andTaskTypeEqualTo(taskType.trim());
        }
        if (taskStatus != null && !taskStatus.isBlank()) {
            c.andTaskStatusEqualTo(taskStatus.trim());
        }
        if (batchId != null && !batchId.isBlank()) {
            c.andBatchIdEqualTo(batchId.trim());
        }
        if (episodeId != null && !episodeId.isBlank()) {
            c.andEpisodeIdEqualTo(episodeId.trim());
        }
        if (workId != null && !workId.isBlank()) {
            c.andWorkIdEqualTo(workId.trim());
        }
        ex.setOrderByClause("created_time desc, id desc");
        PageHelper.startPage(safePageNum, safePageSize);
        List<ShortAiTask> rows = shortAiTaskMapper.selectByExample(ex);
        long total = ((com.github.pagehelper.Page<ShortAiTask>) rows).getTotal();
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortAiTask row : rows) {
            items.add(view(row));
        }
        PageResponse response = new PageResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setMessage("OK");
        response.setResult(items);
        response.setPageNum(safePageNum);
        response.setPageSize(safePageSize);
        response.setTotalSize(total);
        return response;
    }

    public ApiResponse batchProgress(String batchId) {
        String userId = workAccessService.requireUser();
        if (batchId == null || batchId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "batch_id required");
        }
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andUserIdEqualTo(userId).andBatchIdEqualTo(batchId);
        List<ShortAiTask> rows = shortAiTaskMapper.selectByExample(ex);
        int queued = 0;
        int running = 0;
        int succeeded = 0;
        int failed = 0;
        int canceled = 0;
        for (ShortAiTask row : rows) {
            if ("QUEUED".equals(row.getTaskStatus())) {
                queued++;
            } else if ("RUNNING".equals(row.getTaskStatus())) {
                running++;
            } else if ("SUCCEEDED".equals(row.getTaskStatus())) {
                succeeded++;
            } else if ("FAILED".equals(row.getTaskStatus())) {
                failed++;
            } else if ("CANCELED".equals(row.getTaskStatus())) {
                canceled++;
            }
        }
        int total = rows.size();
        int done = succeeded + failed + canceled;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("queued", queued);
        result.put("running", running);
        result.put("succeeded", succeeded);
        result.put("failed", failed);
        result.put("canceled", canceled);
        result.put("progress_percent", total == 0 ? 0 : done * 100 / total);
        List<Map<String, Object>> failedTasks = new ArrayList<>();
        for (ShortAiTask row : rows) {
            if (!"FAILED".equals(row.getTaskStatus())) {
                continue;
            }
            Map<String, Object> fail = new LinkedHashMap<>();
            fail.put("task_id", row.getTaskId());
            fail.put("target_id", row.getTargetId());
            fail.put("task_type", row.getTaskType());
            fail.put("fail_reason_code", row.getFailReasonCode());
            fail.put("fail_reason", visibleFailReason(
                    row.getTaskStatus(), row.getFailReasonCode(), row.getFailReason()));
            failedTasks.add(fail);
        }
        result.put("failed_task_list", failedTasks);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse recover(AiTaskReq req) {
        return retry(req);
    }

    @Transactional
    public ApiResponse retry(AiTaskReq req) {
        ShortAiTask task = requireTask(req == null ? null : req.getTaskId());
        if (!GenerationTaskStatus.recoverable(task.getTaskStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "only failed/canceled can retry");
        }
        ShortAiTask patch = new ShortAiTask();
        patch.setTaskStatus("QUEUED");
        patch.setFailReason(null);
        patch.setFailReasonCode(null);
        patch.setProgressPercent(0);
        patch.setRetryCount((task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1);
        patch.setUpdatedTime(new Date());
        boolean useFallback = req != null && req.getFallbackModelFlag() != null
                && req.getFallbackModelFlag() == 1;
        if (useFallback) {
            AiModelConfig alt = aiModelResolveService.resolveFallback(task.getTaskType(), task.getModelCode());
            if (alt != null && alt.getModelCode() != null && !alt.getModelCode().isBlank()) {
                patch.setModelCode(alt.getModelCode());
            }
        }
        String nextModel = patch.getModelCode() != null ? patch.getModelCode() : task.getModelCode();
        aiTaskQueue.ensurePipeline(task.getTaskType(), nextModel);
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId());
        shortAiTaskMapper.updateByExampleSelective(patch, up);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        result.put("task_status", "QUEUED");
        ProductModelAlias.hideStoredModel(result, task.getTaskType(),
                patch.getModelCode() != null ? patch.getModelCode() : task.getModelCode());
        result.put("fallback_model_flag", useFallback ? 1 : 0);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse cancel(AiTaskReq req) {
        ShortAiTask task = requireTask(req == null ? null : req.getTaskId());
        if (!GenerationTaskStatus.cancellable(task.getTaskStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "only queued/running/paused can cancel");
        }
        ShortAiTask patch = new ShortAiTask();
        patch.setTaskStatus("CANCELED");
        patch.setFinishedTime(new Date());
        patch.setUpdatedTime(new Date());
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId());
        shortAiTaskMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    private List<ShortShot> resolveShots(AiTaskReq req) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "body required");
        }
        List<ShortShot> shots = new ArrayList<>();
        if (req.getShotIds() != null && !req.getShotIds().isEmpty()) {
            for (String shotId : req.getShotIds()) {
                ShortShotExample ex = new ShortShotExample();
                ex.createCriteria().andShotIdEqualTo(shotId).andDeletedFlagEqualTo(0);
                ShortShot shot = shortShotMapper.selectOneByExample(ex);
                if (shot == null || !workAccessService.requireUser().equals(shot.getUserId())) {
                    throw new ForbiddenException("shot not found");
                }
                shots.add(shot);
            }
            return shots;
        }
        ShortEpisode ep = workAccessService.requireEpisode(req.getEpisodeId());
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        return shortShotMapper.selectByExample(ex);
    }

    private void assertShotReady(ShortShot shot, String type) {
        if ("LOCKED".equals(shot.getShotStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "镜" + shot.getShotNo() + " 已定稿，不能再生成");
        }
        if (ShotConsistency.shotNeedsPrompt(shot)) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "镜" + shot.getShotNo() + " 还没有可生成的画面描述，先写人话分镜");
        }
        if ("VIDEO".equals(type)
                && (shot.getFirstFrameAssetId() == null || shot.getFirstFrameAssetId().isBlank())) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "镜" + shot.getShotNo() + " 需先出图确认构图，再生成视频");
        }
        for (String characterId : ShotConsistency.characterIds(shot.getCharacterRefJson())) {
            ShortCharacterExample cx = new ShortCharacterExample();
            cx.createCriteria().andCharacterIdEqualTo(characterId).andDeletedFlagEqualTo(0);
            ShortCharacter row = shortCharacterMapper.selectOneByExample(cx);
            if (row == null) {
                throw new BizException(CommonErrorCode.RULE_REJECT,
                        "镜" + shot.getShotNo() + " 引用的角色素材缺失：" + characterId);
            }
            if (!"LOCKED".equals(row.getCardStatus()) || row.getRefImageAssetId() == null
                    || row.getRefImageAssetId().isBlank()) {
                throw new BizException(CommonErrorCode.RULE_REJECT,
                        "镜" + shot.getShotNo() + " 引用的角色「" + row.getCharacterName()
                                + "」尚未定妆，请先完成定妆");
            }
        }
        if (shot.getSceneCardId() != null && !shot.getSceneCardId().isBlank()) {
            ShortSceneCardExample sx = new ShortSceneCardExample();
            sx.createCriteria().andSceneCardIdEqualTo(shot.getSceneCardId()).andDeletedFlagEqualTo(0);
            ShortSceneCard scene = shortSceneCardMapper.selectOneByExample(sx);
            if (scene == null) {
                throw new BizException(CommonErrorCode.RULE_REJECT,
                        "镜" + shot.getShotNo() + " 引用的场景素材缺失：" + shot.getSceneCardId());
            }
            if (!"LOCKED".equals(scene.getCardStatus())
                    || scene.getRefImageAssetId() == null || scene.getRefImageAssetId().isBlank()) {
                throw new BizException(CommonErrorCode.RULE_REJECT,
                        "镜" + shot.getShotNo() + " 引用的场景「" + scene.getCardName()
                                + "」尚未锁定场景卡，请先定妆");
            }
        }
        for (String propId : ShotConsistency.propIds(shot.getPropRefJson())) {
            ShortSceneCardExample px = new ShortSceneCardExample();
            px.createCriteria().andSceneCardIdEqualTo(propId).andDeletedFlagEqualTo(0);
            ShortSceneCard prop = shortSceneCardMapper.selectOneByExample(px);
            if (prop == null) {
                throw new BizException(CommonErrorCode.RULE_REJECT,
                        "镜" + shot.getShotNo() + " 引用的道具素材缺失：" + propId);
            }
            if (!"LOCKED".equals(prop.getCardStatus())
                    || prop.getRefImageAssetId() == null || prop.getRefImageAssetId().isBlank()) {
                throw new BizException(CommonErrorCode.RULE_REJECT,
                        "镜" + shot.getShotNo() + " 引用的道具「" + prop.getCardName()
                                + "」尚未锁定道具卡，请先定妆");
            }
        }
    }

    private void patchShotStatus(ShortShot shot, String status) {
        ShortShot patch = new ShortShot();
        patch.setShotStatus(status);
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
    }

    private int costForShot(AiModelConfig model, String type, ShortShot shot) {
        int duration = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
        return pricedCost(model, type, usageAmount(model, duration));
    }

    private ShortAiTask requireTask(String taskId) {
        String userId = workAccessService.requireUser();
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTaskIdEqualTo(taskId);
        ShortAiTask task = shortAiTaskMapper.selectOneByExample(ex);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new ForbiddenException("task not found");
        }
        return task;
    }

    private void ensureWallet(String userId) {
        paidOrderGrantService.ensureFreeTier(userId);
    }

    private Map<String, Object> view(ShortAiTask row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("task_id", row.getTaskId());
        item.put("batch_id", row.getBatchId());
        item.put("task_type", row.getTaskType());
        item.put("task_status", row.getTaskStatus());
        item.putAll(GenerationTaskStatus.flags(row.getTaskStatus(), row.getTaskType()));
        item.put("target_type", row.getTargetType());
        item.put("target_type_name", ShortLabels.targetTypeName(row.getTargetType()));
        item.put("target_id", row.getTargetId());
        item.put("progress_percent", row.getProgressPercent());
        item.put("fail_reason_code", row.getFailReasonCode());
        item.put("fail_reason", visibleFailReason(
                row.getTaskStatus(), row.getFailReasonCode(), row.getFailReason()));
        ProductModelAlias.hideStoredModel(item, row.getTaskType(), row.getModelCode());
        item.put("result_json", clientResult(row));
        item.put("cost_cent", row.getCostCent());
        item.put("created_time", row.getCreatedTime());
        return item;
    }

    /**
     * Success, or a missing fail code, is not a failure. A blank stored reason must stay blank
     * instead of becoming the generic model-unavailable sentence.
     */
    private static Map<String, Object> clientResult(ShortAiTask row) {
        Map<String, Object> json = ShortJson.map(row.getResultJson());
        if (json == null || json.isEmpty()) {
            return json;
        }
        for (String key : List.of("model", "model_code", "model_name", "display_name")) {
            Object value = json.get(key);
            if (value instanceof String text && ProductModelAlias.leaks(text)) {
                ProductModelAlias.Face face = ProductModelAlias.forStored(row.getTaskType(), text);
                json.put(key, "model_name".equals(key) || "display_name".equals(key) ? face.name() : face.code());
            }
        }
        return json;
    }

    static String visibleFailReason(String taskStatus, String failReasonCode, String failReason) {
        if (isSuccessStatus(taskStatus) || failReasonCode == null || failReasonCode.isBlank()) {
            return null;
        }
        return TokenHubClientSupport.userFacingMessage(failReason);
    }

    private static boolean isSuccessStatus(String taskStatus) {
        if (taskStatus == null || taskStatus.isBlank()) {
            return false;
        }
        String status = taskStatus.trim();
        return "SUCCEEDED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }
}
