package com.bgssai.shortai.account.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.account.dto.ReqBindContact;
import com.bgssai.shortai.account.dto.ReqBindOAuth;
import com.bgssai.shortai.account.dto.ReqUnbindLoginMethod;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.login.oauth.OAuthIds;
import com.bgssai.shortai.login.oauth.OAuthProvider;
import com.bgssai.shortai.login.oauth.OAuthProviderFactory;
import com.bgssai.shortai.login.oauth.OAuthStateUtil;
import com.bgssai.shortai.login.oauth.OAuthUserInfo;
import com.bgssai.shortai.login.otp.InMemoryOtpStore;
import com.bgssai.shortai.login.service.PasswordService;
import com.bgssai.shortai.mapper.OauthBindingMapper;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.OauthBinding;
import com.bgssai.shortai.model.OauthBindingExample;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;

@Service
public class AccountLoginMethodService {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private OauthBindingMapper oauthBindingMapper;
    @Autowired
    private OAuthStateUtil oauthStateUtil;
    @Autowired
    private OAuthProviderFactory oauthProviderFactory;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private InMemoryOtpStore otpStore;

    public ApiResponse listMethods() {
        SysUser user = requireUser();
        List<Map<String, Object>> methods = collectMethods(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method_list", methods);
        result.put("method_count", methods.size());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse bindOAuth(ReqBindOAuth req) {
        if (req == null || req.getPlatform() == null || req.getPlatform().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "platform required");
        }
        SysUser user = requireUser();
        String platform = req.getPlatform().trim().toLowerCase(Locale.ROOT);
        oauthStateUtil.verify(req.getState(), platform);
        OAuthProvider provider = oauthProviderFactory.get(platform);
        OAuthUserInfo info = provider.fetchUserInfo(req.getCode());

        OauthBinding existing = findBinding(platform, info.getOpenId());
        if (existing != null) {
            if (user.getUserId().equals(existing.getUserId())) {
                return Responses.ok(Map.of("bound", true, "platform", platform, "already", true));
            }
            throw new BizException(CommonErrorCode.BINDING_CONFLICT);
        }

        OauthBindingExample own = new OauthBindingExample();
        own.createCriteria().andUserIdEqualTo(user.getUserId()).andPlatformEqualTo(platform)
                .andDelFlagEqualTo(Boolean.FALSE);
        if (oauthBindingMapper.countByExample(own) > 0) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "该登录方式已绑定");
        }

        OauthBinding row = new OauthBinding();
        row.setBindingId(OAuthIds.next("ob-"));
        row.setPlatform(platform);
        row.setOpenId(info.getOpenId());
        row.setUnionId(info.getUnionId());
        row.setUserId(user.getUserId());
        row.setNickname(info.getNickname());
        row.setAvatarUrl(info.getAvatarUrl());
        row.setDelFlag(Boolean.FALSE);
        row.setCreator(user.getUserId());
        row.setModifier(user.getUserId());
        try {
            oauthBindingMapper.insertSelective(row);
        } catch (DuplicateKeyException e) {
            throw new BizException(CommonErrorCode.BINDING_CONFLICT);
        }
        return Responses.ok(Map.of("bound", true, "platform", platform));
    }

    @Transactional
    public ApiResponse bindContact(ReqBindContact req) {
        if (req == null || req.getMethodType() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "method_type required");
        }
        SysUser user = requireUser();
        String type = req.getMethodType().trim().toLowerCase(Locale.ROOT);
        if ("phone".equals(type)) {
            String phone = req.getPhone() == null ? "" : req.getPhone().trim();
            if (!otpStore.consume("sms", phone, req.getCode() == null ? "" : req.getCode().trim())) {
                throw new BizException("4302", "验证码错误或已过期");
            }
            SysUserExample clash = new SysUserExample();
            clash.createCriteria().andPhoneEqualTo(phone).andDelFlagEqualTo(false);
            SysUser other = sysUserMapper.selectOneByExample(clash);
            if (other != null && !other.getUserId().equals(user.getUserId())) {
                throw new BizException(CommonErrorCode.BINDING_CONFLICT, "手机号已绑定其他账号");
            }
            user.setPhone(phone);
            user.setUpdateTime(new Date());
            sysUserMapper.updateByPrimaryKeySelective(user);
            return Responses.ok(Map.of("bound", true, "method_type", "phone"));
        }
        if ("email".equals(type)) {
            String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!otpStore.consume("email", email, req.getCode() == null ? "" : req.getCode().trim())) {
                throw new BizException("4302", "验证码错误或已过期");
            }
            SysUserExample clash = new SysUserExample();
            clash.createCriteria().andEmailEqualTo(email).andDelFlagEqualTo(false);
            SysUser other = sysUserMapper.selectOneByExample(clash);
            if (other != null && !other.getUserId().equals(user.getUserId())) {
                throw new BizException(CommonErrorCode.BINDING_CONFLICT, "邮箱已绑定其他账号");
            }
            user.setEmail(email);
            user.setUpdateTime(new Date());
            sysUserMapper.updateByPrimaryKeySelective(user);
            return Responses.ok(Map.of("bound", true, "method_type", "email"));
        }
        throw new BizException(CommonErrorCode.INVALID_PARAM, "method_type 无效");
    }

    @Transactional
    public ApiResponse unbind(ReqUnbindLoginMethod req) {
        if (req == null || req.getMethodType() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "method_type required");
        }
        SysUser user = requireUser();
        reauth(user, req);
        List<Map<String, Object>> before = collectMethods(user);
        if (before.size() <= 1) {
            throw new BizException(CommonErrorCode.LAST_LOGIN_METHOD);
        }
        String type = req.getMethodType().trim().toLowerCase(Locale.ROOT);
        if ("password".equals(type)) {
            user.setPassword("");
            user.setUpdateTime(new Date());
            sysUserMapper.updateByPrimaryKeySelective(user);
        } else if ("phone".equals(type)) {
            user.setPhone(null);
            // selective skips null — use full update
            SysUser full = load(user.getUserId());
            full.setPhone(null);
            full.setUpdateTime(new Date());
            sysUserMapper.updateByPrimaryKey(full);
        } else if ("email".equals(type)) {
            SysUser full = load(user.getUserId());
            full.setEmail(null);
            full.setUpdateTime(new Date());
            sysUserMapper.updateByPrimaryKey(full);
        } else if ("oauth".equals(type)) {
            String platform = req.getPlatform() == null ? "" : req.getPlatform().trim().toLowerCase(Locale.ROOT);
            if (platform.isBlank()) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "platform required");
            }
            OauthBindingExample ex = new OauthBindingExample();
            ex.createCriteria().andUserIdEqualTo(user.getUserId()).andPlatformEqualTo(platform)
                    .andDelFlagEqualTo(Boolean.FALSE);
            OauthBinding binding = oauthBindingMapper.selectOneByExample(ex);
            if (binding == null) {
                throw new BizException(CommonErrorCode.RESOURCE_NOT_FOUND, "未绑定该方式");
            }
            binding.setDelFlag(Boolean.TRUE);
            binding.setModifier(user.getUserId());
            binding.setOpenId("unbound:" + binding.getBindingId() + ":" + System.currentTimeMillis());
            oauthBindingMapper.updateByPrimaryKeySelective(binding);
        } else {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "method_type 无效");
        }
        List<Map<String, Object>> after = collectMethods(load(user.getUserId()));
        if (after.isEmpty()) {
            throw new BizException(CommonErrorCode.LAST_LOGIN_METHOD);
        }
        return Responses.ok(Map.of("unbound", true, "method_list", after));
    }

    private void reauth(SysUser user, ReqUnbindLoginMethod req) {
        String method = req.getVerifyMethod() == null ? "" : req.getVerifyMethod().trim().toLowerCase(Locale.ROOT);
        if ("password".equals(method)) {
            if (!passwordService.matches(user, req.getPassword())) {
                throw new BizException(CommonErrorCode.INVALID_CREDENTIALS);
            }
            return;
        }
        if ("sms".equals(method)) {
            if (user.getPhone() == null || !otpStore.consume("sms", user.getPhone(),
                    req.getCode() == null ? "" : req.getCode().trim())) {
                throw new BizException("4302", "验证码错误或已过期");
            }
            return;
        }
        if ("email".equals(method)) {
            if (user.getEmail() == null || !otpStore.consume("email", user.getEmail(),
                    req.getCode() == null ? "" : req.getCode().trim())) {
                throw new BizException("4302", "验证码错误或已过期");
            }
            return;
        }
        throw new BizException(CommonErrorCode.INVALID_PARAM, "敏感变更需重新验证身份");
    }

    private List<Map<String, Object>> collectMethods(SysUser user) {
        List<Map<String, Object>> methods = new ArrayList<>();
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            methods.add(method("password", "password", "账号密码", true));
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            methods.add(method("phone", "phone", maskPhone(user.getPhone()), true));
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            methods.add(method("email", "email", maskEmail(user.getEmail()), true));
        }
        OauthBindingExample ex = new OauthBindingExample();
        ex.createCriteria().andUserIdEqualTo(user.getUserId()).andDelFlagEqualTo(Boolean.FALSE);
        List<OauthBinding> bindings = oauthBindingMapper.selectByExample(ex);
        if (bindings != null) {
            for (OauthBinding b : bindings) {
                methods.add(method("oauth", b.getPlatform(),
                        b.getNickname() == null ? b.getPlatform() : b.getNickname(), true));
            }
        }
        return methods;
    }

    private static Map<String, Object> method(String type, String platform, String label, boolean bound) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("method_type", type);
        m.put("platform", platform);
        m.put("label", label);
        m.put("bound", bound);
        return m;
    }

    private OauthBinding findBinding(String platform, String openId) {
        OauthBindingExample ex = new OauthBindingExample();
        ex.createCriteria().andPlatformEqualTo(platform).andOpenIdEqualTo(openId)
                .andDelFlagEqualTo(Boolean.FALSE);
        return oauthBindingMapper.selectOneByExample(ex);
    }

    private SysUser requireUser() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("no user in context");
        }
        SysUser user = load(userId);
        if (user == null) {
            throw new BizException(CommonErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        AccountGuard.rejectIfDeleted(user);
        return user;
    }

    private SysUser load(String userId) {
        SysUserExample ex = new SysUserExample();
        ex.createCriteria().andUserIdEqualTo(userId);
        return sysUserMapper.selectOneByExample(ex);
    }

    private static String maskPhone(String phone) {
        if (phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
