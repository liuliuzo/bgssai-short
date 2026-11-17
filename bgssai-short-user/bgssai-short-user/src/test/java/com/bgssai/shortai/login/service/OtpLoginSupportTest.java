package com.bgssai.shortai.login.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.session.SingleSessionGuard;
import com.bgssai.shortai.login.dto.resp.RespCodeSend;
import com.bgssai.shortai.login.dto.resp.RespUserLogin;
import com.bgssai.shortai.login.mail.SmtpMailSender;
import com.bgssai.shortai.login.otp.InMemoryOtpStore;
import com.bgssai.shortai.login.sms.TencentSmsSender;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

class OtpLoginSupportTest {

    @BeforeAll
    static void initJwt() {
        ReflectionTestUtils.invokeMethod(JwtUtil.class, "configure",
                "test-only-jwt-secret-key-minimum-32-bytes-0123456789", 3_600_000L);
    }

    @Test
    void rejectsMalformedPlusPrefixPhoneBeforeSend() {
        InMemoryOtpStore otpStore = new InMemoryOtpStore();
        TencentSmsSender smsSender = mock(TencentSmsSender.class);
        SmtpMailSender mailSender = mock(SmtpMailSender.class);
        SysUserMapper mapper = mock(SysUserMapper.class);
        OtpLoginSupport support = new OtpLoginSupport(otpStore, smsSender, mailSender, mapper);

        BizException barePlus = assertThrows(BizException.class, () -> support.sendSms("+"));
        assertEquals("4301", barePlus.getCode());
        BizException badPlus = assertThrows(BizException.class, () -> support.sendSms("+86abc"));
        assertEquals("4301", badPlus.getCode());
    }

    @Test
    void sendFailureLeavesNoValidOtpAndNoCooldownLock() throws Exception {
        InMemoryOtpStore otpStore = new InMemoryOtpStore();
        TencentSmsSender smsSender = mock(TencentSmsSender.class);
        SmtpMailSender mailSender = mock(SmtpMailSender.class);
        SysUserMapper mapper = mock(SysUserMapper.class);
        when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
        OtpLoginSupport support = new OtpLoginSupport(otpStore, smsSender, mailSender, mapper);
        ReflectionTestUtils.setField(support, "singleSessionGuard", new SingleSessionGuard(mapper));

        String[] sentCode = new String[1];
        doThrow(new BizException("5203", "短信通道暂不可用"))
                .doAnswer(inv -> {
                    sentCode[0] = inv.getArgument(1);
                    return null;
                })
                .when(smsSender).send(anyString(), anyString());

        BizException failEx = assertThrows(BizException.class, () -> support.sendSms("+8613800138000"));
        assertEquals("5203", failEx.getCode());
        assertFalse(otpStore.consume("sms", "13800138000", "000000"));

        // Must be immediately retryable with zero cooldown lock after previous failure
        RespCodeSend resp = support.sendSms("13800138000");
        assertEquals(300, resp.getExpiresIn());
        String json = new ObjectMapper().writeValueAsString(resp);
        assertFalse(json.contains("product"));
        assertFalse(json.contains("seq"));
        assertNotNull(sentCode[0]);

        SysUser existing = new SysUser();
        existing.setUserId("u-1");
        existing.setUserName("13800138000");
        existing.setPhone("13800138000");
        existing.setDelFlag(false);
        when(mapper.selectOneByExample(any())).thenReturn(existing);

        RespUserLogin login = support.loginSms("+86 138-0013-8000", sentCode[0]);
        assertEquals("u-1", login.getUserId());
        assertEquals("4302", assertThrows(BizException.class, () -> support.loginSms("13800138000", sentCode[0])).getCode());
    }

    @Test
    void emailSendFailureLeavesNoOtpAndNoCooldown() {
        InMemoryOtpStore otpStore = new InMemoryOtpStore();
        TencentSmsSender smsSender = mock(TencentSmsSender.class);
        SmtpMailSender mailSender = mock(SmtpMailSender.class);
        SysUserMapper mapper = mock(SysUserMapper.class);
        when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
        OtpLoginSupport support = new OtpLoginSupport(otpStore, smsSender, mailSender, mapper);
        ReflectionTestUtils.setField(support, "singleSessionGuard", new SingleSessionGuard(mapper));

        String[] sentCode = new String[1];
        doThrow(new BizException("5204", "邮箱通道暂不可用"))
                .doAnswer(inv -> {
                    sentCode[0] = inv.getArgument(1);
                    return null;
                })
                .when(mailSender).sendLoginCode(anyString(), anyString());

        assertEquals("5204", assertThrows(BizException.class, () -> support.sendEmail("User@Example.COM")).getCode());

        RespCodeSend ok = support.sendEmail("user@example.com");
        assertEquals(300, ok.getExpiresIn());
        verify(mailSender, org.mockito.Mockito.times(2)).sendLoginCode(anyString(), anyString());

        when(mapper.selectOneByExample(any())).thenReturn(null);
        RespUserLogin login = support.loginEmail("user@example.com", sentCode[0]);
        assertTrue(Boolean.TRUE.equals(login.getNewUser()));
    }

    @Test
    void otpStoreEvictsAfterFiveWrongAttemptsAndIsSingleUse() {
        InMemoryOtpStore otpStore = new InMemoryOtpStore();
        otpStore.save("sms", "+85291234567", "654321");

        for (int i = 0; i < 4; i++) {
            assertFalse(otpStore.consume("sms", "+85291234567", "111111"));
        }
        assertFalse(otpStore.consume("sms", "+85291234567", "000000"));
        assertFalse(otpStore.consume("sms", "+85291234567", "654321"));
    }
}
