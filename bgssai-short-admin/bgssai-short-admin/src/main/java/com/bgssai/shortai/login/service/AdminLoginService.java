package com.bgssai.shortai.login.service;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.session.SingleSessionGuard;
import com.bgssai.shortai.login.dto.req.ReqAdminLogin;
import com.bgssai.shortai.login.dto.resp.RespAdminLogin;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;
import com.bgssai.shortai.utils.JwtUtil;

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
    private LoginAuditService loginAuditService;

    @Autowired
    private SingleSessionGuard singleSessionGuard;

    /**
     * Authenticate and return a token.
     *
     * @param req     login request body
     * @param httpReq servlet request (for IP and user-agent)
     * @return RespAdminLogin with the JWT token
     * @throws BizException INVALID_CREDENTIALS (4001) if credentials wrong, FORBIDDEN (4003) if role not admin
     */
    public RespAdminLogin login(ReqAdminLogin req, HttpServletRequest httpReq) {
        if (httpReq != null && RequestContextHolder.getRequestAttributes() == null) {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpReq));
        }
        log.info("admin login attempt identifier={}", req.getIdentifier());

        // 逻辑删除的账号不是账号：不带 del_flag=0 过滤的话，被删账号仍能通过密码校验拿到 token，
        // 随后每个业务请求都被会话闸门拦掉，用户看到的是「登录成功却处处失效」的误导文案。
        // 标识支持用户名或邮箱（管理端占位符写「用户名 / 邮箱」）。
        SysUser user = findAdminByIdentifier(req.getIdentifier());

        if (user == null) {
            log.warn("admin login failed: user not found identifier={}", req.getIdentifier());
            loginAuditService.recordFailure(null, "LOGIN_FAIL_PASSWORD", "password", req.getIdentifier(), "auto", "账号或密码不正确");
            throw new BizException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordService.matches(user, req.getPassword())) {
            log.warn("admin login failed: wrong password identifier={}", req.getIdentifier());
            loginAuditService.recordFailure(user.getUserId(), "LOGIN_FAIL_PASSWORD", "password", req.getIdentifier(), "auto", "账号或密码不正确");
            throw new BizException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        if (!AdminAuthGuard.ADMIN_ROLE.equals(user.getOrgCode())) {
            log.warn("admin login denied: not admin identifier={} orgCode={}",
                    req.getIdentifier(), user.getOrgCode());
            loginAuditService.recordFailure(user.getUserId(), "LOGIN_FAIL_ROLE", "password", req.getIdentifier(), "auto", "无管理端权限");
            throw new BizException(CommonErrorCode.FORBIDDEN, "access denied: only admin may use this console");
        }

        // 单会话：先生成并覆盖写入新的会话标识，再把它签进 token。顺序不可颠倒——先签后写会在
        // 写库失败时留下一个库里对不上的 token。覆盖写入即刻使该账号此前签发的所有 token 失效。
        String sessionId = singleSessionGuard.newSessionId();
        singleSessionGuard.bindSession(user.getUserId(), sessionId);

        String token = JwtUtil.generateToken(
                user.getUserId(),
                user.getRealName(),
                "01",
                null,
                null,
                AdminAuthGuard.ADMIN_ROLE,
                sessionId);

        loginAuditService.recordSuccess(user.getUserId(), "password", req.getIdentifier(), "auto", sessionId);
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


    private SysUser findAdminByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String id = identifier.trim();
        SysUserExample byName = new SysUserExample();
        byName.createCriteria().andUserNameEqualTo(id).andDelFlagEqualTo(false);
        SysUser user = sysUserMapper.selectOneByExample(byName);
        if (user != null) {
            return user;
        }
        if (!id.contains("@")) {
            return null;
        }
        SysUserExample byEmail = new SysUserExample();
        byEmail.createCriteria().andEmailEqualTo(id).andDelFlagEqualTo(false);
        return sysUserMapper.selectOneByExample(byEmail);
    }
}
