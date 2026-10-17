package com.bgssai.shortai.account.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.account.dto.ReqDeleteAccount;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.login.otp.InMemoryOtpStore;
import com.bgssai.shortai.login.service.PasswordService;
import com.bgssai.shortai.mapper.OauthBindingMapper;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.OauthBinding;
import com.bgssai.shortai.model.OauthBindingExample;
import com.bgssai.shortai.model.PaymentOrderExample;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;

@Service
public class AccountDeletionService {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private OauthBindingMapper oauthBindingMapper;
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private InMemoryOtpStore otpStore;
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public ApiResponse preview() {
        SysUser user = requireUser();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consequences", List.of(
                "注销后无法再用密码、验证码或第三方授权登录原账号",
                "个人资料（手机、邮箱、昵称）将被匿名化",
                "未支付订单与进行中任务需先处理，余额与作品按已公布规则保留或匿名归属",
                "注销前签发的登录凭证全部失效"));
        result.put("open_payment_orders", countOpenOrders(user.getUserId()));
        result.put("running_tasks", countRunningTasks(user.getUserId()));
        result.put("available_quota_tokens", null);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse delete(ReqDeleteAccount req) {
        if (req == null || !Boolean.TRUE.equals(req.getAcknowledgeConsequences())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请先阅读并确认注销后果");
        }
        SysUser user = requireUser();
        verifyIdentity(user, req);

        int openOrders = countOpenOrders(user.getUserId());
        int running = countRunningTasks(user.getUserId());
        if ((openOrders > 0 || running > 0) && !Boolean.TRUE.equals(req.getForceDespiteOpenItems())) {
            throw new BizException(CommonErrorCode.ACCOUNT_HAS_OPEN_ITEMS,
                    "存在未结订单或进行中任务，请先处理后再注销");
        }

        Date now = new Date();
        String anon = "deleted_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        user.setUserName(anon);
        user.setRealName("已注销用户");
        user.setPassword("");
        user.setPhone(null);
        user.setEmail(null);
        user.setAvatar(null);
        user.setCurrentSessionId("");
        user.setDeletedAt(now);
        user.setDelFlag(Boolean.TRUE);
        user.setStatus(Boolean.FALSE);
        user.setUpdateBy(user.getUserId());
        user.setUpdateTime(now);
        sysUserMapper.updateByPrimaryKey(user);

        OauthBindingExample bex = new OauthBindingExample();
        bex.createCriteria().andUserIdEqualTo(user.getUserId()).andDelFlagEqualTo(Boolean.FALSE);
        List<OauthBinding> bindings = oauthBindingMapper.selectByExample(bex);
        if (bindings != null) {
            for (OauthBinding b : bindings) {
                b.setDelFlag(Boolean.TRUE);
                b.setModifier(user.getUserId());
                b.setOpenId("deleted:" + b.getBindingId());
                oauthBindingMapper.updateByPrimaryKeySelective(b);
            }
        }

        if (jdbcTemplate != null) {
            jdbcTemplate.update(
                    "INSERT INTO account_deletion_log(user_id, verify_method, reason, anonymized, creator, modifier) VALUES (?,?,?,?,?,?)",
                    user.getUserId(),
                    normalize(req.getVerifyMethod()),
                    req.getReason() == null ? null : req.getReason().trim(),
                    1,
                    user.getUserId(),
                    user.getUserId());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted", true);
        result.put("deleted_at", now.getTime());
        return Responses.ok(result);
    }

    private void verifyIdentity(SysUser user, ReqDeleteAccount req) {
        String method = normalize(req.getVerifyMethod());
        if ("password".equals(method)) {
            if (!passwordService.matches(user, req.getPassword())) {
                throw new BizException(CommonErrorCode.INVALID_CREDENTIALS);
            }
            return;
        }
        if ("sms".equals(method)) {
            String phone = req.getPhone() == null ? user.getPhone() : req.getPhone().trim();
            if (phone == null || phone.isBlank() || !phone.equals(user.getPhone())) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "手机号与账号不一致");
            }
            if (!otpStore.consume("sms", phone, req.getCode() == null ? "" : req.getCode().trim())) {
                throw new BizException("4302", "验证码错误或已过期");
            }
            return;
        }
        if ("email".equals(method)) {
            String email = req.getEmail() == null ? user.getEmail()
                    : req.getEmail().trim().toLowerCase(Locale.ROOT);
            if (email == null || email.isBlank() || user.getEmail() == null
                    || !email.equalsIgnoreCase(user.getEmail())) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "邮箱与账号不一致");
            }
            if (!otpStore.consume("email", email, req.getCode() == null ? "" : req.getCode().trim())) {
                throw new BizException("4302", "验证码错误或已过期");
            }
            return;
        }
        throw new BizException(CommonErrorCode.INVALID_PARAM, "verify_method 无效");
    }

    private int countOpenOrders(String userId) {
        PaymentOrderExample ex = new PaymentOrderExample();
        ex.createCriteria().andUserIdEqualTo(userId).andStatusEqualTo("created").andDelFlagEqualTo(0);
        return (int) paymentOrderMapper.countByExample(ex);
    }

    private int countRunningTasks(String userId) {
        if (jdbcTemplate == null) {
            return 0;
        }
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM short_ai_task WHERE user_id=? AND del_flag=0 AND task_status IN ('QUEUED','RUNNING','PAUSED')",
                    Integer.class, userId);
            return n == null ? 0 : n;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private SysUser requireUser() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("no user in context");
        }
        SysUserExample ex = new SysUserExample();
        ex.createCriteria().andUserIdEqualTo(userId);
        SysUser user = sysUserMapper.selectOneByExample(ex);
        if (user == null) {
            throw new BizException(CommonErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        AccountGuard.rejectIfDeleted(user);
        return user;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
