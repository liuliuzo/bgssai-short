package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortShot;

/**
 * Binds character/scene cards onto shots and builds consistency prompt fragments
 * (Jellyfish-style: locked portrait is first-class input, not optional flavor text).
 */
public final class ShotConsistency {

    private ShotConsistency() {
    }

    public static String bindCharacterRefs(List<ShortCharacter> characters, String haystack) {
        if (characters == null || characters.isEmpty() || haystack == null || haystack.isBlank()) {
            return null;
        }
        List<Map<String, Object>> refs = new ArrayList<>();
        for (ShortCharacter c : characters) {
            if (c == null || c.getCharacterName() == null || c.getCharacterName().isBlank()) {
                continue;
            }
            if (haystack.contains(c.getCharacterName().trim())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("character_id", c.getCharacterId());
                item.put("character_name", c.getCharacterName());
                refs.add(item);
            }
        }
        return refs.isEmpty() ? null : ShortJson.toJson(refs);
    }

    public static String bindSceneCardId(List<ShortSceneCard> scenes, String locationOrSummary) {
        if (scenes == null || scenes.isEmpty() || locationOrSummary == null || locationOrSummary.isBlank()) {
            return null;
        }
        String hay = locationOrSummary.trim();
        for (ShortSceneCard sc : scenes) {
            if (sc == null || sc.getCardName() == null || sc.getCardName().isBlank()) {
                continue;
            }
            if (!isType(sc, "SCENE")) {
                continue;
            }
            String name = sc.getCardName().trim();
            if (hay.contains(name) || name.contains(hay)) {
                return sc.getSceneCardId();
            }
        }
        return null;
    }

    public static String bindPropRefs(List<ShortSceneCard> cards, String haystack) {
        if (cards == null || cards.isEmpty() || haystack == null || haystack.isBlank()) {
            return null;
        }
        List<String> ids = new ArrayList<>();
        for (ShortSceneCard sc : cards) {
            if (sc == null || sc.getCardName() == null || sc.getCardName().isBlank() || !isType(sc, "PROP")) {
                continue;
            }
            if (haystack.contains(sc.getCardName().trim())) {
                ids.add(sc.getSceneCardId());
            }
        }
        return ids.isEmpty() ? null : ShortJson.toJson(ids);
    }

    public static String refsFromIds(List<String> ids) {
        if (ids == null) {
            return null;
        }
        List<String> clean = new ArrayList<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                clean.add(id.trim());
            }
        }
        return ShortJson.toJson(clean);
    }

    public static List<String> characterIds(String characterRefJson) {
        return ShortJson.idList(characterRefJson, "character_id");
    }

    public static List<String> propIds(String propRefJson) {
        return ShortJson.idList(propRefJson, "scene_card_id", "prop_id");
    }

    public static boolean containsRef(String json, String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        for (String item : ShortJson.idList(json, "character_id", "scene_card_id", "prop_id")) {
            if (id.equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isType(ShortSceneCard card, String expected) {
        if (card == null) {
            return false;
        }
        String type = card.getCardType() == null || card.getCardType().isBlank() ? "SCENE" : card.getCardType();
        return expected.equals(type);
    }

    public static String appearanceFragment(List<ShortCharacter> lockedChars, ShortSceneCard lockedScene) {
        return appearanceFragment(lockedChars, lockedScene, null);
    }

    public static String appearanceFragment(List<ShortCharacter> lockedChars, ShortSceneCard lockedScene,
            List<ShortSceneCard> lockedProps) {
        StringBuilder sb = new StringBuilder();
        if (lockedChars != null) {
            for (ShortCharacter c : lockedChars) {
                if (c == null) {
                    continue;
                }
                String name = c.getCharacterName() == null ? "角色" : c.getCharacterName().trim();
                String appearance = c.getAppearancePrompt() == null ? "" : c.getAppearancePrompt().trim();
                if (sb.length() > 0) {
                    sb.append("；");
                }
                sb.append("角色「").append(name).append("」保持定妆一致");
                if (!appearance.isBlank()) {
                    sb.append("：").append(appearance);
                }
            }
        }
        if (lockedScene != null) {
            String name = lockedScene.getCardName() == null ? "场景" : lockedScene.getCardName().trim();
            String appearance = lockedScene.getAppearancePrompt() == null ? ""
                    : lockedScene.getAppearancePrompt().trim();
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append("场景「").append(name).append("」保持场景卡一致");
            if (!appearance.isBlank()) {
                sb.append("：").append(appearance);
            }
        }
        if (lockedProps != null) {
            for (ShortSceneCard prop : lockedProps) {
                if (prop == null) {
                    continue;
                }
                String name = prop.getCardName() == null ? "道具" : prop.getCardName().trim();
                String appearance = prop.getAppearancePrompt() == null ? "" : prop.getAppearancePrompt().trim();
                if (sb.length() > 0) {
                    sb.append("；");
                }
                sb.append("道具「").append(name).append("」保持道具卡一致");
                if (!appearance.isBlank()) {
                    sb.append("：").append(appearance);
                }
            }
        }
        return sb.toString();
    }

    public static String withConsistency(String basePrompt, String fragment) {
        String base = basePrompt == null ? "" : basePrompt.trim();
        String frag = fragment == null ? "" : fragment.trim();
        if (frag.isBlank()) {
            return base;
        }
        if (base.isBlank()) {
            return frag + "，竖屏 9:16 电影感短剧画面";
        }
        return base + "。一致性要求：" + frag;
    }

    public static boolean shotNeedsPrompt(ShortShot shot) {
        if (shot == null) {
            return true;
        }
        String image = shot.getImagePrompt() == null ? "" : shot.getImagePrompt().trim();
        String desc = shot.getShotDesc() == null ? "" : shot.getShotDesc().trim();
        return image.isBlank() && desc.isBlank();
    }
}
