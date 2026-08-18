package com.bgssai.shortai.production.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.quota.service.QuotaConsumeService;

class ShortAiTaskWorkerTest {

    @Test
    void casMissDoesNotConsume() {
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        QuotaConsumeService consume = org.mockito.Mockito.mock(QuotaConsumeService.class);
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "quotaConsumeService", consume);
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
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker, "shortAiTaskMapper", mapper);
        ReflectionTestUtils.setField(worker, "quotaConsumeService", consume);
        ShortAiTask task = new ShortAiTask();
        task.setId(9L);
        task.setTaskId("t-1");
        task.setUserId("u-1");
        task.setTaskStatus("QUEUED");
        task.setTargetType("UNKNOWN");
        task.setCostCent(15);
        when(mapper.selectOneByExample(any())).thenReturn(task);
        when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
        worker.tick();
        verify(consume).consume("u-1", 15);
    }
}
