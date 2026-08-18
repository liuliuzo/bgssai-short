package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortScriptMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortScript;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ProductionDtos.ShotReq;
import com.bgssai.shortai.production.support.ShortJson;

class ShotServiceSplitTest {

    private ShortShotMapper shortShotMapper;
    private ShotService service;

    @BeforeEach
    void setUp() {
        UserContext.setUserId("u-1");
        shortShotMapper = org.mockito.Mockito.mock(ShortShotMapper.class);
        ShortEpisodeMapper episodeMapper = org.mockito.Mockito.mock(ShortEpisodeMapper.class);
        ShortScriptMapper scriptMapper = org.mockito.Mockito.mock(ShortScriptMapper.class);
        ShortWorkMapper workMapper = org.mockito.Mockito.mock(ShortWorkMapper.class);
        WorkAccessService access = new WorkAccessService();
        ReflectionTestUtils.setField(access, "shortEpisodeMapper", episodeMapper);
        ShortEpisode ep = new ShortEpisode();
        ep.setId(1L);
        ep.setEpisodeId("e-1");
        ep.setWorkId("w-1");
        ep.setUserId("u-1");
        ep.setScriptStatus("READY");
        when(episodeMapper.selectOneByExample(any())).thenReturn(ep);
        ShortScript script = new ShortScript();
        List<Map<String, Object>> scenes = new ArrayList<>();
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("scene_no", 1);
        scene.put("scene_summary", "冲突");
        List<Map<String, Object>> lines = new ArrayList<>();
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("line_text", "退婚书我签");
        lines.add(line);
        Map<String, Object> line2 = new LinkedHashMap<>();
        line2.put("line_text", "你凭什么");
        lines.add(line2);
        scene.put("line_list", lines);
        scenes.add(scene);
        script.setSceneJson(ShortJson.toJson(scenes));
        when(scriptMapper.selectOneByExample(any())).thenReturn(script);
        ShortWork work = new ShortWork();
        work.setVisualStyle("MODERN_URBAN");
        when(workMapper.selectOneByExample(any())).thenReturn(work);
        when(shortShotMapper.selectByExample(any())).thenReturn(new ArrayList<>());
        when(shortShotMapper.selectOneByExample(any())).thenReturn(null);
        service = new ShotService();
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "workStatusService", org.mockito.Mockito.mock(WorkStatusService.class));
        ReflectionTestUtils.setField(service, "shortShotMapper", shortShotMapper);
        ReflectionTestUtils.setField(service, "shortScriptMapper", scriptMapper);
        ReflectionTestUtils.setField(service, "shortEpisodeMapper", episodeMapper);
        ReflectionTestUtils.setField(service, "shortWorkMapper", workMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void splitInsertsOneShotPerLine() {
        ShotReq req = new ShotReq();
        req.setEpisodeId("e-1");
        Object result = service.split(req).getResult();
        verify(shortShotMapper, times(2)).insertSelective(any());
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals(2, map.get("shot_count"));
    }
}
