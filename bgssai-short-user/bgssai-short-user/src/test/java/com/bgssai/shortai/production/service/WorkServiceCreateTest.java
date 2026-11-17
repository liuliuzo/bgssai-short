package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortShot;
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

    @Test
    @SuppressWarnings("unchecked")
    void detailIncludesStageCountsAndFilmExpiry() {
        ShortCharacterMapper charMapper = org.mockito.Mockito.mock(ShortCharacterMapper.class);
        ShortSceneCardMapper sceneMapper = org.mockito.Mockito.mock(ShortSceneCardMapper.class);
        ShortShotMapper shotMapper = org.mockito.Mockito.mock(ShortShotMapper.class);
        ShortFilmMapper filmMapper = org.mockito.Mockito.mock(ShortFilmMapper.class);
        ReflectionTestUtils.setField(service, "shortCharacterMapper", charMapper);
        ReflectionTestUtils.setField(service, "shortSceneCardMapper", sceneMapper);
        ReflectionTestUtils.setField(service, "shortShotMapper", shotMapper);
        ReflectionTestUtils.setField(service, "shortFilmMapper", filmMapper);

        ShortWork work = new ShortWork();
        work.setWorkId("w-1");
        work.setUserId("u-1");
        work.setWorkTitle("测试作品");
        work.setEpisodeCount(2);
        work.setOutlineJson("[{\"episode_no\":1},{\"episode_no\":2}]");
        when(shortWorkMapper.selectOneByExample(any())).thenReturn(work);

        ShortEpisode ep1 = new ShortEpisode();
        ep1.setEpisodeId("e-1");
        ep1.setEpisodeNo(1);
        ep1.setScriptStatus("READY");
        ep1.setComposeStatus("READY");
        ShortEpisode ep2 = new ShortEpisode();
        ep2.setEpisodeId("e-2");
        ep2.setEpisodeNo(2);
        ep2.setScriptStatus("NONE");
        ep2.setComposeStatus("NONE");
        when(shortEpisodeMapper.selectByExample(any())).thenReturn(List.of(ep1, ep2));

        ShortCharacter c1 = new ShortCharacter();
        c1.setCardStatus("LOCKED");
        c1.setRefImageAssetId("as-1");
        ShortCharacter c2 = new ShortCharacter();
        c2.setCardStatus("DRAFT");
        when(charMapper.selectByExample(any())).thenReturn(List.of(c1, c2));

        ShortSceneCard sc1 = new ShortSceneCard();
        sc1.setCardStatus("LOCKED");
        sc1.setRefImageAssetId("as-2");
        when(sceneMapper.selectByExample(any())).thenReturn(List.of(sc1));

        ShortShot s1 = new ShortShot();
        s1.setEpisodeId("e-1");
        s1.setShotStatus("VIDEO_READY");
        s1.setFirstFrameAssetId("f-1");
        s1.setVideoAssetId("v-1");
        s1.setPrevizText("镜1画面足够长");
        ShortShot s2 = new ShortShot();
        s2.setEpisodeId("e-1");
        s2.setShotStatus("IMAGE_READY");
        s2.setFirstFrameAssetId("f-2");
        s2.setShotDesc("镜2画面描述足够长");
        ShortShot s3 = new ShortShot();
        s3.setEpisodeId("e-1");
        s3.setShotStatus("PENDING");
        when(shotMapper.selectByExample(any())).thenReturn(List.of(s1, s2, s3));

        ShortFilm f1 = new ShortFilm();
        f1.setEpisodeId("e-1");
        f1.setExpiredFlag(1);
        ShortFilm f2 = new ShortFilm();
        f2.setEpisodeId("e-2");
        f2.setExpiredFlag(0);
        when(filmMapper.selectByExample(any())).thenReturn(List.of(f1, f2));

        Map<String, Object> detail = (Map<String, Object>) service.detail("w-1").getResult();
        assertEquals(2, detail.get("outline_count"));
        assertEquals(2, detail.get("character_count"));
        assertEquals(1, detail.get("scene_card_count"));
        assertEquals(3, detail.get("look_total"));
        assertEquals(2, detail.get("look_locked"));

        List<Map<String, Object>> episodes = (List<Map<String, Object>>) detail.get("episode_list");
        assertEquals(2, episodes.size());
        Map<String, Object> item1 = episodes.get(0);
        assertEquals(3, item1.get("shot_total"));
        assertEquals(2, item1.get("shot_frame_ready"));
        assertEquals(1, item1.get("shot_ready"));
        assertEquals(2, item1.get("shot_spoken_ready"));
        assertEquals(1, item1.get("film_expired_flag"));

        Map<String, Object> item2 = episodes.get(1);
        assertEquals(0, item2.get("shot_total"));
        assertEquals(0, item2.get("shot_frame_ready"));
        assertEquals(0, item2.get("shot_ready"));
        assertEquals(0, item2.get("shot_spoken_ready"));
        assertEquals(0, item2.get("film_expired_flag"));
    }
}
