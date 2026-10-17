package com.bgssai.shortai.login.service;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        log.info("user login attempt identifier={}", req.getIdentifier());

        SysUserExample example = new SysUserExample();
        example.createCriteria().andUserNameEqualTo(req.getIdentifier());
        SysUser user = sysUserMapper.selectOneByExample(example);

        if (user == null) {
            log.warn("user login failed: user not found identifier={}", req.getIdentifier());
            throw new BizException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordService.matches(user, req.getPassword())) {
            log.warn("user login failed: wrong password identifier={}", req.getIdentifier());
            loginAuditService.record(user.getUserId(), "LOGIN_FAIL_PASSWORD", httpReq);
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

        loginAuditService.record(user.getUserId(), "LOGIN_SUCCESS", httpReq);
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
}
