package com.bgssai.shortai.production.support;

import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;

/**
 * 一张角色或场景卡上只能有一张当前图。锁定或上传真实照片时，清掉旧的生成候选选中标记。
 */
public final class CardAssetSelection {

    private CardAssetSelection() {
    }

    public static void selectOnly(ShortAssetMapper mapper, String userId, String cardId, String assetId) {
        if (mapper == null || userId == null || cardId == null || assetId == null || assetId.isBlank()) {
            return;
        }
        clear(mapper, userId, cardId);
        ShortAsset pick = new ShortAsset();
        pick.setSelectedFlag(1);
        ShortAssetExample one = new ShortAssetExample();
        one.createCriteria().andAssetIdEqualTo(assetId).andUserIdEqualTo(userId).andShotIdEqualTo(cardId)
                .andDeletedFlagEqualTo(0);
        mapper.updateByExampleSelective(pick, one);
    }

    public static void clear(ShortAssetMapper mapper, String userId, String cardId) {
        if (mapper == null || userId == null || cardId == null) {
            return;
        }
        ShortAsset clear = new ShortAsset();
        clear.setSelectedFlag(0);
        ShortAssetExample all = new ShortAssetExample();
        all.createCriteria().andUserIdEqualTo(userId).andShotIdEqualTo(cardId).andDeletedFlagEqualTo(0);
        mapper.updateByExampleSelective(clear, all);
    }

    /** 已锁定的参考图优先于旧候选上残留的 selected_flag。 */
    public static int shownFlag(String assetId, String refAssetId, Integer stored) {
        if (refAssetId != null && !refAssetId.isBlank()) {
            return refAssetId.equals(assetId) ? 1 : 0;
        }
        return stored != null && stored == 1 ? 1 : 0;
    }
}
