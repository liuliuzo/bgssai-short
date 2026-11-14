package com.bgssai.shortai.login.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bgssai.shortai.loginlog.detail.LoginIdentifierMask;
import com.bgssai.shortai.loginlog.detail.LoginRequestFacts;
import com.bgssai.shortai.loginlog.detail.LoginRiskSignals;
import com.bgssai.shortai.loginlog.detail.LoginUserAgent;
import com.bgssai.shortai.loginlog.location.IpGeo;
import com.bgssai.shortai.loginlog.location.LoginIpLocationResolver;
import com.bgssai.shortai.mapper.LoginLogMapper;
import com.bgssai.shortai.model.LoginLog;
import com.bgssai.shortai.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Writes a login row from facts already on the request. Failures are swallowed so audit
 * never blocks a session. Passwords, OTP codes, and tokens are not accepted.
 */
@Service
public class LoginAuditService {

    static final String CHANNEL = "web";

    private static final Logger log = LoggerFactory.getLogger(LoginAuditService.class);
    private static final Set<String> ACTIONS = Set.of(
            "LOGIN_SUCCESS", "LOGIN_FAIL_PASSWORD", "LOGIN_FAIL_ROLE", "LOGIN_FAIL_OTP", "LOGIN_FAIL_OAUTH");
    private static final Set<String> METHODS = Set.of(
            "password", "email_otp", "phone_otp", "wechat", "alipay", "douyin", "baidu", "bgssai", "oauth");
    private static final Set<String> REASONS = Set.of(
            "登录成功", "账号或密码不正确", "无管理端权限", "验证码错误或已过期", "第三方登录未完成", "账号不可用");

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private LoginIpLocationResolver ipLocationResolver;

    public void record(String userId, String action) {
        record(userId, action, currentRequest());
    }

    public void record(String userId, String action, HttpServletRequest httpReq) {
        if ("LOGIN_SUCCESS".equals(action)) {
            recordSuccess(userId, null, null, "auto", null);
        } else {
            recordFailure(userId, action, null, null, "auto", null);
        }
    }

    public void recordSuccess(String userId, String method, String identifier, String identifierKind, String sessionId) {
        write(userId, "LOGIN_SUCCESS", "SUCCESS", "登录成功", method, identifier, identifierKind, sessionId);
    }

    public void recordFailure(String userId, String action, String method, String identifier, String identifierKind, String reason) {
        write(userId, action, "FAILURE", reason, method, identifier, identifierKind, null);
    }

    public void markLogout(String sessionId) {
        if (sessionId == null || sessionId.isBlank() || loginLogMapper == null) return;
        try {
            loginLogMapper.markLogout(sessionId, new Date());
        } catch (Exception e) {
            log.warn("login logout stamp failed err={}", e.getMessage());
        }
    }

    public void touchLastSeen(String sessionId, Date seenAt) {
        if (sessionId == null || sessionId.isBlank() || seenAt == null || loginLogMapper == null) return;
        try {
            loginLogMapper.touchLastSeen(sessionId, seenAt);
        } catch (Exception e) {
            log.debug("login last-seen stamp skipped");
        }
    }

    private void write(String userId, String action, String result, String reason, String method,
                       String identifier, String identifierKind, String sessionId) {
        if (action == null || !ACTIONS.contains(action)) {
            return;
        }
        try {
            LoginRequestFacts facts = LoginRequestFacts.read(currentRequest(), CHANNEL);
            LoginIdentifierMask.Masked masked = LoginIdentifierMask.mask(identifier, identifierKind);
            Date now = new Date();
            LoginLog row = new LoginLog();
            row.setUserId(blankToNull(userId));
            row.setAction(action);
            row.setLoginResult(result);
            row.setResultReason(REASONS.contains(reason) ? reason : null);
            row.setLoginMethod(METHODS.contains(method) ? method : null);
            row.setIdentifierType(masked.getType());
            row.setIdentifierMasked(masked.getValue());
            row.setClientChannel(facts.getClientChannel());
            row.setBrowserName(facts.getBrowserName());
            row.setOsName(facts.getOsName());
            row.setDeviceType(facts.getDeviceType());
            row.setDeviceLabel(facts.getDeviceLabel());
            row.setIpAddress(facts.getIp());
            row.setUserAgent(facts.getUserAgent());
            applyGeo(row, facts.getIp());
            row.setRequestScheme(facts.getScheme());
            row.setTlsProtocol(facts.getTlsProtocol());
            row.setRefererHost(facts.getRefererHost());
            row.setEntryPath(facts.getEntryPath());
            row.setRequestId(facts.getRequestId());
            row.setAcceptLanguage(facts.getAcceptLanguage());
            row.setClientTimezone(facts.getClientTimezone());
            row.setClientHints(facts.getClientHints());
            if ("SUCCESS".equals(result) && sessionId != null && sessionId.matches("[A-Za-z0-9-]{16,64}")) {
                row.setSessionId(sessionId);
                row.setSessionIssuedAt(now);
                row.setSessionExpiresAt(new Date(now.getTime() + JwtUtil.expirationMillis()));
                row.setSessionLastSeenAt(now);
                applyRisk(row, userId);
            }
            row.setDelFlag(Boolean.FALSE);
            row.setCreateTime(now);
            insert(row);
        } catch (Exception e) {
            log.warn("login log insert failed action={} err={}", action, e.getMessage());
        }
    }

    private void applyGeo(LoginLog row, String ip) {
        if (ipLocationResolver == null || ip == null) return;
        try {
            IpGeo geo = ipLocationResolver.lookup(ip);
            if (geo == null) return;
            row.setIpCountry(geo.getCountry());
            row.setIpProvince(geo.getProvince());
            row.setIpCity(geo.getCity());
            row.setIpIsp(geo.getIsp());
        } catch (Exception e) {
            log.debug("login geo snapshot skipped");
        }
    }

    private void applyRisk(LoginLog row, String userId) {
        if (userId == null || userId.isBlank() || loginLogMapper == null) return;
        try {
            List<LoginLog> prior = loginLogMapper.selectRecentSuccess(userId, 30);
            List<LoginRiskSignals.Prior> history = new ArrayList<>();
            if (prior != null) {
                for (LoginLog old : prior) {
                    history.add(new LoginRiskSignals.Prior(
                            LoginUserAgent.deviceKey(old.getBrowserName(), old.getOsName(), old.getDeviceType()),
                            old.getIpCity()));
                }
            }
            LoginRiskSignals.Result signals = LoginRiskSignals.compare(
                    LoginUserAgent.deviceKey(row.getBrowserName(), row.getOsName(), row.getDeviceType()),
                    row.getIpCity(),
                    history);
            row.setRiskNewDevice(signals.getNewDevice());
            row.setRiskNewCity(signals.getNewCity());
        } catch (Exception e) {
            log.debug("login risk comparison skipped");
        }
    }

    private void insert(LoginLog row) {
        try {
            loginLogMapper.insertSelective(row);
        } catch (Exception richFailed) {
            log.warn("rich login log insert failed, retrying legacy columns err={}", richFailed.getMessage());
            LoginLog legacy = new LoginLog();
            legacy.setUserId(row.getUserId());
            legacy.setAction(row.getAction());
            legacy.setIpAddress(row.getIpAddress());
            legacy.setUserAgent(row.getUserAgent());
            legacy.setDelFlag(row.getDelFlag());
            legacy.setCreateTime(row.getCreateTime());
            try {
                loginLogMapper.insertSelective(legacy);
            } catch (Exception legacyFailed) {
                log.warn("login log insert failed action={} err={}", row.getAction(), legacyFailed.getMessage());
            }
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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
}
