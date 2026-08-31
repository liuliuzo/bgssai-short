package com.bgssai.shortai.production.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class AiTaskQueue {

    private static final Logger log = LoggerFactory.getLogger(AiTaskQueue.class);

    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private AiModelResolveService aiModelResolveService;

    public ShortAiTask enqueue(String userId, String workId, String episodeId, String taskType,
            String targetType, String targetId, Map<String, Object> request, String batchId, int autoNext) {
        return enqueue(userId, workId, episodeId, taskType, targetType, targetId, request, batchId, autoNext, null);
    }

    public ShortAiTask enqueue(String userId, String workId, String episodeId, String taskType,
            String targetType, String targetId, Map<String, Object> request, String batchId, int autoNext,
            String modelCode) {
        Map<String, Object> req = request == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request);
        String resolvedModelCode = modelCode;
        if (resolvedModelCode == null || resolvedModelCode.isBlank()) {
            Object fromReq = req.get("model_code");
            if (fromReq != null && !String.valueOf(fromReq).isBlank()) {
                resolvedModelCode = String.valueOf(fromReq).trim();
            }
        }
        AiModelConfig model = aiModelResolveService.resolve(taskType, resolvedModelCode);
        if (resolvedModelCode == null || resolvedModelCode.isBlank()) {
            req.put("model_code", model.getModelCode());
        }
        ShortAiTask task = new ShortAiTask();
        task.setTaskId(ShortIds.id("t-"));
        task.setBatchId(batchId == null ? ShortIds.id("b-") : batchId);
        task.setUserId(userId);
        task.setWorkId(workId);
        task.setEpisodeId(episodeId);
        task.setTaskType(taskType);
        task.setTaskStatus("QUEUED");
        task.setTargetType(targetType);
        task.setTargetId(targetId);
        task.setModelCode(model.getModelCode());
        task.setRequestJson(ShortJson.toJson(req));
        task.setProgressPercent(0);
        task.setRetryCount(0);
        task.setAutoNextFlag(autoNext);
        task.setCostCent(aiModelResolveService.costCent(model, estimatedUsage(model, req)));
        Date now = new Date();
        task.setQueuedTime(now);
        task.setCreatedTime(now);
        task.setUpdatedTime(now);
        shortAiTaskMapper.insertSelective(task);
        log.info("enqueue ai task taskId={} type={} model={} target={}", task.getTaskId(), taskType,
                model.getModelCode(), targetId);
        return task;
    }

    private static int estimatedUsage(AiModelConfig model, Map<String, Object> request) {
        String unit = model.getPriceUnit();
        if ("PER_SECOND".equals(unit)) {
            int seconds = ShortJson.integer(request.get("duration_sec"), 0);
            if (seconds <= 0) {
                seconds = ShortJson.integer(ShortJson.map(model.getConfigJson()).get("duration"), 1);
            }
            return Math.max(1, seconds);
        }
        if ("PER_MINUTE".equals(unit)) {
            int seconds = ShortJson.integer(request.get("duration_sec"), 0);
            return seconds <= 0 ? 1 : Math.max(1, (seconds + 59) / 60);
        }
        return 1;
    }
}
