package com.bgssai.shortai.production.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.mapper.AiUsageRecordMapper;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.storage.ObjectStorage;

class ShortAiTaskWorkerTest {

    @Test
    void casMissDoesNotConsume() {
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        QuotaConsumeService consume = org.mockito.Mockito.mock(QuotaConsumeService.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "quotaConsumeService", consume);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        ShortAiTask task = new ShortAiTask();
        task.setId(9L);
        task.setTaskId("t-1");
        task.setTaskStatus("QUEUED");
        when(mapper.selectOneByExample(any())).thenReturn(task);
        when(mapper.updateByExampleSelective(any(), any())).thenReturn(0);
        worker.tick();
        verify(consume, never()).consume(any(), anyInt());
    }

    @Test
    void successMarksSucceededAndConsumes() {
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        QuotaConsumeService consume = org.mockito.Mockito.mock(QuotaConsumeService.class);
        AiModelResolveService resolve = org.mockito.Mockito.mock(AiModelResolveService.class);
        AiProviderRegistry registry = org.mockito.Mockito.mock(AiProviderRegistry.class);
        AiUsageRecordMapper usageMapper = org.mockito.Mockito.mock(AiUsageRecordMapper.class);
        ObjectStorage storage = org.mockito.Mockito.mock(ObjectStorage.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "quotaConsumeService", consume);
        ReflectionTestUtils.setField(worker, "aiModelResolveService", resolve);
        ReflectionTestUtils.setField(worker, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(worker, "aiUsageRecordMapper", usageMapper);
        ReflectionTestUtils.setField(worker, "objectStorage", storage);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        ShortAiTask task = new ShortAiTask();
        task.setId(9L);
        task.setTaskId("t-1");
        task.setUserId("u-1");
        task.setTaskStatus("QUEUED");
        task.setTaskType("TEXT");
        task.setTargetType("UNKNOWN");
        task.setModelCode("gpt-4o-mini");
        task.setCostCent(15);
        AiModelConfig model = new AiModelConfig();
        model.setModelCode("gpt-4o-mini");
        model.setProviderCode("OPENAI_COMPAT");
        model.setUnitPriceCent(15);
        model.setQualityLevel("STANDARD");
        model.setPriceUnit("PER_CALL");
        when(mapper.selectOneByExample(any())).thenReturn(task);
        when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
        when(resolve.resolve(any(), any())).thenReturn(model);
        worker.tick();
        verify(consume).consume("u-1", 15);
        verify(usageMapper).insertSelective(any());
    }

    /** 超过「模型 timeout_sec + 宽限」仍在 RUNNING 的任务必须被判超时，否则会永远卡住。 */
    @Test
    void reapsStaleRunningTask() {
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        AiModelResolveService resolve = org.mockito.Mockito.mock(AiModelResolveService.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "aiModelResolveService", resolve);

        AiModelConfig model = new AiModelConfig();
        model.setTimeoutSec(120);
        when(resolve.resolve(any(), any())).thenReturn(model);

        ShortAiTask stale = new ShortAiTask();
        stale.setId(1L);
        stale.setTaskId("t-stale");
        stale.setTaskStatus("RUNNING");
        stale.setTaskType("IMAGE");
        stale.setStartedTime(new java.util.Date(System.currentTimeMillis() - 3600_000L));
        when(mapper.selectByExample(any())).thenReturn(java.util.List.of(stale));

        worker.reapStaleTasks();

        org.mockito.ArgumentCaptor<ShortAiTask> patch =
                org.mockito.ArgumentCaptor.forClass(ShortAiTask.class);
        verify(mapper).updateByExampleSelective(patch.capture(), any());
        org.junit.jupiter.api.Assertions.assertEquals("FAILED", patch.getValue().getTaskStatus());
        org.junit.jupiter.api.Assertions.assertEquals("MODEL_TIMEOUT", patch.getValue().getFailReasonCode());
    }

    /** 还没到超时上限的 RUNNING 任务不能被误杀。 */
    @Test
    void keepsFreshRunningTask() {
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        AiModelResolveService resolve = org.mockito.Mockito.mock(AiModelResolveService.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "aiModelResolveService", resolve);

        AiModelConfig model = new AiModelConfig();
        model.setTimeoutSec(600);
        when(resolve.resolve(any(), any())).thenReturn(model);

        ShortAiTask fresh = new ShortAiTask();
        fresh.setId(2L);
        fresh.setTaskId("t-fresh");
        fresh.setTaskStatus("RUNNING");
        fresh.setTaskType("VIDEO");
        fresh.setStartedTime(new java.util.Date(System.currentTimeMillis() - 5_000L));
        when(mapper.selectByExample(any())).thenReturn(java.util.List.of(fresh));

        worker.reapStaleTasks();

        verify(mapper, never()).updateByExampleSelective(any(), any());
    }

    /**
     * 库/表缺失时 tick 必须吞掉 SQL 异常并进入退避，避免 fixedDelay=800 把 ERROR 打满磁盘。
     */
    @Test
    void schemaFailurePausesPolling() {
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        when(mapper.selectOneByExample(any())).thenThrow(
                new InvalidDataAccessResourceUsageException(
                        "Table 'bgssai_short.short_ai_task' doesn't exist"));

        worker.tick();
        worker.tick();

        verify(mapper, times(1)).selectOneByExample(any());
        long pauseUntil = (Long) ReflectionTestUtils.getField(worker, "pollPauseUntilEpochMs");
        Assertions.assertTrue(pauseUntil > System.currentTimeMillis());
        Assertions.assertEquals(1, ReflectionTestUtils.getField(worker, "pollFailureStreak")
                instanceof java.util.concurrent.atomic.AtomicInteger
                        ? ((java.util.concurrent.atomic.AtomicInteger) ReflectionTestUtils
                                .getField(worker, "pollFailureStreak")).get()
                        : -1);
    }
}
