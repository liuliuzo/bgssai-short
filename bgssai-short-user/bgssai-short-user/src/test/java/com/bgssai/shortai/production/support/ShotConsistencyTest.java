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
    void shotNeedsPromptWhenEmpty() {
        ShortShot empty = new ShortShot();
        assertTrue(ShotConsistency.shotNeedsPrompt(empty));
        empty.setShotDesc("有描述");
        assertFalse(ShotConsistency.shotNeedsPrompt(empty));
    }
}
