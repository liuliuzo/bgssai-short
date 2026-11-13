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

    @Test
    void splitRequiresReadyScriptWithChineseGuidanceAndListExposesBlockReason() {
        ShortEpisodeMapper episodeMapper = (ShortEpisodeMapper) ReflectionTestUtils.getField(service, "shortEpisodeMapper");
        ShortEpisode draftEp = new ShortEpisode();
        draftEp.setId(2L);
        draftEp.setEpisodeId("e-draft");
        draftEp.setWorkId("w-1");
        draftEp.setUserId("u-1");
        draftEp.setScriptStatus("DRAFT");
        when(episodeMapper.selectOneByExample(any())).thenReturn(draftEp);
        ShotReq draftReq = new ShotReq();
        draftReq.setEpisodeId("e-draft");
        var ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.bgssai.shortai.common.exception.BizException.class,
                () -> service.split(draftReq));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("定稿"));

        ShortEpisode readyEp = new ShortEpisode();
        readyEp.setId(1L);
        readyEp.setEpisodeId("e-1");
        readyEp.setWorkId("w-1");
        readyEp.setUserId("u-1");
        readyEp.setScriptStatus("READY");
        when(episodeMapper.selectOneByExample(any())).thenReturn(readyEp);

        var unlockedChar = new com.bgssai.shortai.model.ShortCharacter();
        unlockedChar.setCharacterId("c-1");
        unlockedChar.setCharacterName("沈知微");
        unlockedChar.setCardStatus("DRAFT");
        var unlockedScene = new com.bgssai.shortai.model.ShortSceneCard();
        unlockedScene.setSceneCardId("sc-1");
        unlockedScene.setCardType("SCENE");
        unlockedScene.setCardName("宴会厅");
        unlockedScene.setCardStatus("CANDIDATE");

        ShortCharacterMapper characterMapper = (ShortCharacterMapper) ReflectionTestUtils.getField(service, "shortCharacterMapper");
        ShortSceneCardMapper sceneCardMapper = (ShortSceneCardMapper) ReflectionTestUtils.getField(service, "shortSceneCardMapper");
        when(characterMapper.selectByExample(any())).thenReturn(List.of(unlockedChar));
        when(sceneCardMapper.selectByExample(any())).thenReturn(List.of(unlockedScene));

        var shot = new com.bgssai.shortai.model.ShortShot();
        shot.setShotId("sh-1");
        shot.setShotNo(1);
        shot.setSceneNo(1);
        shot.setWorkId("w-1");
        shot.setEpisodeId("e-1");
        shot.setUserId("u-1");
        shot.setShotStatus("PENDING");
        shot.setShotDesc("沈知微步入宴会厅");
        shot.setImagePrompt("沈知微步入宴会厅");
        shot.setCharacterRefJson("[\"c-1\"]");
        shot.setSceneCardId("sc-1");
        when(shortShotMapper.selectByExample(any())).thenReturn(List.of(shot));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listed = (List<Map<String, Object>>) service.list("e-1", 1, 20, null).getResult();
        assertEquals(1, listed.size());
        assertEquals(1, listed.get(0).get("blocked_flag"));
        String reason = String.valueOf(listed.get(0).get("block_reason"));
        org.junit.jupiter.api.Assertions.assertTrue(reason.contains("沈知微"));
        org.junit.jupiter.api.Assertions.assertTrue(reason.contains("宴会厅"));

        unlockedChar.setCardStatus("LOCKED");
        unlockedChar.setRefImageAssetId("a-char");
        unlockedScene.setCardStatus("LOCKED");
        unlockedScene.setRefImageAssetId("a-scene");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> unblocked = (List<Map<String, Object>>) service.list("e-1", 1, 20, null).getResult();
        assertEquals(0, unblocked.get(0).get("blocked_flag"));
        assertEquals("", unblocked.get(0).get("block_reason"));
    }

}
