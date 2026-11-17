package com.bgssai.shortai.login.service;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.account.service.AccountGuard;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.session.SingleSessionGuard;
import com.bgssai.shortai.login.dto.resp.RespCodeSend;
import com.bgssai.shortai.login.dto.resp.RespUserLogin;
import com.bgssai.shortai.login.mail.SmtpMailSender;
import com.bgssai.shortai.login.oauth.OAuthIds;
import com.bgssai.shortai.login.otp.InMemoryOtpStore;
import com.bgssai.shortai.login.sms.TencentSmsPhoneRoute;
import com.bgssai.shortai.login.sms.TencentSmsSender;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;
import com.bgssai.shortai.utils.JwtUtil;

@Service
public class OtpLoginSupport {
    @org.springframework.beans.factory.annotation.Autowired
    private SingleSessionGuard singleSessionGuard;

    private static final String USER_ROLE = "USER";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long COOLDOWN_MS = 60_000L;

    private final InMemoryOtpStore otpStore;
    private final TencentSmsSender tencentSmsSender;
    private final SmtpMailSender smtpMailSender;
    private final SysUserMapper sysUserMapper;
    private final ConcurrentHashMap<String, Long> cooldown = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> inFlight = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    private LoginAuditService loginAuditService;

    public OtpLoginSupport(InMemoryOtpStore otpStore,
                           TencentSmsSender tencentSmsSender,
                           SmtpMailSender smtpMailSender,
                           SysUserMapper sysUserMapper) {
        this.otpStore = otpStore;
        this.tencentSmsSender = tencentSmsSender;
        this.smtpMailSender = smtpMailSender;
        this.sysUserMapper = sysUserMapper;
    }

    public RespCodeSend sendSms(String rawPhone) {
        TencentSmsPhoneRoute.Route route = TencentSmsPhoneRoute.routeOf(rawPhone);
        String phone = TencentSmsPhoneRoute.normalizeForStorage(rawPhone);
        if (route == null || phone == null) {
            throw new BizException("4301", "手机号格式不正确");
        }
        String key = "sms:" + phone;
        reserveSendSlot(key);
        try {
            String code = nextCode();
            tencentSmsSender.send(rawPhone, code);
            otpStore.save("sms", phone, code);
            cooldown.put(key, System.currentTimeMillis());
            return expires();
        } finally {
            inFlight.remove(key);
        }
    }

    public RespCodeSend sendEmail(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new BizException("4307", "邮箱格式不正确");
        }
        String key = "email:" + email;
        reserveSendSlot(key);
        try {
            String code = nextCode();
            smtpMailSender.sendLoginCode(email, code);
            otpStore.save("email", email, code);
            cooldown.put(key, System.currentTimeMillis());
            return expires();
        } finally {
            inFlight.remove(key);
        }
    }

    public RespUserLogin loginSms(String rawPhone, String code) {
        String phone = TencentSmsPhoneRoute.normalizeForStorage(rawPhone);
        String trimmedCode = code == null ? "" : code.trim();
        if (phone == null) {
            recordLoginFailure(null, "phone_otp", rawPhone == null ? "" : rawPhone.trim());
            throw new BizException("4301", "手机号格式不正确");
        }
        if (trimmedCode.isEmpty() || !otpStore.consume("sms", phone, trimmedCode)) {
            SysUser existing = findByPhone(phone);
            recordLoginFailure(existing == null ? null : existing.getUserId(), "phone_otp", phone);
            throw new BizException("4302", "验证码错误或已过期");
        }
        SysUser user = findByPhone(phone);
        boolean isNew = false;
        if (user == null) {
            user = provision(phone, phone, null);
            isNew = true;
        } else {
            AccountGuard.rejectIfDeleted(user);
        }
        return issue(user, isNew, "phone_otp", phone);
    }

    public RespUserLogin loginEmail(String rawEmail, String code) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        String trimmedCode = code == null ? "" : code.trim();
        if (email.isEmpty() || !EMAIL.matcher(email).matches()) {
            recordLoginFailure(null, "email_otp", email);
            throw new BizException("4307", "邮箱格式不正确");
        }
        if (trimmedCode.isEmpty() || !otpStore.consume("email", email, trimmedCode)) {
            SysUser existing = findByEmail(email);
            recordLoginFailure(existing == null ? null : existing.getUserId(), "email_otp", email);
            throw new BizException("4302", "验证码错误或已过期");
        }
        SysUser user = findByEmail(email);
        boolean isNew = false;
        if (user == null) {
            String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : "u";
            String suffix = local.length() <= 4 ? local : local.substring(0, 4);
            String userName = "e" + suffix + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
            user = provision(userName, null, email);
            isNew = true;
        } else {
            AccountGuard.rejectIfDeleted(user);
        }
        return issue(user, isNew, "email_otp", email);
    }

    private void recordLoginFailure(String userId, String method, String identifier) {
        if (loginAuditService != null) {
            loginAuditService.recordFailure(userId, "LOGIN_FAIL_OTP", method, identifier, "auto", "验证码错误或已过期");
        }
    }

    private SysUser provision(String userName, String phone, String email) {
        try {
            return insert(userName, phone, email);
        } catch (DuplicateKeyException e) {
            SysUser existing = phone != null ? findByPhone(phone) : findByEmail(email);
            if (existing != null) {
                return existing;
            }
            throw e;
        }
    }

    private SysUser insert(String userName, String phone, String email) {
        String userId = OAuthIds.next("u-");
        SysUser row = new SysUser();
        row.setUserId(userId);
        row.setUserName(userName);
        row.setPhone(phone);
        row.setEmail(email);
        row.setOrgCode(USER_ROLE);
        row.setStatus(Boolean.TRUE);
        row.setDelFlag(Boolean.FALSE);
        row.setCreateBy(userId);
        sysUserMapper.insertSelective(row);
        return row;
    }

    private RespUserLogin issue(SysUser user, boolean isNew, String method, String identifier) {
        String sessionId = singleSessionGuard.newSessionId();
        singleSessionGuard.bindSession(user.getUserId(), sessionId);
        String token = JwtUtil.generateToken(user.getUserId(), user.getRealName(), "01", null, null, USER_ROLE, sessionId);
        if (loginAuditService != null) {
            loginAuditService.recordSuccess(user.getUserId(), method, identifier, "auto", sessionId);
        }
        RespUserLogin resp = new RespUserLogin();
        resp.setJwtToken(token);
        resp.setRoleCode(USER_ROLE);
        resp.setRoleName("用户");
        resp.setUserId(user.getUserId());
        resp.setUserName(user.getUserName());
        resp.setRealName(user.getRealName());
        resp.setNewUser(isNew);
        return resp;
    }

    private SysUser findByPhone(String normalizedPhone) {
        SysUserExample ex = new SysUserExample();
        ex.createCriteria().andPhoneEqualTo(normalizedPhone).andDelFlagEqualTo(false);
        SysUser user = sysUserMapper.selectOneByExample(ex);
        if (user != null) {
            return user;
        }
        String mainland = TencentSmsPhoneRoute.normalizeMainland(normalizedPhone);
        if (mainland != null) {
            SysUserExample legacyEx = new SysUserExample();
            legacyEx.createCriteria().andPhoneEqualTo("+86" + mainland).andDelFlagEqualTo(false);
            return sysUserMapper.selectOneByExample(legacyEx);
        }
        return null;
    }

    private SysUser findByEmail(String email) {
        SysUserExample ex = new SysUserExample();
        ex.createCriteria().andEmailEqualTo(email).andDelFlagEqualTo(false);
        return sysUserMapper.selectOneByExample(ex);
    }

    private void reserveSendSlot(String key) {
        Long last = cooldown.get(key);
        long now = System.currentTimeMillis();
        if (last != null && now - last < COOLDOWN_MS) {
            throw new BizException("4304", "发送过于频繁，请稍后再试");
        }
        if (inFlight.putIfAbsent(key, Boolean.TRUE) != null) {
            throw new BizException("4304", "发送过于频繁，请稍后再试");
        }
    }

    private static String nextCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static RespCodeSend expires() {
        RespCodeSend resp = new RespCodeSend();
        resp.setExpiresIn(300);
        return resp;
    }
}
