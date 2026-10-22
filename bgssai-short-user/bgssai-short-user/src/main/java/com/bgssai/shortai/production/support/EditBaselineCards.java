package com.bgssai.shortai.production.support;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortShot;

/**
 * 剪辑工程上展示的导出基准。锁定判定只看 card_status / shot_status = LOCKED，
 * 已作废（未锁定）的角色版本不得再标成与现行时间轴一致。
 */
public final class EditBaselineCards {

    public static final String CHARACTER_VERSION_LABEL = "角色版本";

    public static final String TIMELINE_VERSION_LABEL = "时间轴版本";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private EditBaselineCards() {
    }

    public static List<Map<String, Object>> lockedCharacterCards(List<ShortCharacter> characters) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (characters == null) {
            return list;
        }
        for (ShortCharacter row : characters) {
            if (row == null || !"LOCKED".equals(row.getCardStatus())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            String name = blank(row.getCharacterName(), "未命名角色");
            item.put("character_id", row.getCharacterId());
            item.put("character_name", name);
            item.put("card_status", "LOCKED");
            item.put("version_label", CHARACTER_VERSION_LABEL);
            item.put("version_text", characterVersionText(row, name));
            item.put("ref_image_asset_id", row.getRefImageAssetId());
            item.put("consistent_flag", 1);
            item.put("voided_flag", 0);
            list.add(item);
        }
        return list;
    }

    public static List<Map<String, Object>> voidedCharacterCards(List<ShortCharacter> characters,
            List<ShortShot> shots) {
        Set<String> referenced = referencedCharacterIds(shots);
        List<Map<String, Object>> list = new ArrayList<>();
        if (characters == null) {
            return list;
        }
        for (ShortCharacter row : characters) {
            if (row == null || "LOCKED".equals(row.getCardStatus())) {
                continue;
            }
            if (row.getDeletedFlag() != null && row.getDeletedFlag() == 1) {
                continue;
            }
            if (row.getCharacterId() == null || !referenced.contains(row.getCharacterId())) {
                continue;
            }
            String name = blank(row.getCharacterName(), "未命名角色");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("character_id", row.getCharacterId());
            item.put("character_name", name);
            item.put("card_status", row.getCardStatus() == null ? "DRAFT" : row.getCardStatus());
            item.put("version_label", "");
            item.put("version_text", name + " 已作废，不能标为与现行时间轴一致");
            item.put("consistent_flag", 0);
            item.put("voided_flag", 1);
            list.add(item);
        }
        return list;
    }

    /**
     * 成片导出时记下的基准。抽检列表与时间轴卡片使用同一份镜号、时长、字幕和角色。
     * 时间轴仍引用已作废角色时，成片不得再标成与现行时间轴一致。
     */
    public static Map<String, Object> filmRecord(List<ShortShot> shots, List<ShortCharacter> characters,
            String aspectRatio) {
        List<Map<String, Object>> locked = lockedCharacterCards(characters);
        List<Map<String, Object>> voided = voidedCharacterCards(characters, shots);
        Map<String, Object> timeline = timelineCard(shots, characters);
        boolean aligned = "LOCKED".equals(timeline.get("card_status")) && voided.isEmpty();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("character_version_list", locked);
        record.put("voided_character_list", voided);
        record.put("timeline_version", timeline);
        record.put("aspect_ratio", aspectRatio == null || aspectRatio.isBlank() ? "9:16" : aspectRatio.trim());
        record.put("consistent_flag", aligned ? 1 : 0);
        record.put("baseline_note", voided.isEmpty()
                ? (aligned ? "成片与已锁定的角色版本、时间轴版本一致" : "时间轴尚未锁定，不能标成与现行时间轴一致")
                : "已作废角色版本不能标为与现行时间轴一致");
        record.put("spot_check", timeline.get("shot_list"));
        return record;
    }

    public static Map<String, Object> timelineCard(List<ShortShot> shots, List<ShortCharacter> characters) {
        List<Map<String, Object>> shotList = new ArrayList<>();
        int totalSec = 0;
        int lineCount = 0;
        int versionNo = 1;
        boolean shotLocked = false;
        int count = 0;
        if (shots != null) {
            for (ShortShot shot : shots) {
                if (shot == null) {
                    continue;
                }
                count++;
                if ("LOCKED".equals(shot.getShotStatus())) {
                    shotLocked = true;
                }
                int sec = shot.getShotDurationSec() == null ? 0 : shot.getShotDurationSec();
                totalSec += Math.max(sec, 0);
                String dialogue = shot.getDialogueText() == null ? "" : shot.getDialogueText().trim();
                if (!dialogue.isEmpty()) {
                    lineCount++;
                }
                int ver = shot.getShotVersion() == null || shot.getShotVersion() < 1 ? 1 : shot.getShotVersion();
                versionNo = Math.max(versionNo, ver);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("shot_no", shot.getShotNo());
                item.put("shot_duration_sec", sec);
                item.put("dialogue_text", dialogue);
                item.put("shot_status", shot.getShotStatus());
                item.put("character_names", characterNames(shot, characters));
                shotList.add(item);
            }
        }
        boolean characterLocked = hasLockedCharacter(characters);
        boolean locked = count > 0 && (shotLocked || characterLocked);
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("card_status", locked ? "LOCKED" : "DRAFT");
        card.put("version_label", locked ? TIMELINE_VERSION_LABEL : "");
        card.put("version_no", versionNo);
        card.put("shot_count", count);
        card.put("total_duration_sec", totalSec);
        card.put("subtitle_count", lineCount);
        card.put("shot_list", shotList);
        card.put("consistent_flag", locked ? 1 : 0);
        card.put("version_text", locked
                ? "v" + versionNo + " · " + count + " 镜 · 总时长 " + totalSec + " 秒 · 字幕 " + lineCount + " 条"
                : "时间轴尚未锁定");
        return card;
    }

    private static boolean hasLockedCharacter(List<ShortCharacter> characters) {
        if (characters == null) {
            return false;
        }
        for (ShortCharacter row : characters) {
            if (row != null && "LOCKED".equals(row.getCardStatus())) {
                return true;
            }
        }
        return false;
    }

    private static String characterVersionText(ShortCharacter row, String name) {
        String portrait = row.getRefImageAssetId() == null || row.getRefImageAssetId().isBlank()
                ? "已锁定定妆" : "定妆 " + row.getRefImageAssetId().trim();
        if (row.getUpdatedTime() == null) {
            return name + " · " + portrait;
        }
        return name + " · " + portrait + " · " + STAMP.format(row.getUpdatedTime().toInstant());
    }

    private static Set<String> referencedCharacterIds(List<ShortShot> shots) {
        Set<String> ids = new LinkedHashSet<>();
        if (shots == null) {
            return ids;
        }
        for (ShortShot shot : shots) {
            if (shot == null) {
                continue;
            }
            ids.addAll(ShotConsistency.characterIds(shot.getCharacterRefJson()));
        }
        return ids;
    }

    private static String characterNames(ShortShot shot, List<ShortCharacter> characters) {
        List<String> ids = ShotConsistency.characterIds(shot.getCharacterRefJson());
        if (ids.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            String name = id;
            if (characters != null) {
                for (ShortCharacter row : characters) {
                    if (row != null && id.equals(row.getCharacterId())
                            && row.getCharacterName() != null && !row.getCharacterName().isBlank()) {
                        name = row.getCharacterName().trim();
                        break;
                    }
                }
            }
            names.add(name);
        }
        return String.join("、", names);
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
