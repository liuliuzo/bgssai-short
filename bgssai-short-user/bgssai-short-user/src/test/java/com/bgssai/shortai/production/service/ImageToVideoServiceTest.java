package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aiprovider.ImageConditionedVideoClient;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.ContentDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.ContentKind;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.PollDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.PollKind;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.SubmitDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.SubmitKind;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProvider;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.production.dto.ImageToVideoDtos.SubmitReq;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.storage.ObjectStorage;

class ImageToVideoServiceTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
    private static final String JOB = "i2v_" + "ef".repeat(16);

    private ImageToVideoService service;
    private ImageConditionedVideoClient client;
    private ShortAiTaskMapper tasks;
    private ShortAssetMapper assets;
    private ShortShotMapper shots;
    private AiModelConfigMapper models;
    private ObjectStorage storage;
    private ShortTaskSettlementService settlement;
    private QuotaConsumeService quota;
    private final AtomicReference<ShortAiTask> stored = new AtomicReference<>();
    private final AtomicReference<Long> active = new AtomicReference<>(0L);
    private ShortAsset image;
    private AiModelConfig model;

    @BeforeEach
    void setUp() {
        service = new ImageToVideoService();
        client = mock(ImageConditionedVideoClient.class);
        tasks = mock(ShortAiTaskMapper.class);
        assets = mock(ShortAssetMapper.class);
        shots = mock(ShortShotMapper.class);
        models = mock(AiModelConfigMapper.class);
        storage = mock(ObjectStorage.class);
        settlement = mock(ShortTaskSettlementService.class);
        quota = mock(QuotaConsumeService.class);
        WorkAccessService access = mock(WorkAccessService.class);
        Environment environment = mock(Environment.class);
        AssetService assetService = mock(AssetService.class);
        when(access.requireUser()).thenReturn("u-1");
        when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
        when(assetService.resolveUrl(any())).thenReturn("/bgssai/user/assets/preview");
        image = new ShortAsset();
        image.setAssetId("a-1");
        image.setUserId("u-1");
        image.setAssetType("IMAGE");
        image.setStorageKey("short/test/u-1/library/a-1.png");
        image.setMimeType("image/png");
        model = new AiModelConfig();
        model.setModelId(ImageConditionedVideoProvider.MODEL_ID);
        model.setModelCode(ImageConditionedVideoProvider.MODEL_CODE);
        model.setProviderCode(ImageConditionedVideoProvider.CODE);
        model.setEnabledFlag(1);
        model.setUnitPriceCent(0);
        model.setPriceUnit("PER_CALL");
        model.setConfigJson("{\"secret_ref\":\"ai.image_to_video.api_key\"}");
        when(models.selectOneByExample(any(AiModelConfigExample.class))).thenReturn(model);
        when(client.ready(any())).thenReturn(true);
        when(client.callable(any())).thenReturn(true);
        when(storage.get(any())).thenReturn(PNG);
        when(storage.put(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(assets.selectOneByExample(any(ShortAssetExample.class))).thenAnswer(invocation -> {
            if (conditions(invocation.getArgument(0)).contains("source_task_id")) {
                return null;
            }
            return image;
        });
        when(tasks.selectOneByExample(any(ShortAiTaskExample.class))).thenAnswer(invocation -> {
            if (conditions(invocation.getArgument(0)).contains("target_id")) {
                return stored.get() != null && "QUEUED".equals(stored.get().getTaskStatus())
                        || stored.get() != null ? clientMatch(invocation.getArgument(0)) : null;
            }
            return stored.get();
        });
        when(tasks.countByExample(any())).thenAnswer(invocation -> active.get());
        when(tasks.insertSelective(any())).thenAnswer(invocation -> {
            ShortAiTask row = invocation.getArgument(0);
            row.setId(7L);
            stored.set(row);
            return 1;
        });
        when(tasks.selectByPrimaryKey(any())).thenAnswer(invocation -> stored.get());
        when(tasks.updateByExampleSelective(any(), any())).thenAnswer(invocation -> {
            apply(stored.get(), invocation.getArgument(0));
            return 1;
        });
        when(tasks.selectByExample(any())).thenAnswer(invocation -> stored.get() == null ? List.of() : List.of(stored.get()));
        when(assets.insertSelective(any())).thenReturn(1);
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "videoClient", client);
        ReflectionTestUtils.setField(service, "aiModelConfigMapper", models);
        ReflectionTestUtils.setField(service, "shortAiTaskMapper", tasks);
        ReflectionTestUtils.setField(service, "shortAssetMapper", assets);
        ReflectionTestUtils.setField(service, "shortShotMapper", shots);
        ReflectionTestUtils.setField(service, "objectStorage", storage);
        ReflectionTestUtils.setField(service, "settlement", settlement);
        ReflectionTestUtils.setField(service, "quotaConsumeService", quota);
        ReflectionTestUtils.setField(service, "assetService", assetService);
        ReflectionTestUtils.setField(service, "environment", environment);
        service.setMediaCheck((result, ability) -> {
            result.setWidth(384);
            result.setHeight(256);
            result.setDurationSec(3);
        });
    }

    @Test
    void unavailableDoesNotCreateOrCall() {
        when(client.ready(any())).thenReturn(false);
        BizException ex = assertThrows(BizException.class, () -> service.submit(request("a person turns", 384)));
        assertEquals(CommonErrorCode.ABILITY_UNAVAILABLE.code(), ex.getCode());
        assertFalse(ex.getMessage().toLowerCase().contains("ltx"));
        verify(tasks, never()).insertSelective(any());
        verify(client, never()).submit(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void outOfRangeIsRejectedWithoutClamping() {
        BizException ex = assertThrows(BizException.class, () -> service.submit(request("a person turns", 1920)));
        assertEquals(CommonErrorCode.INVALID_PARAM.code(), ex.getCode());
        verify(tasks, never()).insertSelective(any());
        verify(client, never()).submit(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void otherUsersAssetAndShotAreHidden() {
        image.setUserId("u-2");
        assertThrows(ForbiddenException.class, () -> service.submit(request("a person turns", 384)));
        image.setUserId("u-1");
        ShortShot shot = new ShortShot();
        shot.setShotId("s-9");
        shot.setUserId("u-2");
        when(shots.selectOneByExample(any(ShortShotExample.class))).thenReturn(shot);
        SubmitReq foreignShot = request("a person turns", 384);
        foreignShot.setAssetId(null);
        foreignShot.setShotId("s-9");
        assertThrows(ForbiddenException.class, () -> service.submit(foreignShot));
        verify(tasks, never()).insertSelective(any());
    }

    @Test
    void userImageUrlIsNotFetched() {
        image.setStorageKey("HTTPS://evil.example/a.png");
        BizException upper = assertThrows(BizException.class, () -> service.submit(request("a person turns", 384)));
        assertEquals(CommonErrorCode.RULE_REJECT.code(), upper.getCode());
        image.setStorageKey("//evil.example/a.png");
        assertThrows(BizException.class, () -> service.submit(request("a person turns", 384)));
        verify(storage, never()).get(any());
        verify(client, never()).submit(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void busyAndSameRequestStaySingle() {
        active.set(1L);
        BizException busy = assertThrows(BizException.class, () -> service.submit(request("a person turns", 384)));
        assertEquals(CommonErrorCode.RATE_LIMIT_EXCEEDED.code(), busy.getCode());
        verify(client, never()).submit(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        active.set(0L);
        Map<?, ?> first = map(service.submit(request("a person turns", 384)).getResult());
        Map<?, ?> again = map(service.submit(request("a person turns", 384)).getResult());
        assertEquals(first.get("task_id"), again.get("task_id"));
        verify(tasks, times(1)).insertSelective(any());
        SubmitReq changed = request("a different motion", 384);
        assertThrows(BizException.class, () -> service.submit(changed));
        verify(tasks, times(1)).insertSelective(any());
    }

    @Test
    void failureDoesNotSettleAndSuccessSettlesOnce() {
        Map<?, ?> queued = map(service.submit(request("a person turns", 384)).getResult());
        assertEquals("bgssai-video", queued.get("model_code"));
        assertEquals("BGSSAI Video", queued.get("display_name"));
        assertEquals(1, queued.get("ai_generated"));
        assertFalse(queued.containsKey("provider_job_id"));
        stored.get().setTaskStatus("RUNNING");
        stored.get().setStartedTime(new Date());
        when(client.submit(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new SubmitDecision(SubmitKind.ACCEPTED, JOB, "", ""));
        service.kickoff(stored.get());
        assertTrue(stored.get().getRequestJson().contains(JOB));
        dueNow();
        when(client.poll(any(), eq(JOB))).thenReturn(new PollDecision(PollKind.FAILED, "I2V_FAILED", "ltx-video exploded"));
        service.pollImageToVideo();
        assertEquals("FAILED", stored.get().getTaskStatus());
        assertFalse(stored.get().getFailReason().toLowerCase().contains("ltx"));
        verify(client, never()).fetch(any(), any());
        verify(settlement, never()).settle(any(), any(), anyInt());
        stored.get().setTaskStatus("RUNNING");
        stored.get().setFailReason(null);
        stored.get().setFailReasonCode(null);
        dueNow();
        when(client.poll(any(), eq(JOB))).thenReturn(new PollDecision(PollKind.COMPLETED, "", ""));
        when(client.fetch(any(), eq(JOB))).thenReturn(new ContentDecision(ContentKind.READY,
                new byte[] {0, 0, 0, 20, 'f', 't', 'y', 'p'}, "", ""));
        when(settlement.settle(any(), any(), anyInt())).thenAnswer(invocation -> {
            stored.get().setTaskStatus("SUCCEEDED");
            return true;
        });
        service.pollImageToVideo();
        service.pollImageToVideo();
        verify(settlement, times(1)).settle(any(), any(), anyInt());
        verify(quota, times(1)).assertAffordable(eq("u-1"), eq(0));
        stored.get().setResultJson("{\"video_asset_id\":\"a-v\",\"metrics\":{\"dtype\":\"bf16\"},\"provider_job_id\":\""
                + JOB + "\",\"base_url\":\"http://127.0.0.1:8007\"}");
        Map<?, ?> done = map(service.status(String.valueOf(queued.get("task_id"))).getResult());
        assertEquals("bgssai-video", done.get("model_code"));
        assertEquals(1, done.get("ai_generated"));
        assertFalse(done.containsKey("metrics"));
        assertFalse(done.containsKey("provider_job_id"));
        assertFalse(String.valueOf(done).contains("127.0.0.1"));
        assertFalse(String.valueOf(done).toLowerCase().contains("ltx"));
    }

    @Test
    void ambiguousSubmitIsNotSentAgain() {
        service.submit(request("a person turns", 384));
        stored.get().setTaskStatus("RUNNING");
        stored.get().setStartedTime(new Date());
        when(client.submit(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new SubmitDecision(SubmitKind.AMBIGUOUS, "", "I2V_AMBIGUOUS",
                        ImageConditionedVideoProtocol.AMBIGUOUS));
        service.kickoff(stored.get());
        service.kickoff(stored.get());
        assertEquals("I2V_AMBIGUOUS", stored.get().getFailReasonCode());
        verify(client, times(1)).submit(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(settlement, never()).settle(any(), any(), anyInt());
    }

    @Test
    void officialShotPreservesBatchIdAndExpiresFilmOnSuccess() {
        FinishedFilmLifecycle filmLifecycle = mock(FinishedFilmLifecycle.class);
        ReflectionTestUtils.setField(service, "finishedFilmLifecycle", filmLifecycle);
        ShortShot shot = new ShortShot();
        shot.setId(11L);
        shot.setShotId("s-11");
        shot.setUserId("u-1");
        shot.setWorkId("w-1");
        shot.setEpisodeId("e-1");
        shot.setShotStatus("VIDEO_GENERATING");
        shot.setStaleFlag(1);
        when(shots.selectOneByExample(any(ShortShotExample.class))).thenReturn(shot);
        when(shots.updateByExampleSelective(any(), any())).thenReturn(1);

        ShortAiTask queued = service.enqueueForShot(shot, "a-1", "镜头缓推", 80, "b-official-1");
        assertEquals("b-official-1", queued.getBatchId());
        assertEquals("s-11", queued.getTargetId());

        stored.get().setTaskStatus("RUNNING");
        stored.get().setStartedTime(new Date());
        when(client.submit(any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new SubmitDecision(SubmitKind.ACCEPTED, JOB, "", ""));
        service.kickoff(stored.get());
        dueNow();
        when(client.poll(any(), eq(JOB))).thenReturn(new PollDecision(PollKind.COMPLETED, "", ""));
        when(client.fetch(any(), eq(JOB))).thenReturn(new ContentDecision(ContentKind.READY,
                new byte[] {0, 0, 0, 20, 'f', 't', 'y', 'p'}, "", ""));
        when(settlement.settle(any(), any(), anyInt())).thenReturn(true);
        service.pollImageToVideo();

        org.mockito.ArgumentCaptor<ShortShot> shotPatch = org.mockito.ArgumentCaptor.forClass(ShortShot.class);
        verify(shots).updateByExampleSelective(shotPatch.capture(), any());
        assertEquals("VIDEO_READY", shotPatch.getValue().getShotStatus());
        assertEquals(0, shotPatch.getValue().getStaleFlag());
        verify(filmLifecycle).expireAfterUpstreamChange("e-1");
    }

    private void dueNow() {
        Map<String, Object> body = ShortJson.map(stored.get().getRequestJson());
        body.put("next_poll_at_ms", 0);
        stored.get().setRequestJson(ShortJson.toJson(body));
    }

    private static SubmitReq request(String prompt, int width) {
        SubmitReq req = new SubmitReq();
        req.setAssetId("a-1");
        req.setPrompt(prompt);
        req.setWidth(width);
        req.setClientRequestId("req12345678");
        return req;
    }

    private ShortAiTask clientMatch(ShortAiTaskExample example) {
        ShortAiTask row = stored.get();
        if (row == null) {
            return null;
        }
        String text = conditions(example);
        return text.contains("target_id") ? row : null;
    }

    private static void apply(ShortAiTask row, ShortAiTask patch) {
        if (row == null || patch == null) {
            return;
        }
        if (patch.getTaskStatus() != null) {
            row.setTaskStatus(patch.getTaskStatus());
        }
        if (patch.getRequestJson() != null) {
            row.setRequestJson(patch.getRequestJson());
        }
        if (patch.getResultJson() != null) {
            row.setResultJson(patch.getResultJson());
        }
        if (patch.getFailReason() != null) {
            row.setFailReason(patch.getFailReason());
        }
        if (patch.getFailReasonCode() != null) {
            row.setFailReasonCode(patch.getFailReasonCode());
        }
        if (patch.getProgressPercent() != null) {
            row.setProgressPercent(patch.getProgressPercent());
        }
    }

    private static String conditions(Object example) {
        StringBuilder text = new StringBuilder();
        List<?> groups;
        if (example instanceof ShortAssetExample assetExample) {
            groups = assetExample.getOredCriteria();
        } else if (example instanceof ShortAiTaskExample taskExample) {
            groups = taskExample.getOredCriteria();
        } else {
            return "";
        }
        for (Object group : groups) {
            List<?> criteria = group instanceof ShortAssetExample.Criteria assetCriteria
                    ? assetCriteria.getAllCriteria()
                    : ((ShortAiTaskExample.Criteria) group).getAllCriteria();
            for (Object criterion : criteria) {
                if (criterion instanceof ShortAssetExample.Criterion assetCriterion) {
                    text.append(assetCriterion.getCondition()).append(';');
                } else if (criterion instanceof ShortAiTaskExample.Criterion taskCriterion) {
                    text.append(taskCriterion.getCondition()).append(';');
                }
            }
        }
        return text.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
