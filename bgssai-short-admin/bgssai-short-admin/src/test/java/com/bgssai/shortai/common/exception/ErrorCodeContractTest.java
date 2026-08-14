package com.bgssai.shortai.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Offline unit test pinning the response codes to the published contract
 * (docs/api/README.md error-code table). It exists because the codes previously drifted:
 * the enum carried 601 / 603 for unauthorized / forbidden while the contract, the prototypes
 * and the frontend interceptor all spoke 2002 / 4003, so a role rejection came back with a
 * code no client recognised.
 */
class ErrorCodeContractTest {

    @Test
    void unauthorizedMatchesContract() {
        assertEquals("2002", CommonErrorCode.UNAUTHORIZED.code());
    }

    @Test
    void forbiddenMatchesContract() {
        assertEquals("4003", CommonErrorCode.FORBIDDEN.code());
    }

    @Test
    void invalidCredentialsMatchesContract() {
        assertEquals("4001", CommonErrorCode.INVALID_CREDENTIALS.code());
        assertEquals("invalid credentials", CommonErrorCode.INVALID_CREDENTIALS.message());
    }

    @Test
    void invalidParamAndRateLimitMatchContract() {
        assertEquals("50000001", CommonErrorCode.INVALID_PARAM.code());
        assertEquals("429000", CommonErrorCode.RATE_LIMIT_EXCEEDED.code());
        assertEquals("999999", CommonErrorCode.SYSTEM_ERROR.code());
    }

    @Test
    void bizExceptionCarriesTheEnumCode() {
        BizException ex = new BizException(CommonErrorCode.FORBIDDEN, "access denied");
        assertEquals("4003", ex.getCode());
        assertEquals("access denied", ex.getMessage());
    }

    @Test
    void forbiddenExceptionDefaultsToForbiddenCode() {
        assertEquals("4003", new ForbiddenException("only admin can view login logs").getCode());
    }

    @Test
    void unauthorizedExceptionDefaultsToUnauthorizedCode() {
        assertEquals("2002", new UnauthorizedException("no user in context").getCode());
    }
}
