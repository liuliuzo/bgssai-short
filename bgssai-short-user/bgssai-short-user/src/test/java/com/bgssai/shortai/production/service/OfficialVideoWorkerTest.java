package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.OfficialVideoStillOpenException;
import com.bgssai.shortai.billing.UsageReceiptService;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.production.support.ShortJson;

class OfficialVideoWorkerTest {

    @Test
    void aBusyOfficialLaneDoesNotMarkTheRestRunning() {
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", tasks);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        ShortAiTask running = official(1L, "RUNNING");
        ShortAiTask waiting = official(2L, "QUEUED");
        when(tasks.selectByExample(any())).thenAnswer(call -> {
            String status = statusOf(call.getArgument(0));
            if ("RUNNING".equals(status)) {
                return List.of(running);
            }
            if ("QUEUED".equals(status)) {
                return List.of(waiting);
            }
            return List.of();
        });
        worker.tick();
        verify(tasks, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void aRunningImageDoesNotBlockAQueuedOfficialVideo() {
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", tasks);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", (org.springframework.core.task.TaskExecutor) runnable -> { });
        ShortAiTask image = new ShortAiTask();
        image.setId(1L);
        image.setTaskType("IMAGE");
        image.setTaskStatus("QUEUED");
        image.setModelCode("bgssai-image");
        ShortAiTask video = official(2L, "QUEUED");
        AtomicInteger queuedReads = new AtomicInteger();
        when(tasks.selectByExample(any())).thenAnswer(call -> {
            String status = statusOf(call.getArgument(0));
            if ("RUNNING".equals(status)) {
                return List.of();
            }
            if ("QUEUED".equals(status)) {
                return queuedReads.incrementAndGet() == 1 ? List.of(image) : List.of(video);
            }
            return List.of();
        });
        when(tasks.updateByExampleSelective(any(), any())).thenReturn(1);
        worker.tick();
        worker.tick();
        ArgumentCaptor<ShortAiTask> patches = ArgumentCaptor.forClass(ShortAiTask.class);
        ArgumentCaptor<ShortAiTaskExample> claims = ArgumentCaptor.forClass(ShortAiTaskExample.class);
        verify(tasks, org.mockito.Mockito.atLeast(2)).updateByExampleSelective(patches.capture(), claims.capture());
        boolean claimedVideo = false;
        for (int i = 0; i < claims.getAllValues().size(); i++) {
            if ("RUNNING".equals(patches.getAllValues().get(i).getTaskStatus()) && hasId(claims.getAllValues().get(i), 2L)) {
                claimedVideo = true;
            }
        }
        assertTrue(claimedVideo);
    }

    @Test
    void gatewayTimeoutKeepsTheOriginalReceiptOpen() {
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShortShotMapper shots = mock(ShortShotMapper.class);
        AiModelResolveService resolve = mock(AiModelResolveService.class);
        AiProviderRegistry registry = mock(AiProviderRegistry.class);
        AiProvider provider = mock(AiProvider.class);
        UsageReceiptService receipts = mock(UsageReceiptService.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", tasks);
        ReflectionTestUtils.setField(worker, "shortShotMapper", shots);
        ReflectionTestUtils.setField(worker, "aiModelResolveService", resolve);
        ReflectionTestUtils.setField(worker, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(worker, "usageReceiptService", receipts);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        ShortAiTask task = official(9L, "QUEUED");
        task.setUserId("u");
        task.setWorkId("w");
        task.setTaskId("t-1");
        task.setTargetType("SHOT");
        task.setTargetId("shot");
        task.setRequestJson("{}");
        ShortShot shot = new ShortShot();
        shot.setShotId("shot");
        shot.setShotNo(1);
        shot.setShotDesc("人物走进宴会厅");
        when(tasks.selectByExample(any())).thenReturn(List.of(task));
        when(tasks.updateByExampleSelective(any(), any())).thenReturn(1);
        when(shots.selectOneByExample(any())).thenReturn(shot);
        AiModelConfig model = new AiModelConfig();
        model.setModelCode("bgssai-video");
        model.setProviderCode("TENCENT_TOKENHUB");
        when(resolve.resolve(any(), any())).thenReturn(model);
        when(registry.resolve(model)).thenReturn(provider);
        when(provider.execute(any())).thenThrow(new OfficialVideoStillOpenException("981ab970-447e-4a04-a2f0-a085ebbf435b"));
        worker.tick();
        ArgumentCaptor<ShortAiTask> patches = ArgumentCaptor.forClass(ShortAiTask.class);
        verify(tasks, org.mockito.Mockito.atLeastOnce()).updateByExampleSelective(patches.capture(), any());
        assertTrue(patches.getAllValues().stream().noneMatch(p -> "FAILED".equals(p.getTaskStatus())));
        ShortAiTask kept = patches.getAllValues().stream().filter(p -> p.getRequestJson() != null).findFirst().orElseThrow();
        assertEquals("981ab970-447e-4a04-a2f0-a085ebbf435b",
                ShortJson.map(kept.getRequestJson()).get("resume_request_id"));
        verify(receipts, never()).begin(any(), any(), any());
    }

    @Test
    void timeoutWithoutAPersistedReceiptIsNotLeftOpen() {
        ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        ShortShotMapper shots = mock(ShortShotMapper.class);
        AiModelResolveService resolve = mock(AiModelResolveService.class);
        AiProviderRegistry registry = mock(AiProviderRegistry.class);
        AiProvider provider = mock(AiProvider.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", tasks);
        ReflectionTestUtils.setField(worker, "shortShotMapper", shots);
        ReflectionTestUtils.setField(worker, "aiModelResolveService", resolve);
        ReflectionTestUtils.setField(worker, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
        ShortAiTask task = official(9L, "QUEUED");
        task.setUserId("u");
        task.setWorkId("w");
        task.setTaskId("t-1");
        task.setTargetType("SHOT");
        task.setTargetId("shot");
        task.setRequestJson("{}");
        ShortShot shot = new ShortShot();
        shot.setShotId("shot");
        shot.setShotNo(1);
        shot.setShotDesc("人物走进宴会厅");
        when(tasks.selectByExample(any())).thenReturn(List.of(task));
        when(tasks.updateByExampleSelective(any(), any())).thenReturn(1, 0, 1);
        when(shots.selectOneByExample(any())).thenReturn(shot);
        AiModelConfig model = new AiModelConfig();
        model.setModelCode("bgssai-video");
        model.setProviderCode("TENCENT_TOKENHUB");
        when(resolve.resolve(any(), any())).thenReturn(model);
        when(registry.resolve(model)).thenReturn(provider);
        when(provider.execute(any())).thenThrow(new OfficialVideoStillOpenException("981ab970-447e-4a04-a2f0-a085ebbf435b"));
        worker.tick();
        ArgumentCaptor<ShortAiTask> patches = ArgumentCaptor.forClass(ShortAiTask.class);
        verify(tasks, org.mockito.Mockito.atLeastOnce()).updateByExampleSelective(patches.capture(), any());
        assertTrue(patches.getAllValues().stream().anyMatch(p -> "FAILED".equals(p.getTaskStatus())));
    }

    private static ShortAiTask official(long id, String status) {
        ShortAiTask task = new ShortAiTask();
        task.setId(id);
        task.setTaskType("VIDEO");
        task.setTaskStatus(status);
        task.setModelCode("bgssai-video");
        return task;
    }

    private static boolean hasId(ShortAiTaskExample example, long id) {
        if (example == null || example.getOredCriteria().isEmpty()) {
            return false;
        }
        for (ShortAiTaskExample.Criterion criterion : example.getOredCriteria().get(0).getAllCriteria()) {
            if ("id =".equals(criterion.getCondition()) && Long.valueOf(id).equals(criterion.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static String statusOf(ShortAiTaskExample example) {
        if (example == null || example.getOredCriteria().isEmpty()) {
            return "";
        }
        for (ShortAiTaskExample.Criterion criterion : example.getOredCriteria().get(0).getAllCriteria()) {
            if ("task_status =".equals(criterion.getCondition())) {
                return String.valueOf(criterion.getValue());
            }
        }
        return "";
    }
}
