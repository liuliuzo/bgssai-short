package com.bgssai.shortai.login.service;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bgssai.shortai.account.service.AccountGuard;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.login.dto.req.ReqUserLogin;
import com.bgssai.shortai.login.dto.resp.RespUserLogin;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;
import com.bgssai.shortai.utils.JwtUtil;
import com.bgssai.shortai.common.session.SingleSessionGuard;

/**
 * User password login.
 * Verifies credentials and issues a JWT with role=USER, recording a login log for audit.
 * Unlike the admin console there is no role gate: any active account may log in here.
 * This is the reference implementation of the flat-service pattern: the whole business path
 * reads top-to-bottom in one method (Standards §1 Flat Code).
 */
@Service
public class UserLoginService {
    @org.springframework.beans.factory.annotation.Autowired
    private SingleSessionGuard singleSessionGuard;


    private static final Logger log = LoggerFactory.getLogger(UserLoginService.class);

    private static final String USER_ROLE = "USER";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private LoginAuditService loginAuditService;

    /**
     * Authenticate and return a token.
     *
     * @param req     login request body
     * @param httpReq servlet request (for IP and user-agent)
     * @return RespUserLogin with the JWT token
     * @throws BizException INVALID_CREDENTIALS (4001) if credentials are wrong
     */
    public RespUserLogin login(ReqUserLogin req, HttpServletRequest httpReq) {
        if (httpReq != null && RequestContextHolder.getRequestAttributes() == null) {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpReq));
        }
        log.info("user login attempt identifier={}", req.getIdentifier());

        // 标识支持用户名或邮箱（登录页占位符写「手机号 / 邮箱 / 账号」）。先用户名，含 @ 再邮箱。
        SysUser user = findUserByIdentifier(req.getIdentifier());

        if (user == null) {
            log.warn("user login failed: user not found identifier={}", req.getIdentifier());
            loginAuditService.recordFailure(null, "LOGIN_FAIL_PASSWORD", "password", req.getIdentifier(), "auto", "账号或密码不正确");
            throw new BizException(CommonErrorCode.INVALID_CREDENTIALS);
        }
        try {
            AccountGuard.rejectIfDeleted(user);
        } catch (BizException deleted) {
            loginAuditService.recordFailure(user.getUserId(), "LOGIN_FAIL_PASSWORD", "password", req.getIdentifier(), "auto", "账号不可用");
            throw deleted;
        }

        if (!passwordService.matches(user, req.getPassword())) {
            log.warn("user login failed: wrong password identifier={}", req.getIdentifier());
            loginAuditService.recordFailure(user.getUserId(), "LOGIN_FAIL_PASSWORD", "password", req.getIdentifier(), "auto", "账号或密码不正确");
            throw new BizException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        String sessionId = singleSessionGuard.newSessionId();
        singleSessionGuard.bindSession(user.getUserId(), sessionId);
        String token = JwtUtil.generateToken(
                user.getUserId(),
                user.getRealName(),
                "01",
                null,
                null,
                USER_ROLE, sessionId);

        loginAuditService.recordSuccess(user.getUserId(), "password", req.getIdentifier(), "auto", sessionId);
        log.info("user login success userId={}", user.getUserId());

        RespUserLogin resp = new RespUserLogin();
        resp.setJwtToken(token);
        resp.setRoleCode(USER_ROLE);
        resp.setRoleName("用户");
        resp.setUserId(user.getUserId());
        resp.setUserName(user.getUserName());
        resp.setRealName(user.getRealName());
        return resp;
    }

    private SysUser findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String id = identifier.trim();
        SysUserExample byName = new SysUserExample();
        byName.createCriteria().andUserNameEqualTo(id);
        SysUser user = sysUserMapper.selectOneByExample(byName);
        if (user != null) {
            return user;
        }
        if (!id.contains("@")) {
            return null;
        }
        SysUserExample byEmail = new SysUserExample();
        byEmail.createCriteria().andEmailEqualTo(id);
        return sysUserMapper.selectOneByExample(byEmail);
    }
}
