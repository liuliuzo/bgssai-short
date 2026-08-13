package com.bgssai.shortdrama.login.service;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortdrama.common.AdminAuthGuard;
import com.bgssai.shortdrama.common.exception.BizException;
import com.bgssai.shortdrama.common.exception.CommonErrorCode;
import com.bgssai.shortdrama.login.dto.req.ReqAdminLogin;
import com.bgssai.shortdrama.login.dto.resp.RespAdminLogin;
import com.bgssai.shortdrama.mapper.LoginLogMapper;
import com.bgssai.shortdrama.mapper.SysUserMapper;
import com.bgssai.shortdrama.model.LoginLog;
import com.bgssai.shortdrama.model.SysUser;
import com.bgssai.shortdrama.model.SysUserExample;
import com.bgssai.shortdrama.utils.JwtUtil;

/**
 * Admin password login.
 * Verifies credentials, checks role=PLATFORM_ADMIN, issues a JWT with the role claim,
 * and records a login log for audit. Only admin-role users may log in here; all other
 * roles are rejected. This is the reference implementation of the flat-service pattern:
 * the whole business path reads top-to-bottom in one method (Standards §1 Flat Code).
 */
@Service
public class AdminLoginService {

    private static final Logger log = LoggerFactory.getLogger(AdminLoginService.class);

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private LoginLogMapper loginLogMapper;

    /**
     * Authenticate and return a token.
     *
     * @param req     login request body
     * @param httpReq servlet request (for IP and user-agent)
     * @return RespAdminLogin with the JWT token
     * @throws BizException INVALID_CREDENTIALS (4001) if credentials wrong, FORBIDDEN (4003) if role not admin
     */
    public RespAdminLogin login(ReqAdminLogin req, HttpServletRequest httpReq) {
        log.info("admin login attempt identifier={}", req.getIdentifier());

        SysUserExample example = new SysUserExample();
        example.createCriteria().andUserNameEqualTo(req.getIdentifier());
        SysUser user = sysUserMapper.selectOneByExample(example);

        if (user == null) {
            log.warn("admin login failed: user not found identifier={}", req.getIdentifier());
            throw new BizException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordService.matches(user, req.getPassword())) {
            log.warn("admin login failed: wrong password identifier={}", req.getIdentifier());
            appendLoginLog(user.getUserId(), "LOGIN_FAIL_PASSWORD", httpReq);
            throw new BizException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        if (!AdminAuthGuard.ADMIN_ROLE.equals(user.getOrgCode())) {
            log.warn("admin login denied: not admin identifier={} orgCode={}",
                    req.getIdentifier(), user.getOrgCode());
            appendLoginLog(user.getUserId(), "LOGIN_FAIL_ROLE", httpReq);
            throw new BizException(CommonErrorCode.FORBIDDEN, "access denied: only admin may use this console");
        }

        String token = JwtUtil.generateToken(
                user.getUserId(),
                user.getRealName(),
                "01",
                null,
                null,
                AdminAuthGuard.ADMIN_ROLE);

        appendLoginLog(user.getUserId(), "LOGIN_SUCCESS", httpReq);
        log.info("admin login success userId={}", user.getUserId());

        RespAdminLogin resp = new RespAdminLogin();
        resp.setJwtToken(token);
        resp.setRoleCode(AdminAuthGuard.ADMIN_ROLE);
        resp.setRoleName("管理员");
        resp.setUserId(user.getUserId());
        resp.setUserName(user.getUserName());
        resp.setRealName(user.getRealName());
        return resp;
    }

    private void appendLoginLog(String userId, String action, HttpServletRequest httpReq) {
        try {
            String ip = resolveIp(httpReq);
            String ua = httpReq != null ? httpReq.getHeader("User-Agent") : null;
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setAction(action);
            loginLog.setIpAddress(ip);
            loginLog.setUserAgent(ua);
            loginLog.setDelFlag(false);
            loginLogMapper.insertSelective(loginLog);
        } catch (Exception e) {
            log.warn("[AdminLoginService] login log insert failed userId={} err={}", userId, e.getMessage());
        }
    }

    private static String resolveIp(HttpServletRequest req) {
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isEmpty()) return real.trim();
        return req.getRemoteAddr();
    }
}
