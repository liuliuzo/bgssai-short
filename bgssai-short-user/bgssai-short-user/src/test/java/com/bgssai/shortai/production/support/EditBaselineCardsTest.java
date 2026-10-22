package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortShot;

class EditBaselineCardsTest {

    @Test
    void lockedCardsShowChineseVersionLabelsAndVoidedIsNotConsistent() {
        ShortCharacter locked = character("c-1", "沈知微", "LOCKED", "a-50012");
        locked.setUpdatedTime(new Date(1_761_000_000_000L));
        ShortCharacter voided = character("c-2", "陆时衍", "DRAFT", "a-old");

        ShortShot shot = new ShortShot();
        shot.setShotNo(3);
        shot.setShotStatus("LOCKED");
        shot.setShotDurationSec(4);
        shot.setShotVersion(2);
        shot.setDialogueText("你敢签");
        shot.setCharacterRefJson("[{\"character_id\":\"c-1\"},{\"character_id\":\"c-2\"}]");

        List<Map<String, Object>> lockedCards = EditBaselineCards.lockedCharacterCards(List.of(locked, voided));
        assertEquals(1, lockedCards.size());
        assertEquals("LOCKED", lockedCards.get(0).get("card_status"));
        assertEquals("角色版本", lockedCards.get(0).get("version_label"));
        assertEquals(1, lockedCards.get(0).get("consistent_flag"));
        assertTrue(String.valueOf(lockedCards.get(0).get("version_text")).contains("沈知微"));
        assertTrue(String.valueOf(lockedCards.get(0).get("version_text")).contains("定妆 a-50012"));

        List<Map<String, Object>> voidedCards = EditBaselineCards.voidedCharacterCards(List.of(locked, voided),
                List.of(shot));
        assertEquals(1, voidedCards.size());
        assertEquals(0, voidedCards.get(0).get("consistent_flag"));
        assertEquals(1, voidedCards.get(0).get("voided_flag"));
        assertEquals("", voidedCards.get(0).get("version_label"));
        assertTrue(String.valueOf(voidedCards.get(0).get("version_text")).contains("不能标为与现行时间轴一致"));

        Map<String, Object> timeline = EditBaselineCards.timelineCard(List.of(shot), List.of(locked, voided));
        assertEquals("LOCKED", timeline.get("card_status"));
        assertEquals("时间轴版本", timeline.get("version_label"));
        assertEquals(1, timeline.get("consistent_flag"));
        assertTrue(String.valueOf(timeline.get("version_text")).contains("v2"));
        assertTrue(String.valueOf(timeline.get("version_text")).contains("总时长 4 秒"));
        assertTrue(String.valueOf(timeline.get("version_text")).contains("字幕 1 条"));
    }

    @Test
    void filmRecordMatchesTimelineAndRefusesVoidedCharacter() {
        ShortCharacter locked = character("c-1", "沈知微", "LOCKED", "a-50012");
        ShortCharacter voided = character("c-2", "陆时衍", "DRAFT", "a-old");
        ShortShot first = shot(1, 4, "LOCKED", "你敢签", "[{\"character_id\":\"c-1\"}]");
        ShortShot second = shot(2, 5, "LOCKED", "三年前那纸", "[{\"character_id\":\"c-1\"}]");

        Map<String, Object> portrait = EditBaselineCards.filmRecord(List.of(first, second), List.of(locked), "9:16");
        Map<String, Object> wide = EditBaselineCards.filmRecord(List.of(first, second), List.of(locked), "16:9");
        assertEquals(1, portrait.get("consistent_flag"));
        assertEquals("角色版本", ((List<Map<String, Object>>) portrait.get("character_version_list")).get(0).get("version_label"));
        Map<String, Object> timeline = (Map<String, Object>) portrait.get("timeline_version");
        assertEquals("时间轴版本", timeline.get("version_label"));
        assertEquals(timeline.get("shot_list"), portrait.get("spot_check"));
        assertEquals(wide.get("spot_check"), portrait.get("spot_check"));
        List<Map<String, Object>> checks = (List<Map<String, Object>>) portrait.get("spot_check");
        assertEquals(1, checks.get(0).get("shot_no"));
        assertEquals(4, checks.get(0).get("shot_duration_sec"));
        assertEquals("你敢签", checks.get(0).get("dialogue_text"));
        assertEquals("沈知微", checks.get(0).get("character_names"));
        assertEquals(2, checks.get(1).get("shot_no"));

        ShortShot mixed = shot(3, 4, "LOCKED", "你敢签",
                "[{\"character_id\":\"c-1\"},{\"character_id\":\"c-2\"}]");
        Map<String, Object> rejected = EditBaselineCards.filmRecord(List.of(mixed), List.of(locked, voided), "9:16");
        assertEquals(0, rejected.get("consistent_flag"));
        assertEquals("", ((List<Map<String, Object>>) rejected.get("voided_character_list")).get(0).get("version_label"));
        assertTrue(String.valueOf(rejected.get("baseline_note")).contains("不能标为与现行时间轴一致"));
    }

    @Test
    void unlockedTimelineDoesNotClaimVersionLabel() {
        ShortShot shot = new ShortShot();
        shot.setShotNo(1);
        shot.setShotStatus("VIDEO_READY");
        shot.setShotDurationSec(3);
        Map<String, Object> timeline = EditBaselineCards.timelineCard(List.of(shot), List.of());
        assertEquals("DRAFT", timeline.get("card_status"));
        assertEquals("", timeline.get("version_label"));
        assertEquals(0, timeline.get("consistent_flag"));
    }

    private static ShortShot shot(int no, int sec, String status, String dialogue, String characterRef) {
        ShortShot shot = new ShortShot();
        shot.setShotNo(no);
        shot.setShotStatus(status);
        shot.setShotDurationSec(sec);
        shot.setShotVersion(2);
        shot.setDialogueText(dialogue);
        shot.setCharacterRefJson(characterRef);
        return shot;
    }

    private static ShortCharacter character(String id, String name, String status, String assetId) {
        ShortCharacter row = new ShortCharacter();
        row.setCharacterId(id);
        row.setCharacterName(name);
        row.setCardStatus(status);
        row.setRefImageAssetId(assetId);
        row.setDeletedFlag(0);
        return row;
    }
}
