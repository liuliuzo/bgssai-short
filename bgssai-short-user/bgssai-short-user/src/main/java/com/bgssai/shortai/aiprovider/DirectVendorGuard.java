package com.bgssai.shortai.aiprovider;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/**
 * Short never calls vendor hosts directly. Billed generation must go through the CN hub
 * via {@link AiProviderRegistry#resolve}.
 */
public final class DirectVendorGuard {

    private DirectVendorGuard() {
    }

    public static BizException rejected(String providerCode) {
        return new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                TokenHubClientSupport.userFacingMessage(
                        "direct vendor disabled for " + providerCode + "; use CN hub"));
    }
}
