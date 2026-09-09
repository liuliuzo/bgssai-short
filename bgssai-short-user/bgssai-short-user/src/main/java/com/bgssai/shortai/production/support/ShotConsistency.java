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
            String name = sc.getCardName().trim();
            if (hay.contains(name) || name.contains(hay)) {
                return sc.getSceneCardId();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> characterIds(String characterRefJson) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> item : ShortJson.list(characterRefJson)) {
            String id = ShortJson.text(item.get("character_id"));
            if (!id.isBlank()) {
                ids.add(id);
            }
        }
        return ids;
    }

    public static String appearanceFragment(List<ShortCharacter> lockedChars, ShortSceneCard lockedScene) {
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
