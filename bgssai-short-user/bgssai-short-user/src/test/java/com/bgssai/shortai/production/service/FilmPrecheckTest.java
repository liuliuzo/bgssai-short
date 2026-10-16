package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;

/**
 * 成片合成预检：一个镜头都没有的剧集不得判为可合成（否则空集合成照常扣费）。
 */
class FilmPrecheckTest {

    private FilmService service;
    private ShortShotMapper shotMapper;

    @BeforeEach
    void setUp() {
        service = new FilmService();
        WorkAccessService access = mock(WorkAccessService.class);
        shotMapper = mock(ShortShotMapper.class);
        ShortDubbingMapper dubbingMapper = mock(ShortDubbingMapper.class);
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "shortShotMapper", shotMapper);
        ReflectionTestUtils.setField(service, "shortDubbingMapper", dubbingMapper);
        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("ep-1");
        ep.setWorkId("w-1");
        when(access.requireEpisode("ep-1")).thenReturn(ep);
        when(dubbingMapper.selectByExample(any())).thenReturn(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> precheck() {
        return (Map<String, Object>) service.precheck("ep-1").getResult();
    }

    @Test
    void episode_without_shots_is_not_ready() {
        when(shotMapper.selectByExample(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = precheck();

        assertFalse((Boolean) result.get("ready"));
        List<Map<String, Object>> blockers = (List<Map<String, Object>>) result.get("blocker_list");
        assertEquals("NO_SHOTS", blockers.get(0).get("blocker_code"));
    }

    @Test
    void episode_whose_shots_all_have_video_is_ready() {
        ShortShot shot = new ShortShot();
        shot.setShotId("s-1");
        shot.setShotNo(1);
        shot.setSceneNo(1);
        shot.setShotStatus("VIDEO_READY");
        shot.setVideoAssetId("a-video-1");
        shot.setDialogueText("你好");
        when(shotMapper.selectByExample(any())).thenReturn(List.of(shot));

        assertTrue((Boolean) precheck().get("ready"));
    }
}
