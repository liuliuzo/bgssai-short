package com.bgssai.shortai.production.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
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
        ShortAiTask task = new ShortAiTask();
        task.setId(9L);
        task.setTaskId("t-1");
        task.setUserId("u-1");
        task.setTaskStatus("QUEUED");
        task.setTaskType("TEXT");
        task.setTargetType("UNKNOWN");
        task.setModelCode("mock-text");
        task.setCostCent(15);
        AiModelConfig model = new AiModelConfig();
        model.setModelCode("mock-text");
        model.setProviderCode("MOCK");
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
}
