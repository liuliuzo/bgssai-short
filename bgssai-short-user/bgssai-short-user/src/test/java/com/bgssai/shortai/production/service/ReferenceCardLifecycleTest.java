package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.bgssai.shortai.mapper.*;
import com.bgssai.shortai.model.*;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.production.dto.ProductionDtos.CharacterReq;

class ReferenceCardLifecycleTest {
    CharacterService service = new CharacterService();
    WorkAccessService access = mock(WorkAccessService.class);
    ShortCharacterMapper characters = mock(ShortCharacterMapper.class);
    ShortSceneCardMapper scenes = mock(ShortSceneCardMapper.class);
    ShortShotMapper shots = mock(ShortShotMapper.class);
    ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
    ShortSceneCard scene = new ShortSceneCard();
    ShortShot shot = new ShortShot();
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "shortCharacterMapper", characters);
        ReflectionTestUtils.setField(service, "shortSceneCardMapper", scenes);
        ReflectionTestUtils.setField(service, "shortShotMapper", shots);
        ReflectionTestUtils.setField(service, "shortAiTaskMapper", tasks);
        ReflectionTestUtils.setField(service, "shortAssetMapper", mock(ShortAssetMapper.class));
        ReflectionTestUtils.setField(service, "assetService", mock(AssetService.class));
        when(access.requireUser()).thenReturn("u-1");
        ShortWork work = new ShortWork(); work.setWorkId("w-1"); work.setUserId("u-1");
        when(access.requireWork("w-1")).thenReturn(work);
        scene.setId(1L); scene.setSceneCardId("sc-1"); scene.setWorkId("w-1"); scene.setUserId("u-1");
        scene.setCardType("SCENE"); scene.setCardName("雨夜便利店"); scene.setAppearancePrompt("old appearance");
        scene.setCardStatus("LOCKED"); scene.setRefImageAssetId("a-old");
        when(scenes.selectOneByExample(any())).thenReturn(scene);
        when(scenes.selectByExample(any())).thenReturn(List.of(scene));
        shot.setId(2L); shot.setShotId("sh-1"); shot.setSceneCardId("sc-1");
        shot.setShotDesc("A customer enters the store"); shot.setShotStatus("DRAFT"); shot.setBlockedFlag(0);
        when(shots.selectByExample(any())).thenReturn(List.of(shot));
    }
    CharacterReq edit(String value) {
        CharacterReq req = new CharacterReq(); req.setSceneCardId("sc-1"); req.setAppearancePrompt(value); return req;
    }
    @Test void sceneEditUnlocksAndMarksStaleWithoutClearingBindingsOrChangingOwner() {
        CharacterReq req = edit("rainy store, empty checkout counter");
        req.setWorkId("foreign"); req.setCardType("PROP");
        service.updateScene(req);
        ArgumentCaptor<ShortSceneCard> card = ArgumentCaptor.forClass(ShortSceneCard.class);
        verify(scenes).updateByExampleSelective(card.capture(), any());
        assertEquals("DRAFT", card.getValue().getCardStatus());
        assertNull(card.getValue().getWorkId()); assertNull(card.getValue().getUserId()); assertNull(card.getValue().getCardType());
        ArgumentCaptor<ShortShot> patch = ArgumentCaptor.forClass(ShortShot.class);
        verify(shots).updateByExampleSelective(patch.capture(), any());
        assertEquals(1, patch.getValue().getStaleFlag()); assertEquals(1, patch.getValue().getBlockedFlag());
        assertNull(patch.getValue().getCharacterRefJson()); assertNull(patch.getValue().getSceneCardId());
        assertEquals("sc-1", shot.getSceneCardId()); assertEquals("a-old", scene.getRefImageAssetId());
    }
    @Test void sameDescriptionAndLockedShotsRemainUntouched() {
        service.updateScene(edit("old appearance"));
        verify(scenes, never()).updateByExampleSelective(any(), any());
        verify(shots, never()).updateByExampleSelective(any(), any());
        shot.setShotStatus("LOCKED");
        service.updateScene(edit("new description"));
        verify(shots, never()).updateByExampleSelective(any(), any());
    }
    @Test void anotherUsersSceneCannotBeEdited() {
        scene.setUserId("another");
        assertThrows(ForbiddenException.class, () -> service.updateScene(edit("new")));
        verify(scenes, never()).updateByExampleSelective(any(), any());
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> panel() { return (Map<String,Object>) service.consistencyPanel("w-1").getResult(); }
    @SuppressWarnings("unchecked")
    String sceneStatus() { return (String)((List<Map<String,Object>>) panel().get("scene_list")).get(0).get("card_status"); }
    @Test void panelUsesRealReferencesDespiteZeroCacheAndSeparatesNoPromptFromUnlocked() {
        scene.setCardStatus("DRAFT");
        assertEquals(1, panel().get("unlocked_ref_count"));
        assertEquals(0, panel().get("unbound_shot_count"));
        scene.setCardStatus("LOCKED"); scene.setRefImageAssetId("");
        assertEquals(1, panel().get("unlocked_ref_count"));
        scene.setRefImageAssetId("a-1"); shot.setBlockedFlag(1); shot.setShotDesc("");
        assertEquals(0, panel().get("unlocked_ref_count"));
        shot.setSceneCardId("missing"); assertEquals(1, panel().get("unlocked_ref_count"));
    }
    @Test void activeTasksAndHistoricalGeneratingArePresentedTruthfullyWithoutOverridingLock() {
        scene.setCardStatus("GENERATING"); assertEquals("DRAFT", sceneStatus());
        when(tasks.countByExample(any())).thenReturn(1L); assertEquals("GENERATING", sceneStatus());
        scene.setCardStatus("LOCKED"); assertEquals("LOCKED", sceneStatus());
    }
    @Test void sceneLockRecomputesOtherUnlockedReferencesInsteadOfClearingBlocked() {
        scene.setCardStatus("DRAFT"); shot.setCharacterRefJson("[\"missing-character\"]");
        CharacterReq req = edit(null); req.setAssetId("a-new"); service.lockScene(req);
        ArgumentCaptor<ShortShot> patch = ArgumentCaptor.forClass(ShortShot.class);
        verify(shots).updateByExampleSelective(patch.capture(), any());
        assertEquals(1, patch.getValue().getBlockedFlag()); assertEquals("LOCKED", scene.getCardStatus());
    }
}
