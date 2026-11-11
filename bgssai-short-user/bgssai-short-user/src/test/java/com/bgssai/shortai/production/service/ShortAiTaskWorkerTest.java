package com.bgssai.shortai.production.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
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
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.quota.service.QuotaConsumeService;

class ShortAiTaskWorkerTest {

    @Test
    void storageLinkageFailureImmediatelyFailsWithoutAssetOrSettlement() {
        StorageFailureFixture fixture = storageFailure(new NoSuchMethodError("SDK binary mismatch"));
        fixture.worker().tick();
        org.mockito.ArgumentCaptor<ShortAiTask> patches = org.mockito.ArgumentCaptor.forClass(ShortAiTask.class);
        verify(fixture.tasks(), org.mockito.Mockito.atLeastOnce()).updateByExampleSelective(patches.capture(), any());
        ShortAiTask failed = patches.getValue();
        Assertions.assertEquals("FAILED", failed.getTaskStatus());
        Assertions.assertEquals("STORAGE_UNAVAILABLE", failed.getFailReasonCode());
        Assertions.assertEquals("素材存储服务不可用，任务未完成，请联系管理员", failed.getFailReason());
        Assertions.assertNotNull(failed.getFinishedTime());
        verify(fixture.assets(), never()).insertSelective(any());
        verify(fixture.settlement(), never()).settle(any(), any(), anyInt());
        verify(fixture.provider(), times(1)).execute(any());
    }

    @Test
    void fatalVmErrorIsNotConvertedToARecoverableStorageFailure() {
        StorageFailureFixture fixture = storageFailure(new OutOfMemoryError("test fatal error"));
        Assertions.assertThrows(OutOfMemoryError.class, () -> fixture.worker().tick());
        org.mockito.ArgumentCaptor<ShortAiTask> patches = org.mockito.ArgumentCaptor.forClass(ShortAiTask.class);
        verify(fixture.tasks(), org.mockito.Mockito.atLeastOnce()).updateByExampleSelective(patches.capture(), any());
        Assertions.assertTrue(patches.getAllValues().stream().noneMatch(p -> "FAILED".equals(p.getTaskStatus())));
        verify(fixture.assets(), never()).insertSelective(any());
        verify(fixture.settlement(), never()).settle(any(), any(), anyInt());
    }

    private StorageFailureFixture storageFailure(Error error) {
        ShortAiTaskMapper tasks = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        ShortTaskSettlementService settlement = org.mockito.Mockito.mock(ShortTaskSettlementService.class);
        AiModelResolveService resolve = org.mockito.Mockito.mock(AiModelResolveService.class);
        AiProviderRegistry registry = org.mockito.Mockito.mock(AiProviderRegistry.class);
        var provider = org.mockito.Mockito.mock(com.bgssai.shortai.aiprovider.AiProvider.class);
        var assets = org.mockito.Mockito.mock(com.bgssai.shortai.mapper.ShortAssetMapper.class);
        var characters = org.mockito.Mockito.mock(com.bgssai.shortai.mapper.ShortCharacterMapper.class);
        var storage = org.mockito.Mockito.mock(com.bgssai.shortai.storage.ObjectStorage.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", tasks);
        ReflectionTestUtils.setField(worker, "settlement", settlement);
        ReflectionTestUtils.setField(worker, "aiModelResolveService", resolve);
        ReflectionTestUtils.setField(worker, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(worker, "shortAssetMapper", assets);
        ReflectionTestUtils.setField(worker, "shortCharacterMapper", characters);
        ReflectionTestUtils.setField(worker, "objectStorage", storage);
        ReflectionTestUtils.setField(worker, "environment", new org.springframework.mock.env.MockEnvironment());
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        ShortAiTask task = new ShortAiTask();
        task.setId(9L);
        task.setTaskId("t-storage-failure");
        task.setUserId("u-test");
        task.setWorkId("w-test");
        task.setTaskStatus("QUEUED");
        task.setTaskType("IMAGE");
        task.setTargetType("PORTRAIT");
        task.setTargetId("c-test");
        task.setModelCode("bgssai-image");
        task.setRequestJson("{\"candidate_count\":2}");
        var character = new com.bgssai.shortai.model.ShortCharacter();
        character.setCharacterId("c-test");
        character.setCharacterName("Test character");
        character.setUserId("u-test");
        character.setWorkId("w-test");
        when(tasks.selectOneByExample(any())).thenReturn(task);
        when(tasks.updateByExampleSelective(any(), any())).thenReturn(1);
        when(resolve.resolve(any(), any())).thenReturn(new AiModelConfig());
        when(characters.selectOneByExample(any())).thenReturn(character);
        when(registry.resolve(any())).thenReturn(provider);
        when(provider.execute(any())).thenReturn(com.bgssai.shortai.aiprovider.AiProviderResult.binary(new byte[] {1}, "image/png"));
        when(storage.put(any(), any(), any())).thenThrow(error);
        return new StorageFailureFixture(worker, tasks, assets, settlement, provider);
    }

    private record StorageFailureFixture(ShortAiTaskWorker worker, ShortAiTaskMapper tasks,
            com.bgssai.shortai.mapper.ShortAssetMapper assets, ShortTaskSettlementService settlement,
            com.bgssai.shortai.aiprovider.AiProvider provider) { }

    @Test
    void casMissDoesNotSettle() {
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        ShortTaskSettlementService settlement = org.mockito.Mockito.mock(ShortTaskSettlementService.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "settlement", settlement);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        ShortAiTask task = new ShortAiTask();
        task.setId(9L);
        task.setTaskId("t-1");
        task.setTaskStatus("QUEUED");
        when(mapper.selectOneByExample(any())).thenReturn(task);
        when(mapper.updateByExampleSelective(any(), any())).thenReturn(0);
        worker.tick();
        verify(settlement, never()).settle(any(), any(), anyInt());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"UNKNOWN", "SHOT"})
    void invalidOrMissingTaskTargetFailsWithoutCharging(String target) {
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        ShortTaskSettlementService settlement = org.mockito.Mockito.mock(ShortTaskSettlementService.class);
        AiModelResolveService resolve = org.mockito.Mockito.mock(AiModelResolveService.class);
        AiProviderRegistry registry = org.mockito.Mockito.mock(AiProviderRegistry.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "settlement", settlement);
        ReflectionTestUtils.setField(worker, "aiModelResolveService", resolve);
        ReflectionTestUtils.setField(worker, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        ShortAiTask task = new ShortAiTask();
        task.setId(9L);
        task.setTaskId("t-1");
        task.setUserId("u-1");
        task.setTaskStatus("QUEUED");
        task.setTaskType("TEXT");
        task.setTargetType(target);
        ReflectionTestUtils.setField(worker, "shortShotMapper",
                org.mockito.Mockito.mock(com.bgssai.shortai.mapper.ShortShotMapper.class));
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
        verify(settlement, never()).settle(any(), any(), anyInt());
        org.mockito.ArgumentCaptor<ShortAiTask> patches = org.mockito.ArgumentCaptor.forClass(ShortAiTask.class);
        verify(mapper, org.mockito.Mockito.atLeastOnce()).updateByExampleSelective(patches.capture(), any());
        org.junit.jupiter.api.Assertions.assertEquals("FAILED", patches.getValue().getTaskStatus());
    }

    @Test
    void quotaShortfallFailsBeforeTheModelAndDoesNotSettle() {
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        ShortTaskSettlementService settlement = org.mockito.Mockito.mock(ShortTaskSettlementService.class);
        AiModelResolveService resolve = org.mockito.Mockito.mock(AiModelResolveService.class);
        QuotaConsumeService quota = org.mockito.Mockito.mock(QuotaConsumeService.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "settlement", settlement);
        ReflectionTestUtils.setField(worker, "aiModelResolveService", resolve);
        ReflectionTestUtils.setField(worker, "quotaConsumeService", quota);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        ShortAiTask task = new ShortAiTask();
        task.setId(9L);
        task.setTaskId("t-1");
        task.setUserId("u-1");
        task.setTaskStatus("QUEUED");
        task.setTaskType("TEXT");
        task.setTargetType("OUTLINE");
        task.setModelCode("bgssai-chat");
        task.setCostCent(1);
        when(mapper.selectOneByExample(any())).thenReturn(task);
        when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
        when(resolve.resolve(any(), any())).thenReturn(new AiModelConfig());
        doThrow(new BizException(CommonErrorCode.QUOTA_INSUFFICIENT)).when(quota).assertAffordable("u-1", 1);
        worker.tick();
        verify(settlement, never()).settle(any(), any(), anyInt());
        org.mockito.ArgumentCaptor<ShortAiTask> patches = org.mockito.ArgumentCaptor.forClass(ShortAiTask.class);
        verify(mapper, org.mockito.Mockito.atLeastOnce()).updateByExampleSelective(patches.capture(), any());
        org.junit.jupiter.api.Assertions.assertEquals("FAILED", patches.getValue().getTaskStatus());
        org.junit.jupiter.api.Assertions.assertEquals("4007", patches.getValue().getFailReasonCode());
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
