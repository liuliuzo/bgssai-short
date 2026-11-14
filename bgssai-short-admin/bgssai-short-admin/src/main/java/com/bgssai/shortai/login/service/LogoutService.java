package com.bgssai.shortai.login.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.common.session.SingleSessionGuard;

/** Ends the current server session and stamps logout time on that login row when the id is known. */
@Service
public class LogoutService {

    @Autowired
    private SingleSessionGuard singleSessionGuard;

    @Autowired
    private LoginAuditService loginAuditService;

    public void logout() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("no user in context");
        }
        String sessionId = UserContext.getSessionId();
        singleSessionGuard.clearSession(userId);
        if (loginAuditService != null) {
            loginAuditService.markLogout(sessionId);
        }
    }
}
