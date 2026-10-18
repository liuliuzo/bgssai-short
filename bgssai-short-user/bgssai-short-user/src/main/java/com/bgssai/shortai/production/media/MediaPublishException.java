package com.bgssai.shortai.production.media;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.ErrorCode;

/**
 * Tube ingest failure that carries HTTP status / attempt count for publish_log.
 */
public class MediaPublishException extends BizException {

    private final Integer httpStatus;
    private final int attempts;

    public MediaPublishException(ErrorCode errorCode, String message, Integer httpStatus, int attempts) {
        super(errorCode, message);
        this.httpStatus = httpStatus;
        this.attempts = Math.max(1, attempts);
    }

    public MediaPublishException(ErrorCode errorCode, String message, Integer httpStatus, int attempts,
            Throwable cause) {
        super(errorCode, message, cause);
        this.httpStatus = httpStatus;
        this.attempts = Math.max(1, attempts);
    }

    public MediaPublishException(String code, String message, Integer httpStatus, int attempts, Throwable cause) {
        super(code, message, cause);
        this.httpStatus = httpStatus;
        this.attempts = Math.max(1, attempts);
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public int getAttempts() {
        return attempts;
    }
}
