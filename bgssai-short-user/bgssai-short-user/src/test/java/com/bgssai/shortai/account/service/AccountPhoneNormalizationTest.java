package com.bgssai.shortai.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.account.dto.ReqBindContact;
import com.bgssai.shortai.account.dto.ReqDeleteAccount;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.login.otp.InMemoryOtpStore;
import com.bgssai.shortai.login.service.PasswordService;
import com.bgssai.shortai.mapper.OauthBindingMapper;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;

class AccountPhoneNormalizationTest {

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    @Test
    void bindContactNormalizesMainlandPhoneForOtpAndStorage() {
        UserContext.setUserId("u-acc-1");
        InMemoryOtpStore otpStore = new InMemoryOtpStore();
        otpStore.save("sms", "13800138000", "123456");

        SysUser user = new SysUser();
        user.setUserId("u-acc-1");
        user.setUserName("demo");
        user.setDelFlag(false);

        SysUserMapper mapper = mock(SysUserMapper.class);
        when(mapper.selectOneByExample(any())).thenReturn(user, null, user);

        AccountLoginMethodService svc = new AccountLoginMethodService();
        ReflectionTestUtils.setField(svc, "sysUserMapper", mapper);
        ReflectionTestUtils.setField(svc, "oauthBindingMapper", mock(OauthBindingMapper.class));
        ReflectionTestUtils.setField(svc, "otpStore", otpStore);
        ReflectionTestUtils.setField(svc, "passwordService", new PasswordService());

        ReqBindContact req = new ReqBindContact();
        req.setMethodType("phone");
        req.setPhone("+86 138-0013-8000");
        req.setCode("123456");

        svc.bindContact(req);
        assertEquals("13800138000", user.getPhone());

        ReqBindContact invalid = new ReqBindContact();
        invalid.setMethodType("phone");
        invalid.setPhone("+invalid");
        invalid.setCode("123456");
        assertEquals("4301", assertThrows(BizException.class, () -> svc.bindContact(invalid)).getCode());
    }

    @Test
    void deletionVerifyIdentityMatchesNormalizedPhoneAndInternationalE164() {
        UserContext.setUserId("u-del-1");
        InMemoryOtpStore otpStore = new InMemoryOtpStore();
        otpStore.save("sms", "13800138000", "654321");

        SysUser user = new SysUser();
        user.setUserId("u-del-1");
        user.setPhone("13800138000");
        user.setDelFlag(false);

        SysUserMapper mapper = mock(SysUserMapper.class);
        when(mapper.selectOneByExample(any())).thenReturn(user);
        PaymentOrderMapper payMapper = mock(PaymentOrderMapper.class);
        when(payMapper.countByExample(any())).thenReturn(0L);

        AccountDeletionService svc = new AccountDeletionService();
        ReflectionTestUtils.setField(svc, "sysUserMapper", mapper);
        ReflectionTestUtils.setField(svc, "oauthBindingMapper", mock(OauthBindingMapper.class));
        ReflectionTestUtils.setField(svc, "paymentOrderMapper", payMapper);
        ReflectionTestUtils.setField(svc, "otpStore", otpStore);
        ReflectionTestUtils.setField(svc, "passwordService", new PasswordService());

        ReqDeleteAccount req = new ReqDeleteAccount();
        req.setVerifyMethod("sms");
        req.setPhone("+86 138-0013-8000");
        req.setCode("654321");
        req.setAcknowledgeConsequences(true);

        svc.delete(req);
        assertEquals(Boolean.TRUE, user.getDelFlag());
    }
}
