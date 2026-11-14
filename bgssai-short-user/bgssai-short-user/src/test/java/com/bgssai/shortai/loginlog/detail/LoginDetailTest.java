package com.bgssai.shortai.loginlog.detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bgssai.shortai.common.logging.MdcSupport;
import com.bgssai.shortai.login.service.LoginAuditService;
import com.bgssai.shortai.loginlog.location.IpGeo;
import com.bgssai.shortai.loginlog.location.LoginIpLocationResolver;
import com.bgssai.shortai.mapper.LoginLogMapper;
import com.bgssai.shortai.model.LoginLog;

class LoginDetailTest {

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void parsesBrowserOsAndDeviceWithoutGuessingAnEmptyAgent() {
        LoginUserAgent.Summary chrome = LoginUserAgent.parse(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        assertEquals("Chrome 120", chrome.getBrowserName());
        assertEquals("Windows 10", chrome.getOsName());
        assertEquals("desktop", chrome.getDeviceType());
        assertEquals("Chrome 120 · Windows 10 · 桌面", chrome.getDeviceLabel());

        LoginUserAgent.Summary phone = LoginUserAgent.parse(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
        assertEquals("Safari 17", phone.getBrowserName());
        assertEquals("iOS 17", phone.getOsName());
        assertEquals("mobile", phone.getDeviceType());

        LoginUserAgent.Summary empty = LoginUserAgent.parse(" ");
        assertNull(empty.getBrowserName());
        assertNull(empty.getDeviceLabel());
        assertNull(LoginUserAgent.deviceKey(null, null, null));
    }

    @Test
    void masksEmailAndPhoneAndDropsCodesAndTokens() {
        LoginIdentifierMask.Masked email = LoginIdentifierMask.mask("buyer@example.com", "auto");
        assertEquals("email", email.getType());
        assertEquals("b***@example.com", email.getValue());
        assertFalse(email.getValue().contains("buyer"));

        LoginIdentifierMask.Masked phone = LoginIdentifierMask.mask("13800138000", "auto");
        assertEquals("phone", phone.getType());
        assertEquals("138****8000", phone.getValue());

        assertEquals("username", LoginIdentifierMask.mask("demo", "auto").getType());
        assertNull(LoginIdentifierMask.mask("123456", "auto").getValue());
        assertNull(LoginIdentifierMask.mask("eyJhbGciOiJIUzI1NiJ9.aaa.bbb", "auto").getValue());
        assertEquals("微信", LoginIdentifierMask.mask("wechat", "oauth").getValue());
        assertEquals("oauth", LoginIdentifierMask.mask("wechat", "oauth").getType());
    }

    @Test
    void riskUsesOnlyComparableHistory() {
        LoginRiskSignals.Result first = LoginRiskSignals.compare("Chrome|Windows|desktop", "南京市", List.of());
        assertNull(first.getNewDevice());
        assertNull(first.getNewCity());

        LoginLog prior = new LoginLog();
        prior.setBrowserName("Chrome 120");
        prior.setOsName("Windows 10");
        prior.setDeviceType("desktop");
        prior.setIpCity("上海市");
        LoginRiskSignals.Result changed = LoginRiskSignals.compare(
                "Safari 17|iOS|mobile",
                "南京市",
                List.of(new LoginRiskSignals.Prior(
                        LoginUserAgent.deviceKey(prior.getBrowserName(), prior.getOsName(), prior.getDeviceType()),
                        prior.getIpCity())));
        assertEquals(Boolean.TRUE, changed.getNewDevice());
        assertEquals(Boolean.TRUE, changed.getNewCity());
        assertEquals("新设备、新城市", LoginRiskSignals.summary(changed.getNewDevice(), changed.getNewCity()));

        LoginRiskSignals.Result same = LoginRiskSignals.compare(
                "Chrome 120|Windows|desktop",
                "上海市",
                List.of(new LoginRiskSignals.Prior("Chrome 120|Windows|desktop", "上海市")));
        assertEquals(Boolean.FALSE, same.getNewDevice());
        assertEquals(Boolean.FALSE, same.getNewCity());
        assertNull(LoginRiskSignals.compare(null, null, List.of(new LoginRiskSignals.Prior(null, null))).getNewDevice());
    }

    @Test
    void requestFactsKeepLanguageAndDropQuerySecrets() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.8");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");
        request.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
        request.addHeader("Referer", "https://www.bgssai.com/login?code=secret-oauth-code");
        request.addHeader("Sec-CH-UA-Platform", "\"Windows\"");
        request.addHeader("Sec-CH-UA-Mobile", "?0");
        request.setRequestURI("/bgssai/user/login");
        request.setScheme("http");
        SSLSession ssl = mock(SSLSession.class);
        when(ssl.getProtocol()).thenReturn("TLSv1.3");
        request.setAttribute("jakarta.servlet.request.ssl_session", ssl);
        MDC.put(MdcSupport.TRACE_ID, "trace1234abcd");

        LoginRequestFacts facts = LoginRequestFacts.read(request, "web");
        assertEquals("198.51.100.8", facts.getIp());
        assertEquals("https", facts.getScheme());
        assertEquals("TLSv1.3", facts.getTlsProtocol());
        assertEquals("www.bgssai.com", facts.getRefererHost());
        assertFalse(String.valueOf(facts.getRefererHost()).contains("secret"));
        assertEquals("/bgssai/user/login", facts.getEntryPath());
        assertEquals("zh-CN", facts.getAcceptLanguage());
        assertEquals("trace1234abcd", facts.getRequestId());
        assertEquals("web", facts.getClientChannel());
        assertTrue(facts.getClientHints().contains("platform=Windows"));
        assertNull(facts.getClientTimezone());
        assertNull(LoginRequestFacts.read(null, "web").getTlsProtocol());
        assertNull(LoginRequestFacts.dropSecret("eyJaaa.bbb.ccc"));
    }

    @Test
    void auditStoresMaskedContextAndOmitsPasswordOtpAndToken() {
        LoginLogMapper mapper = mock(LoginLogMapper.class);
        LoginIpLocationResolver resolver = mock(LoginIpLocationResolver.class);
        when(resolver.lookup("198.51.100.8")).thenReturn(new IpGeo("美国", "美国", null, null, "Example"));
        LoginLog seen = new LoginLog();
        seen.setBrowserName("Firefox 100");
        seen.setOsName("Linux");
        seen.setDeviceType("desktop");
        seen.setIpCity("北京市");
        when(mapper.selectRecentSuccess("user-1", 30)).thenReturn(List.of(seen));

        LoginAuditService service = new LoginAuditService();
        ReflectionTestUtils.setField(service, "loginLogMapper", mapper);
        ReflectionTestUtils.setField(service, "ipLocationResolver", resolver);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.8");
        request.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) Chrome/121.0.0.0 Safari/537.36");
        request.addHeader("Accept-Language", "zh-CN");
        request.setRequestURI("/bgssai/user/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put(MdcSupport.TRACE_ID, "abcd1234ef567890");

        service.recordSuccess("user-1", "password", "buyer@example.com", "auto", "session1234567890abcd");
        service.recordFailure(null, "LOGIN_FAIL_OTP", "phone_otp", "123456", "auto", "验证码错误或已过期");

        var captor = org.mockito.ArgumentCaptor.forClass(LoginLog.class);
        verify(mapper, atLeastOnce()).insertSelective(captor.capture());
        LoginLog success = captor.getAllValues().get(0);
        assertEquals("SUCCESS", success.getLoginResult());
        assertEquals("登录成功", success.getResultReason());
        assertEquals("password", success.getLoginMethod());
        assertEquals("email", success.getIdentifierType());
        assertEquals("b***@example.com", success.getIdentifierMasked());
        assertEquals("web", success.getClientChannel());
        assertEquals("Chrome 121", success.getBrowserName());
        assertEquals("desktop", success.getDeviceType());
        assertEquals("198.51.100.8", success.getIpAddress());
        assertEquals("美国", success.getIpCountry());
        assertEquals("Example", success.getIpIsp());
        assertEquals("/bgssai/user/login", success.getEntryPath());
        assertEquals("zh-CN", success.getAcceptLanguage());
        assertEquals("abcd1234ef567890", success.getRequestId());
        assertEquals("session1234567890abcd", success.getSessionId());
        assertNotNull(success.getSessionIssuedAt());
        assertTrue(success.getSessionExpiresAt().after(success.getSessionIssuedAt()));
        assertEquals(Boolean.TRUE, success.getRiskNewDevice());
        assertNull(success.getRiskNewCity());
        assertFalse(success.getIdentifierMasked().contains("buyer"));
        assertNull(success.getTlsProtocol());

        LoginLog failure = captor.getAllValues().get(1);
        assertEquals("FAILURE", failure.getLoginResult());
        assertEquals("验证码错误或已过期", failure.getResultReason());
        assertNull(failure.getIdentifierMasked());
        assertNull(failure.getSessionId());
        String dumped = String.valueOf(failure.getUserAgent()) + failure.getResultReason() + failure.getIdentifierMasked();
        assertFalse(dumped.contains("123456"));
    }
}
