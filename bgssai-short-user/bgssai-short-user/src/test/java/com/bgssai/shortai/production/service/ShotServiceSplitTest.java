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
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
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
    private ShortScriptMapper scriptMapper;
    private ShotService service;

    @BeforeEach
    void setUp() {
        UserContext.setUserId("u-1");
        shortShotMapper = org.mockito.Mockito.mock(ShortShotMapper.class);
        ShortEpisodeMapper episodeMapper = org.mockito.Mockito.mock(ShortEpisodeMapper.class);
        scriptMapper = org.mockito.Mockito.mock(ShortScriptMapper.class);
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
        ShortCharacterMapper characterMapper = org.mockito.Mockito.mock(ShortCharacterMapper.class);
        ShortSceneCardMapper sceneCardMapper = org.mockito.Mockito.mock(ShortSceneCardMapper.class);
        when(characterMapper.selectByExample(any())).thenReturn(new ArrayList<>());
        when(sceneCardMapper.selectByExample(any())).thenReturn(new ArrayList<>());
        ReflectionTestUtils.setField(service, "shortCharacterMapper", characterMapper);
        ReflectionTestUtils.setField(service, "shortSceneCardMapper", sceneCardMapper);
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
    @Test
    void originalSixSceneMcpScriptCreatesTwelveFiveSecondShots() throws Exception {
        String fixture;
        try (var input = getClass().getResourceAsStream("/mcp/last-light-scenes.json")) {
            fixture = new String(java.util.Objects.requireNonNull(input).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        ShortScript script = new ShortScript(); script.setSceneJson(fixture);
        when(scriptMapper.selectOneByExample(any())).thenReturn(script);
        List<com.bgssai.shortai.model.ShortCharacter> cast = new ArrayList<>();
        for (String name : List.of("小夏", "老陈", "女儿")) {
            var actor = new com.bgssai.shortai.model.ShortCharacter();
            actor.setCharacterId("actor-" + cast.size()); actor.setCharacterName(name); cast.add(actor);
        }
        ShortCharacterMapper characters = (ShortCharacterMapper) ReflectionTestUtils.getField(service, "shortCharacterMapper");
        when(characters.selectByExample(any())).thenReturn(cast);
        ShotReq req = new ShotReq(); req.setEpisodeId("e-1");
        service.split(req);
        var capture = org.mockito.ArgumentCaptor.forClass(com.bgssai.shortai.model.ShortShot.class);
        verify(shortShotMapper, times(12)).insertSelective(capture.capture());
        assertEquals(60, capture.getAllValues().stream().mapToInt(com.bgssai.shortai.model.ShortShot::getShotDurationSec).sum());
        for (var shot : capture.getAllValues()) {
            org.junit.jupiter.api.Assertions.assertFalse(shot.getDialogueText().isBlank());
            org.junit.jupiter.api.Assertions.assertTrue(shot.getImagePrompt().contains(shot.getDialogueText()));
            assertEquals("PENDING", shot.getShotStatus());
        }
        assertEquals("你救别人，爸等你回家。", capture.getAllValues().get(10).getDialogueText());
        List<String> speakers = new ArrayList<>();
        for (Map<String, Object> scene : ShortJson.list(fixture)) {
            for (Object raw : (List<?>) scene.get("line_list")) {
                speakers.add(String.valueOf(((Map<?, ?>) raw).get("character_name")));
            }
        }
        for (int i = 0; i < speakers.size(); i++) {
            List<Map<String, Object>> refs = ShortJson.list(capture.getAllValues().get(i).getCharacterRefJson());
            assertEquals(speakers.get(i), refs.get(0).get("character_name"), "speaking role must lead shot " + (i + 1));
        }
    }

}
