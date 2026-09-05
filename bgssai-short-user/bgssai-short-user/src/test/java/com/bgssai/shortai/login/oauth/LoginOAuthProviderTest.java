package com.bgssai.shortai.login.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.exception.BizException;

/**
 * 登录页微信 / 支付宝 / 抖音 / 百度 / BGSSAI 授权 URL：库凭据为空必须拒绝，配齐后才能拼出平台授权地址。
 */
class LoginOAuthProviderTest {

    @Test
    void wechat_blank_credentials_throw() {
        WechatOAuthProvider provider = new WechatOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of()));
        BizException ex = assertThrows(BizException.class, () -> provider.buildAuthorizeUrl("state-1"));
        assertEquals("4403", ex.getCode());
        assertEquals("该登录方式暂不可用", ex.getMessage());
    }

    @Test
    void alipay_blank_credentials_throw() {
        AlipayOAuthProvider provider = new AlipayOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of()));
        BizException ex = assertThrows(BizException.class, () -> provider.buildAuthorizeUrl("state-1"));
        assertEquals("4403", ex.getCode());
    }

    @Test
    void douyin_blank_credentials_throw() {
        DouyinOAuthProvider provider = new DouyinOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of()));
        BizException ex = assertThrows(BizException.class, () -> provider.buildAuthorizeUrl("state-1"));
        assertEquals("4403", ex.getCode());
    }

    @Test
    void baidu_blank_credentials_throw() {
        BaiduOAuthProvider provider = new BaiduOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of()));
        BizException ex = assertThrows(BizException.class, () -> provider.buildAuthorizeUrl("state-1"));
        assertEquals("4403", ex.getCode());
        assertEquals("该登录方式暂不可用", ex.getMessage());
    }
    @Test
    void bgssai_blank_credentials_throw() {
        BgssaiOAuthProvider provider = new BgssaiOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of()));
        ReflectionTestUtils.setField(provider, "idpBaseUrl", "https://www.bgssai.com");
        BizException ex = assertThrows(BizException.class, () -> provider.buildAuthorizeUrl("state-1"));
        assertEquals("4403", ex.getCode());
        assertEquals("该登录方式暂不可用", ex.getMessage());
    }

    @Test
    void wechat_configured_builds_qrconnect_url() {
        WechatOAuthProvider provider = new WechatOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of(
                LoginOAuthSecrets.WECHAT_APP_ID, "wx_test_app",
                LoginOAuthSecrets.WECHAT_APP_SECRET, "wx_test_secret")));
        String url = provider.buildAuthorizeUrl("csrf-state");
        assertTrue(url.startsWith("https://open.weixin.qq.com/connect/qrconnect?"));
        assertTrue(url.contains("appid=wx_test_app"));
        assertTrue(url.contains("oauth-callback"));
        assertTrue(url.contains("state=csrf-state"));
    }

    @Test
    void alipay_configured_builds_authorize_url() {
        AlipayOAuthProvider provider = new AlipayOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of(
                LoginOAuthSecrets.ALIPAY_APP_ID, "2021000000000000",
                LoginOAuthSecrets.ALIPAY_APP_PRIVATE_KEY, "dummy-private-key")));
        String url = provider.buildAuthorizeUrl("csrf-state");
        assertTrue(url.startsWith("https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?"));
        assertTrue(url.contains("app_id=2021000000000000"));
        assertTrue(url.contains("oauth-callback"));
    }

    @Test
    void douyin_configured_builds_connect_url() {
        DouyinOAuthProvider provider = new DouyinOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of(
                LoginOAuthSecrets.DOUYIN_CLIENT_KEY, "douyin_key",
                LoginOAuthSecrets.DOUYIN_CLIENT_SECRET, "douyin_secret")));
        String url = provider.buildAuthorizeUrl("csrf-state");
        assertTrue(url.startsWith("https://open.douyin.com/platform/oauth/connect?"));
        assertTrue(url.contains("client_key=douyin_key"));
        assertTrue(url.contains("oauth-callback"));
    }

    @Test
    void baidu_configured_builds_authorize_url() {
        BaiduOAuthProvider provider = new BaiduOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of(
                LoginOAuthSecrets.BAIDU_CLIENT_ID, "baidu_api_key",
                LoginOAuthSecrets.BAIDU_CLIENT_SECRET, "baidu_secret")));
        String url = provider.buildAuthorizeUrl("csrf-state");
        assertTrue(url.startsWith("https://openapi.baidu.com/oauth/2.0/authorize?"));
        assertTrue(url.contains("client_id=baidu_api_key"));
        assertTrue(url.contains("oauth-callback"));
        assertTrue(url.contains("state=csrf-state"));
    }
    @Test
    void bgssai_configured_builds_authorize_url() {
        BgssaiOAuthProvider provider = new BgssaiOAuthProvider();
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of(
                LoginOAuthSecrets.BGSSAI_CLIENT_ID, "bgssai-short",
                LoginOAuthSecrets.BGSSAI_CLIENT_SECRET, "demo-secret-change-me")));
        ReflectionTestUtils.setField(provider, "idpBaseUrl", "https://www.bgssai.com");
        String url = provider.buildAuthorizeUrl("csrf-state");
        assertTrue(url.startsWith("https://www.bgssai.com/oauth/authorize?"));
        assertTrue(url.contains("client_id=bgssai-short"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("scope=profile%20email"));
        assertTrue(url.contains("oauth-callback"));
        assertTrue(url.contains("state=csrf-state"));
    }

    private static OAuthProperties redirectOnlyProperties() {
        OAuthProperties props = new OAuthProperties();
        String redirect = "https://dev.user.bgssai-short.com/oauth-callback";
        props.getWechat().setRedirectUri(redirect);
        props.getAlipay().setRedirectUri(redirect);
        props.getDouyin().setRedirectUri(redirect);
        props.getBaidu().setRedirectUri(redirect);
        props.getBgssai().setRedirectUri(redirect);
        return props;
    }

    private static LoginOAuthSecrets secretsWith(Map<String, String> values) {
        return new LoginOAuthSecrets(key -> {
            String found = values.get(key);
            return found == null || found.isBlank() ? "" : found;
        });
    }
}

