package com.bgssai.shortai.aiprovider;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/**
 * Official video is still pending upstream. The caller must keep the same receipt
 * and poll again, instead of failing the task or starting another billed generation.
 */
public class OfficialVideoStillOpenException extends BizException {

    private static final long serialVersionUID = 1L;

    private final String requestId;

    public OfficialVideoStillOpenException(String requestId) {
        super(CommonErrorCode.UPSTREAM_TIMEOUT, "model task timeout");
        this.requestId = requestId;
    }

    public String requestId() {
        return requestId;
    }
}
