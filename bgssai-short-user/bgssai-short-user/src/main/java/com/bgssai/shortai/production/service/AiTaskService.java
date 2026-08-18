package com.bgssai.shortai.production.service;

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

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.SubscriptionPlanMapper;
import com.bgssai.shortai.mapper.UserSubscriptionMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.SubscriptionPlan;
import com.bgssai.shortai.model.SubscriptionPlanExample;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.model.UserSubscription;
import com.bgssai.shortai.model.UserSubscriptionExample;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;
import com.bgssai.shortai.production.dto.ProductionDtos.AiTaskReq;
import com.bgssai.shortai.production.support.ComplianceLexicon;
import com.bgssai.shortai.production.support.TaskPrices;
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
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private QuotaWalletService quotaWalletService;
    @Autowired
    private PaidOrderGrantService paidOrderGrantService;
    @Autowired
    private UserSubscriptionMapper userSubscriptionMapper;
    @Autowired
    private SubscriptionPlanMapper subscriptionPlanMapper;

    public ApiResponse estimate(AiTaskReq req) {
        List<ShortShot> shots = resolveShots(req);
        String type = req.getTaskType() == null ? "IMAGE" : req.getTaskType();
        int unit = TaskPrices.cost(type);
        if (Integer.valueOf(1).equals(req.getAutoNextFlag()) && "IMAGE".equals(type)) {
            unit += TaskPrices.cost("VIDEO");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_count", shots.size());
        result.put("estimated_cost_cent", shots.size() * unit);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse submit(AiTaskReq req) {
        List<ShortShot> shots = resolveShots(req);
        String type = req.getTaskType() == null ? "IMAGE" : req.getTaskType();
        ensureWallet(workAccessService.requireUser());
        int autoNext = req.getAutoNextFlag() == null ? 0 : req.getAutoNextFlag();
        String batchId = com.bgssai.shortai.production.support.ShortIds.id("b-");
        List<String> ids = new ArrayList<>();
        for (ShortShot shot : shots) {
            Map<String, Object> scan = ComplianceLexicon.scan(
                    (shot.getDialogueText() == null ? "" : shot.getDialogueText()) + " "
                            + (shot.getImagePrompt() == null ? "" : shot.getImagePrompt()),
                    shot.getSceneNo(), null, shot.getShotNo());
            if ("BLOCK".equals(scan.get("risk_level"))) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "shot " + shot.getShotNo() + " BLOCK");
            }
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("shot_id", shot.getShotId());
            ShortAiTask task = aiTaskQueue.enqueue(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(), type, "SHOT",
                    shot.getShotId(), request, batchId, autoNext);
            ids.add(task.getTaskId());
            patchShotStatus(shot, "IMAGE".equals(type) ? "IMAGE_GENERATING" : "VIDEO_GENERATING");
        }
        log.info("submit batch={} count={}", batchId, ids.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch_id", batchId);
        result.put("task_id_list", ids);
        return Responses.ok(result);
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
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse retry(AiTaskReq req) {
        ShortAiTask task = requireTask(req == null ? null : req.getTaskId());
        if (!"FAILED".equals(task.getTaskStatus()) && !"CANCELED".equals(task.getTaskStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "only failed/canceled can retry");
        }
        ShortAiTask patch = new ShortAiTask();
        patch.setTaskStatus("QUEUED");
        patch.setFailReason(null);
        patch.setFailReasonCode(null);
        patch.setProgressPercent(0);
        patch.setRetryCount((task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1);
        patch.setUpdatedTime(new Date());
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId());
        shortAiTaskMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse cancel(AiTaskReq req) {
        ShortAiTask task = requireTask(req == null ? null : req.getTaskId());
        if ("SUCCEEDED".equals(task.getTaskStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "succeeded task cannot cancel");
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

    private void patchShotStatus(ShortShot shot, String status) {
        ShortShot patch = new ShortShot();
        patch.setShotStatus(status);
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
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
        UserSubscriptionExample ex = new UserSubscriptionExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDeleteEqualTo(0);
        UserSubscription sub = userSubscriptionMapper.selectOneByExample(ex);
        if (sub == null) {
            int grant = 10000;
            SubscriptionPlanExample pex = new SubscriptionPlanExample();
            pex.createCriteria().andPlanCodeEqualTo("FREE").andDeleteEqualTo(0);
            SubscriptionPlan plan = subscriptionPlanMapper.selectOneByExample(pex);
            if (plan != null && plan.getMonthlyBalanceCent() != null) {
                grant = plan.getMonthlyBalanceCent();
            }
            paidOrderGrantService.grantSubscription(userId, "FREE", grant);
        }
        UserQuota quota = quotaWalletService.requireWallet(userId);
        quotaWalletService.resetMonthIfNeeded(quota);
    }

    private Map<String, Object> view(ShortAiTask row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("task_id", row.getTaskId());
        item.put("batch_id", row.getBatchId());
        item.put("task_type", row.getTaskType());
        item.put("task_status", row.getTaskStatus());
        item.put("target_type", row.getTargetType());
        item.put("target_id", row.getTargetId());
        item.put("progress_percent", row.getProgressPercent());
        item.put("fail_reason_code", row.getFailReasonCode());
        item.put("fail_reason", row.getFailReason());
        item.put("cost_cent", row.getCostCent());
        item.put("created_time", row.getCreatedTime());
        return item;
    }
}
