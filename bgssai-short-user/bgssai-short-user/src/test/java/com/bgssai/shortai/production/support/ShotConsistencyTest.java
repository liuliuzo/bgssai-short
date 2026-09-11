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
}
