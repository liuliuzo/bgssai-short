package com.bgssai.shortai.login.service;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.account.service.AccountGuard;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.login.dto.resp.RespCodeSend;
import com.bgssai.shortai.login.dto.resp.RespUserLogin;
import com.bgssai.shortai.login.mail.SmtpMailSender;
import com.bgssai.shortai.login.oauth.OAuthIds;
import com.bgssai.shortai.login.otp.InMemoryOtpStore;
import com.bgssai.shortai.login.sms.SmsSendResult;
import com.bgssai.shortai.login.sms.TencentSmsSender;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;
import com.bgssai.shortai.utils.JwtUtil;
import com.bgssai.shortai.common.session.SingleSessionGuard;

@Service
public class OtpLoginSupport {
    @org.springframework.beans.factory.annotation.Autowired
    private SingleSessionGuard singleSessionGuard;


    private static final String USER_ROLE = "USER";
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long COOLDOWN_MS = 60_000L;

    private final InMemoryOtpStore otpStore;
    private final TencentSmsSender tencentSmsSender;
    private final SmtpMailSender smtpMailSender;
    private final SysUserMapper sysUserMapper;
    private final ConcurrentHashMap<String, Long> cooldown = new ConcurrentHashMap<>();

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
        String phone = rawPhone == null ? "" : rawPhone.trim();
        if (!PHONE.matcher(phone).matches() && !phone.startsWith("+")) {
            throw new BizException("4301", "手机号格式不正确");
        }
        acquireCooldown("sms:" + phone);
        String code = nextCode();
        otpStore.save("sms", phone, code);
        SmsSendResult sent;
        try {
            sent = tencentSmsSender.send(phone, code);
        } catch (RuntimeException e) {
            otpStore.delete("sms", phone);
            cooldown.remove("sms:" + phone);
            throw e;
        }
        return expires(sent.getProduct(), sent.getSeq());
    }

    public RespCodeSend sendEmail(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new BizException("4307", "邮箱格式不正确");
        }
        acquireCooldown("email:" + email);
        String code = nextCode();
        otpStore.save("email", email, code);
        try {
            smtpMailSender.sendLoginCode(email, code);
        } catch (RuntimeException e) {
            otpStore.delete("email", email);
            cooldown.remove("email:" + email);
            throw e;
        }
        return expires();
    }

    public RespUserLogin loginSms(String rawPhone, String code) {
        String phone = rawPhone == null ? "" : rawPhone.trim();
        if (!otpStore.consume("sms", phone, code == null ? "" : code.trim())) {
            SysUser existing = findByPhone(phone);
            loginAuditService.recordFailure(existing == null ? null : existing.getUserId(),
                    "LOGIN_FAIL_OTP", "phone_otp", phone, "auto", "验证码错误或已过期");
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
        if (!otpStore.consume("email", email, code == null ? "" : code.trim())) {
            SysUser existing = findByEmail(email);
            loginAuditService.recordFailure(existing == null ? null : existing.getUserId(),
                    "LOGIN_FAIL_OTP", "email_otp", email, "auto", "验证码错误或已过期");
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

    private SysUser findByPhone(String phone) {
        SysUserExample ex = new SysUserExample();
        ex.createCriteria().andPhoneEqualTo(phone).andDelFlagEqualTo(false);
        return sysUserMapper.selectOneByExample(ex);
    }

    private SysUser findByEmail(String email) {
        SysUserExample ex = new SysUserExample();
        ex.createCriteria().andEmailEqualTo(email).andDelFlagEqualTo(false);
        return sysUserMapper.selectOneByExample(ex);
    }

    private void acquireCooldown(String key) {
        Long last = cooldown.get(key);
        long now = System.currentTimeMillis();
        if (last != null && now - last < COOLDOWN_MS) {
            throw new BizException("4304", "发送过于频繁，请稍后再试");
        }
        cooldown.put(key, now);
    }

    private static String nextCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static RespCodeSend expires() {
        return expires(null, null);
    }

    private static RespCodeSend expires(String product, Long seq) {
        RespCodeSend resp = new RespCodeSend();
        resp.setExpiresIn(300);
        resp.setProduct(product);
        resp.setSeq(seq);
        return resp;
    }
}
