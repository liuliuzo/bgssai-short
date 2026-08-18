package com.bgssai.shortai.production.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.production.support.TaskPrices;

@Service
public class AiTaskQueue {

    private static final Logger log = LoggerFactory.getLogger(AiTaskQueue.class);

    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;

    public ShortAiTask enqueue(String userId, String workId, String episodeId, String taskType,
            String targetType, String targetId, Map<String, Object> request, String batchId, int autoNext) {
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
        task.setModelCode("mock");
        task.setRequestJson(ShortJson.toJson(request == null ? new LinkedHashMap<>() : request));
        task.setProgressPercent(0);
        task.setRetryCount(0);
        task.setAutoNextFlag(autoNext);
        task.setCostCent(TaskPrices.cost(taskType));
        Date now = new Date();
        task.setQueuedTime(now);
        task.setCreatedTime(now);
        task.setUpdatedTime(now);
        shortAiTaskMapper.insertSelective(task);
        log.info("enqueue ai task taskId={} type={} target={}", task.getTaskId(), taskType, targetId);
        return task;
    }
}
