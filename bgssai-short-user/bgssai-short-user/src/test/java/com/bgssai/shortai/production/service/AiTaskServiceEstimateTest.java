package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;
import com.bgssai.shortai.production.dto.ProductionDtos.AiTaskReq;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.quota.service.QuotaWalletService;

class AiTaskServiceEstimateTest {

    private AiTaskService service;
    private WorkAccessService workAccessService;
    private AiModelResolveService aiModelResolveService;
    private QuotaWalletService quotaWalletService;
    private QuotaConsumeService quotaConsumeService;
    private ShortShotMapper shortShotMapper;

    @BeforeEach
    void setUp() {
        service = new AiTaskService();
        workAccessService = org.mockito.Mockito.mock(WorkAccessService.class);
        aiModelResolveService = org.mockito.Mockito.mock(AiModelResolveService.class);
        quotaWalletService = org.mockito.Mockito.mock(QuotaWalletService.class);
        quotaConsumeService = org.mockito.Mockito.mock(QuotaConsumeService.class);
        shortShotMapper = org.mockito.Mockito.mock(ShortShotMapper.class);
        ReflectionTestUtils.setField(service, "workAccessService", workAccessService);
        ReflectionTestUtils.setField(service, "aiModelResolveService", aiModelResolveService);
        ReflectionTestUtils.setField(service, "quotaWalletService", quotaWalletService);
        ReflectionTestUtils.setField(service, "quotaConsumeService", quotaConsumeService);
        ReflectionTestUtils.setField(service, "shortShotMapper", shortShotMapper);
        ReflectionTestUtils.setField(service, "shortCharacterMapper",
                org.mockito.Mockito.mock(ShortCharacterMapper.class));
        ReflectionTestUtils.setField(service, "shortSceneCardMapper",
                org.mockito.Mockito.mock(ShortSceneCardMapper.class));
        ReflectionTestUtils.setField(service, "shortAiTaskMapper",
                org.mockito.Mockito.mock(ShortAiTaskMapper.class));
        ReflectionTestUtils.setField(service, "paidOrderGrantService",
                org.mockito.Mockito.mock(PaidOrderGrantService.class));
        ReflectionTestUtils.setField(service, "aiTaskQueue",
                org.mockito.Mockito.mock(AiTaskQueue.class));

        when(workAccessService.requireUser()).thenReturn("u-1");
        UserQuota quota = new UserQuota();
        quota.setBalanceCent(1000);
        quota.setExtraBalanceCent(200);
        quota.setMonthSpentCent(0);
        quota.setMonthlyLimitCent(0);
        quota.setMonthResetDate(new java.util.Date());
        when(quotaWalletService.requireWallet("u-1")).thenReturn(quota);
    }

    private AiModelConfig model(String code, String unit, int price) {
        AiModelConfig m = new AiModelConfig();
        m.setModelCode(code);
        m.setPriceUnit(unit);
        m.setUnitPriceCent(price);
        return m;
    }

    private ShortShot shot(String id, int no, int dur) {
        ShortShot s = new ShortShot();
        s.setShotId(id);
        s.setShotNo(no);
        s.setShotDurationSec(dur);
        s.setUserId("u-1");
        s.setDialogueText("台词" + no);
        return s;
    }

    @Test
    void estimateImageUsesUnitPriceTimesCountWithBreakdown() {
        when(aiModelResolveService.resolve(eq("IMAGE"), any())).thenReturn(model("img-1", "PER_CALL", 80));
        when(aiModelResolveService.costCent(any(), eq(1))).thenReturn(80);
        ShortShot a = shot("s-1", 1, 3);
        ShortShot b = shot("s-2", 2, 5);
        when(shortShotMapper.selectOneByExample(any())).thenReturn(a, b);

        AiTaskReq req = new AiTaskReq();
        req.setTaskType("IMAGE");
        req.setShotIds(List.of("s-1", "s-2"));
        ApiResponse resp = service.estimate(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        assertEquals(160, result.get("estimated_cost_cent"));
        assertEquals(2, result.get("task_count"));
        assertEquals(1200, result.get("available_cent"));
        assertEquals(1, result.get("affordable_flag"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> shots = (List<Map<String, Object>>) result.get("shot_cost_list");
        assertEquals(2, shots.size());
        assertEquals(80, shots.get(0).get("estimated_cost_cent"));
        verify(quotaConsumeService).assertAffordable("u-1", 160);
    }

    @Test
    void estimateVideoPerSecondUsesDuration() {
        when(aiModelResolveService.resolve(eq("VIDEO"), any())).thenReturn(model("vid-1", "PER_SECOND", 50));
        when(aiModelResolveService.costCent(any(), eq(4))).thenReturn(200);
        ShortShot a = shot("s-1", 1, 4);
        when(shortShotMapper.selectOneByExample(any())).thenReturn(a);

        AiTaskReq req = new AiTaskReq();
        req.setTaskType("VIDEO");
        req.setShotIds(List.of("s-1"));
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.estimate(req).getResult();
        assertEquals(200, result.get("estimated_cost_cent"));
        verify(quotaConsumeService).assertAffordable("u-1", 200);
    }

    @Test
    void estimateInsufficientThrows4007() {
        when(aiModelResolveService.resolve(eq("IMAGE"), any())).thenReturn(model("img-1", "PER_CALL", 80));
        when(aiModelResolveService.costCent(any(), anyInt())).thenReturn(80);
        when(shortShotMapper.selectOneByExample(any())).thenReturn(shot("s-1", 1, 3));
        doThrow(new BizException(com.bgssai.shortai.common.exception.CommonErrorCode.QUOTA_INSUFFICIENT))
                .when(quotaConsumeService).assertAffordable(anyString(), anyInt());

        AiTaskReq req = new AiTaskReq();
        req.setTaskType("IMAGE");
        req.setShotIds(List.of("s-1"));
        BizException ex = assertThrows(BizException.class, () -> service.estimate(req));
        assertEquals("4007", ex.getCode());
    }

    @Test
    void estimateComposeUsesEpisodeDuration() {
        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("e-1");
        when(workAccessService.requireEpisode("e-1")).thenReturn(ep);
        when(aiModelResolveService.resolve(eq("COMPOSE"), any())).thenReturn(model("ffmpeg", "PER_CALL", 0));
        when(aiModelResolveService.costCent(any(), eq(1))).thenReturn(0);
        ShortShot a = shot("s-1", 1, 3);
        ShortShot b = shot("s-2", 2, 5);
        when(shortShotMapper.selectByExample(any())).thenReturn(List.of(a, b));

        AiTaskReq req = new AiTaskReq();
        req.setEpisodeId("e-1");
        req.setTaskType("COMPOSE");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.estimate(req).getResult();
        assertEquals(80, result.get("estimated_cost_cent")); // TaskPrices.COMPOSE fallback
        assertEquals(8, result.get("duration_sec"));
        assertEquals(1, result.get("task_count"));
    }

    @Test
    void estimateVoiceSkipsBlankDialogue() {
        when(aiModelResolveService.resolve(eq("VOICE"), any())).thenReturn(model("voice-1", "PER_CALL", 20));
        when(aiModelResolveService.costCent(any(), eq(1))).thenReturn(20);
        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("e-1");
        when(workAccessService.requireEpisode("e-1")).thenReturn(ep);
        ShortShot withLine = shot("s-1", 1, 3);
        ShortShot silent = shot("s-2", 2, 3);
        silent.setDialogueText("  ");
        when(shortShotMapper.selectByExample(any())).thenReturn(List.of(withLine, silent));

        AiTaskReq req = new AiTaskReq();
        req.setEpisodeId("e-1");
        req.setTaskType("VOICE");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) service.estimate(req).getResult();
        assertEquals(1, result.get("task_count"));
        assertEquals(20, result.get("estimated_cost_cent"));
        assertTrue(((List<?>) result.get("shot_cost_list")).size() == 1);
    }
}
