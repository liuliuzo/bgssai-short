package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortShot;

class ShotConsistencyTest {

    @Test
    void bindsCharacterByName() {
        ShortCharacter c = new ShortCharacter();
        c.setCharacterId("c-1");
        c.setCharacterName("林晚");
        String json = ShotConsistency.bindCharacterRefs(List.of(c), "林晚推开门");
        assertTrue(json.contains("c-1"));
        assertEquals(List.of("c-1"), ShotConsistency.characterIds(json));
    }

    @Test
    void appearanceFragmentAndPromptMerge() {
        ShortCharacter c = new ShortCharacter();
        c.setCharacterName("林晚");
        c.setAppearancePrompt("黑长直，米色风衣");
        ShortSceneCard sc = new ShortSceneCard();
        sc.setCardName("天台");
        sc.setAppearancePrompt("夜色霓虹");
        String frag = ShotConsistency.appearanceFragment(List.of(c), sc);
        assertTrue(frag.contains("林晚"));
        assertTrue(frag.contains("天台"));
        String prompt = ShotConsistency.withConsistency("中景对话", frag);
        assertTrue(prompt.contains("一致性要求"));
        String lead = ShotConsistency.leadWithAnchors("中景对话", frag);
        assertTrue(lead.startsWith("角色「林晚」"));
        assertTrue(lead.indexOf("林晚") < lead.indexOf("中景对话"));
    }

    @Test
    void outfitJoinsTheLockedLook() {
        ShortCharacter c = new ShortCharacter();
        c.setCharacterName("沈知微");
        c.setAppearancePrompt("二十五岁都市女性，短发，冷白皮");
        c.setDefaultOutfitCode("OUTFIT_DEFAULT");
        c.setOutfitJson("[{\"outfit_code\":\"OUTFIT_DEFAULT\",\"outfit_name\":\"默认正装\",\"prompt\":\"利落西装\"}]");
        String frag = ShotConsistency.appearanceFragment(List.of(c), null, List.of());
        assertTrue(frag.contains("沈知微"));
        assertTrue(frag.contains("利落西装"));
        assertTrue(frag.contains("服装"));
    }

    @Test
    void readsStringIdArrays() {
        assertEquals(List.of("c-1", "c-2"), ShotConsistency.characterIds("[\"c-1\",\"c-2\"]"));
        assertEquals(List.of("sc-p1"), ShotConsistency.propIds("[\"sc-p1\"]"));
        assertTrue(ShotConsistency.containsRef("[\"sc-p1\"]", "sc-p1"));
    }

    @Test
    void bindsPropsByName() {
        ShortSceneCard prop = new ShortSceneCard();
        prop.setSceneCardId("sc-p1");
        prop.setCardName("退婚书");
        prop.setCardType("PROP");
        ShortSceneCard scene = new ShortSceneCard();
        scene.setSceneCardId("sc-s1");
        scene.setCardName("宴会厅");
        scene.setCardType("SCENE");
        String json = ShotConsistency.bindPropRefs(List.of(prop, scene), "她把退婚书推过去");
        assertEquals(List.of("sc-p1"), ShotConsistency.propIds(json));
        assertEquals("sc-s1", ShotConsistency.bindSceneCardId(List.of(prop, scene), "宴会厅夜景"));
    }

    @Test
    void appearanceFragmentIncludesProps() {
        ShortSceneCard prop = new ShortSceneCard();
        prop.setCardName("退婚书");
        prop.setAppearancePrompt("红印未干");
        String frag = ShotConsistency.appearanceFragment(List.of(), null, List.of(prop));
        assertTrue(frag.contains("退婚书"));
        assertTrue(frag.contains("红印未干"));
    }

    @Test
    void shotNeedsPromptWhenEmpty() {
        ShortShot empty = new ShortShot();
        assertTrue(ShotConsistency.shotNeedsPrompt(empty));
        empty.setShotDesc("有描述");
        assertFalse(ShotConsistency.shotNeedsPrompt(empty));
    }

    @Test
    void checkAnchorReadinessReportsUnlockedCharactersAndReadyState() {
        ShortShot shot = new ShortShot();
        shot.setShotDesc("林晚进入宴会厅");
        shot.setCharacterRefJson("[\"c-1\"]");
        shot.setSceneCardId("sc-1");

        ShortCharacter unlockedChar = new ShortCharacter();
        unlockedChar.setCharacterId("c-1");
        unlockedChar.setCardStatus("DRAFT");

        ShortSceneCard lockedScene = new ShortSceneCard();
        lockedScene.setSceneCardId("sc-1");
        lockedScene.setCardStatus("LOCKED");

        var notReady = ShotConsistency.checkAnchorReadiness(shot, List.of(unlockedChar), List.of(lockedScene));
        assertEquals(Boolean.FALSE, notReady.get("ready"));
        assertEquals(List.of("c-1"), notReady.get("unlocked_character_ids"));

        unlockedChar.setCardStatus("LOCKED");
        var ready = ShotConsistency.checkAnchorReadiness(shot, List.of(unlockedChar), List.of(lockedScene));
        assertEquals(Boolean.TRUE, ready.get("ready"));
        assertEquals(Boolean.TRUE, ready.get("scene_locked"));
    }
}
