package com.bgssai.shortai.login.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bgssai.shortai.mapper.LoginLogMapper;
import com.bgssai.shortai.model.LoginLog;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Shared login-audit writer for password / OTP / OAuth login paths.
 * Failures are swallowed so audit never blocks issuing a session.
 */
@Service
public class LoginAuditService {

    private static final Logger log = LoggerFactory.getLogger(LoginAuditService.class);

    @Autowired
    private LoginLogMapper loginLogMapper;

    public void record(String userId, String action) {
        record(userId, action, currentRequest());
    }

    public void record(String userId, String action, HttpServletRequest httpReq) {
        if (userId == null || userId.isBlank() || action == null || action.isBlank()) {
            return;
        }
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setAction(action);
            loginLog.setIpAddress(resolveIp(httpReq));
            loginLog.setUserAgent(httpReq != null ? httpReq.getHeader("User-Agent") : null);
            loginLog.setDelFlag(Boolean.FALSE);
            loginLog.setCreateTime(new Date());
            loginLogMapper.insertSelective(loginLog);
        } catch (Exception e) {
            log.warn("login log insert failed userId={} action={} err={}", userId, action, e.getMessage());
        }
    }

    private static HttpServletRequest currentRequest() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                return sra.getRequest();
            }
        } catch (IllegalStateException ignored) {
            // non-request thread
        }
        return null;
    }

    private static String resolveIp(HttpServletRequest req) {
        if (req == null) {
            return null;
        }
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return req.getRemoteAddr();
    }
}
