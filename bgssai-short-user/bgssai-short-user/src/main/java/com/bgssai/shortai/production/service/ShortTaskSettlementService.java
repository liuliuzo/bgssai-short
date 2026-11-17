package com.bgssai.shortai.production.service;
import java.util.Date;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bgssai.shortai.aiprovider.DirectVendorGuard;
import com.bgssai.shortai.mapper.*;
import com.bgssai.shortai.model.*;
import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.useraimodel.service.UserAiModelSupport;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;
import com.bgssai.shortai.common.exception.*;

@Service
public class ShortTaskSettlementService {
    private final ShortAiTaskMapper tasks;
    private final QuotaConsumeService quota;
    private final AiUsageRecordMapper aiUsageRecordMapper;
    private final ShortSettlementMapper works;
    private final AiModelResolveService models;
    @Autowired(required = false)
    private UserAiProviderKeyService userAiProviderKeyService;
    public ShortTaskSettlementService(ShortAiTaskMapper tasks, QuotaConsumeService quota,
            AiUsageRecordMapper usage, ShortSettlementMapper works, AiModelResolveService models) {
        this.tasks=tasks; this.quota=quota; this.aiUsageRecordMapper=usage; this.works=works; this.models=models;
    }
    public void setUserAiProviderKeyService(UserAiProviderKeyService userAiProviderKeyService) {
        this.userAiProviderKeyService = userAiProviderKeyService;
    }
    private boolean isUserKeySettlement(ShortAiTask task, AiModelConfig model) {
        if (UserAiModelSupport.isUserKeyModel(model)) {
            return true;
        }
        String userId = task == null ? "" : task.getUserId();
        return DirectVendorGuard.resolveProviderByok(
                model, userId == null ? Map.of() : Map.of("user_id", userId), userAiProviderKeyService).isPresent();
    }
    @Transactional
    public boolean settle(ShortAiTask task, AiModelConfig model, int usage) {
        boolean userKeyModel = isUserKeySettlement(task, model);
        int cost = userKeyModel ? 0 : (task.getCostCent() == null ? models.costCent(model, usage) : task.getCostCent());
        if (cost < 0) throw new BizException(CommonErrorCode.RULE_REJECT, "invalid task cost");
        ShortAiTask done = new ShortAiTask();
        done.setTaskStatus("SUCCEEDED"); done.setProgressPercent(100);
        done.setFinishedTime(new Date()); done.setUpdatedTime(new Date());
        ShortAiTaskExample cas = new ShortAiTaskExample();
        cas.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("RUNNING");
        if (tasks.updateByExampleSelective(done, cas) != 1) return false;
        if (!userKeyModel && cost > 0) {
            quota.consume(task.getUserId(), cost, "task:" + task.getTaskId());
        }
        insertUsage(task, model, usage, cost);
        if (cost > 0 && task.getWorkId() != null && works.addWorkCost(task.getWorkId(), task.getUserId(), cost) != 1)
            throw new BizException(CommonErrorCode.RULE_REJECT, "task work no longer available");
        return true;
    }
    private void insertUsage(ShortAiTask task, AiModelConfig model, int usageAmount, int cost) {
        boolean userKeyModel = isUserKeySettlement(task, model);
        String recordModelCode = userKeyModel && model.getModelId() != null && !model.getModelId().isBlank()
                ? model.getModelId() : model.getModelCode();
        AiUsageRecord row = new AiUsageRecord();
        row.setRecordId(ShortIds.id("ur-"));
        row.setUserId(task.getUserId());
        row.setWorkId(task.getWorkId());
        row.setEpisodeId(task.getEpisodeId());
        row.setShotId("SHOT".equals(task.getTargetType()) ? task.getTargetId() : null);
        row.setTaskId(task.getTaskId());
        row.setAbilityType(task.getTaskType());
        row.setModelCode(recordModelCode);
        row.setQualityLevel(model.getQualityLevel());
        row.setPriceUnit(model.getPriceUnit());
        row.setUsageAmount(usageAmount);
        row.setUnitPriceCent(userKeyModel ? Integer.valueOf(0) : model.getUnitPriceCent());
        row.setCostCent(cost);
        row.setCreatedTime(new Date());
        aiUsageRecordMapper.insertSelective(row);
    }
}
