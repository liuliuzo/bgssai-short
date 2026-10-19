package com.bgssai.shortai.production.service;

import java.util.Locale;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/**
 * Confirmed shot versions and referenced consistency assets.
 * A failed retry must not replace a locked asset, and a missing reference fails before the model call.
 */
public final class ShotVersionGuard {

    private ShotVersionGuard() {
    }

    public static boolean keepConfirmedAsset(String shotStatus, String confirmedAssetId) {
        return "LOCKED".equals(shotStatus) && confirmedAssetId != null && !confirmedAssetId.isBlank();
    }

    public static void requireReferencedAsset(String label, String id, boolean present, boolean locked,
            boolean hasImage) {
        if (id == null || id.isBlank()) {
            return;
        }
        if (!present) {
            throw new BizException(CommonErrorCode.RULE_REJECT, label + "素材缺失：" + id);
        }
        if (!locked || !hasImage) {
            throw new BizException(CommonErrorCode.RULE_REJECT, label + "未确认或缺少参考图：" + id);
        }
    }

    public static void rejectConsistencyLimitation(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (text.contains("无法保持一致") || lower.contains("consistency limitation")) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "模型无法保持一致性，未记为镜头成功");
        }
    }
}
