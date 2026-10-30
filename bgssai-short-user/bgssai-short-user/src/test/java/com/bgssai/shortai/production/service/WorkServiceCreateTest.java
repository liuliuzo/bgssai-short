package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ProductionDtos.WorkCreateReq;
import com.bgssai.shortai.quota.service.QuotaConsumeService;

class WorkServiceCreateTest {

    private ShortWorkMapper shortWorkMapper;
    private ShortEpisodeMapper shortEpisodeMapper;
    private AiTaskQueue aiTaskQueue;
    private QuotaConsumeService quotaConsumeService;
    private WorkService service;

    @BeforeEach
    void setUp() {
        shortWorkMapper = org.mockito.Mockito.mock(ShortWorkMapper.class);
        shortEpisodeMapper = org.mockito.Mockito.mock(ShortEpisodeMapper.class);
        aiTaskQueue = org.mockito.Mockito.mock(AiTaskQueue.class);
        quotaConsumeService = org.mockito.Mockito.mock(QuotaConsumeService.class);
        AiModelResolveService models = org.mockito.Mockito.mock(AiModelResolveService.class);
        AiModelConfig text = new AiModelConfig();
        text.setModelCode("bgssai-chat");
        text.setUnitPriceCent(1);
        when(models.resolve("TEXT", null)).thenReturn(text);
        when(models.costCent(text, 1)).thenReturn(1);
        WorkAccessService access = new WorkAccessService();
        ReflectionTestUtils.setField(access, "shortWorkMapper", shortWorkMapper);
        ReflectionTestUtils.setField(access, "shortEpisodeMapper", shortEpisodeMapper);
        service = new WorkService();
        ReflectionTestUtils.setField(service, "shortWorkMapper", shortWorkMapper);
        ReflectionTestUtils.setField(service, "shortEpisodeMapper", shortEpisodeMapper);
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "workStatusService", org.mockito.Mockito.mock(WorkStatusService.class));
        ReflectionTestUtils.setField(service, "aiTaskQueue", aiTaskQueue);
        ReflectionTestUtils.setField(service, "aiModelResolveService", models);
        ReflectionTestUtils.setField(service, "quotaConsumeService", quotaConsumeService);
        UserContext.setUserId("u-1");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createUsesDefaults() {
        ShortAiTask task = new ShortAiTask();
        task.setTaskId("t-1");
        when(aiTaskQueue.enqueue(any(), any(), org.mockito.ArgumentMatchers.nullable(String.class), any(), any(), any(),
                any(), org.mockito.ArgumentMatchers.nullable(String.class), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(task);
        WorkCreateReq req = new WorkCreateReq();
        req.setWorkTitle("退婚三年后她成了首富");
        req.setOneLinePitch("被退婚的落魄千金三年后带着资本回归");
        service.create(req);
        ArgumentCaptor<ShortWork> captor = ArgumentCaptor.forClass(ShortWork.class);
        org.mockito.Mockito.verify(shortWorkMapper).insertSelective(captor.capture());
        ShortWork work = captor.getValue();
        assertEquals("REVENGE", work.getGenre());
        assertEquals(Integer.valueOf(24), work.getEpisodeCount());
        assertEquals(Integer.valueOf(90), work.getEpisodeDurationSec());
        assertEquals("VERTICAL_9_16", work.getAspectRatio());
        assertEquals("MODERN_URBAN", work.getVisualStyle());
        org.mockito.Mockito.verify(shortEpisodeMapper, org.mockito.Mockito.times(24)).insertSelective(any());
        org.mockito.Mockito.verify(quotaConsumeService).assertAffordable("u-1", 1);
    }

    @Test
    void createDoesNotEnqueueWhenQuotaShort() {
        org.mockito.Mockito.doThrow(new BizException(
                com.bgssai.shortai.common.exception.CommonErrorCode.QUOTA_INSUFFICIENT))
                .when(quotaConsumeService).assertAffordable(any(), org.mockito.ArgumentMatchers.anyInt());
        WorkCreateReq req = new WorkCreateReq();
        req.setWorkTitle("退婚三年后她成了首富");
        req.setOneLinePitch("被退婚的落魄千金三年后带着资本回归");
        BizException ex = assertThrows(BizException.class, () -> service.create(req));
        assertEquals("4007", ex.getCode());
        org.mockito.Mockito.verify(aiTaskQueue, org.mockito.Mockito.never()).enqueue(any(), any(),
                org.mockito.ArgumentMatchers.nullable(String.class), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.nullable(String.class), org.mockito.ArgumentMatchers.anyInt());
        org.mockito.Mockito.verify(shortWorkMapper, org.mockito.Mockito.never()).insertSelective(any());
    }

    @Test
    void otherUserWorkIsForbidden() {
        WorkAccessService access = new WorkAccessService();
        ReflectionTestUtils.setField(access, "shortWorkMapper", shortWorkMapper);
        ShortWork work = new ShortWork();
        work.setWorkId("w-1");
        work.setUserId("other");
        when(shortWorkMapper.selectOneByExample(any())).thenReturn(work);
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> access.requireWork("w-1"));
        assertEquals("4003", ex.getCode());
    }
}
