package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortContinuityIssueMapper;
import com.bgssai.shortai.mapper.ShortEpisodeGapMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortContinuityIssue;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ProductionDtos.CharacterReq;

class CharacterServiceContinuityTest {

    @Test
    void updateCharacterMarksReferencedShotsStaleAndPersistsContinuityIssueAcrossRestart() throws Exception {
        WorkAccessService workAccessService = mock(WorkAccessService.class);
        ShortCharacterMapper characterMapper = mock(ShortCharacterMapper.class);
        ShortShotMapper shotMapper = mock(ShortShotMapper.class);
        ShortContinuityIssueMapper issueMapper = mock(ShortContinuityIssueMapper.class);
        ShortEpisodeGapMapper gapMapper = mock(ShortEpisodeGapMapper.class);

        when(workAccessService.requireUser()).thenReturn("u-1");
        ShortWork work = new ShortWork();
        work.setWorkId("w-1");
        work.setUserId("u-1");
        when(workAccessService.requireWork("w-1")).thenReturn(work);

        ShortCharacter character = new ShortCharacter();
        character.setId(10L);
        character.setCharacterId("c-hero");
        character.setWorkId("w-1");
        character.setUserId("u-1");
        character.setCharacterName("阿青");
        character.setAppearancePrompt("old coat");
        when(characterMapper.selectOneByExample(any())).thenReturn(character);

        ShortShot shot = new ShortShot();
        shot.setId(20L);
        shot.setShotId("s-101");
        shot.setShotNo(1);
        shot.setEpisodeId("ep-1");
        shot.setWorkId("w-1");
        shot.setShotStatus("READY");
        shot.setCharacterRefJson("[\"c-hero\"]");
        when(shotMapper.selectByExample(any())).thenReturn(List.of(shot));

        List<ShortContinuityIssue> persistedIssues = new ArrayList<>();
        when(issueMapper.insertSelective(any())).thenAnswer(invocation -> {
            persistedIssues.add(invocation.getArgument(0));
            return 1;
        });
        when(issueMapper.selectByExample(any())).thenAnswer(invocation -> List.copyOf(persistedIssues));
        when(gapMapper.selectByExample(any())).thenReturn(List.of());

        CharacterService characterService = new CharacterService();
        inject(characterService, "workAccessService", workAccessService);
        inject(characterService, "shortCharacterMapper", characterMapper);
        inject(characterService, "shortShotMapper", shotMapper);
        inject(characterService, "shortSceneCardMapper", mock(com.bgssai.shortai.mapper.ShortSceneCardMapper.class));
        inject(characterService, "shortContinuityIssueMapper", issueMapper);

        CharacterReq req = new CharacterReq();
        req.setCharacterId("c-hero");
        req.setAppearancePrompt("black trenchcoat");
        characterService.updateCharacter(req);

        ArgumentCaptor<ShortShot> shotPatch = ArgumentCaptor.forClass(ShortShot.class);
        verify(shotMapper).updateByExampleSelective(shotPatch.capture(), any());
        assertEquals(1, shotPatch.getValue().getStaleFlag());
        assertEquals(1, persistedIssues.size());
        assertEquals("CHARACTER", persistedIssues.get(0).getIssueType());
        assertEquals("s-101", persistedIssues.get(0).getShotId());

        // New ContinuityService instance after restart reads persisted issue state
        ContinuityService continuityService = new ContinuityService();
        inject(continuityService, "workAccessService", workAccessService);
        inject(continuityService, "shortContinuityIssueMapper", issueMapper);
        inject(continuityService, "shortEpisodeGapMapper", gapMapper);
        inject(continuityService, "shortShotMapper", shotMapper);

        ApiResponse overview = continuityService.overview("w-1");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) overview.getResult();
        assertEquals(1, data.get("open_issue_count"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issueList = (List<Map<String, Object>>) data.get("issue_list");
        assertEquals(1, issueList.size());
        assertTrue(String.valueOf(issueList.get(0).get("issue_desc")).contains("阿青"));
        assertEquals(1, issueList.get(0).get("shot_no"));
        assertEquals(1, issueList.get(0).get("shot_in_episode"));
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
