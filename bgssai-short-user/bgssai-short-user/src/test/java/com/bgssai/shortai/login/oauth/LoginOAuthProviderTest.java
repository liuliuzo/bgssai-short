package com.bgssai.shortai.login.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.login.service.OAuthChannelsService;

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
                LoginOAuthSecrets.ALIPAY_APP_PRIVATE_KEY, "dummy-private-key",
                LoginOAuthSecrets.ALIPAY_PUBLIC_KEY, "dummy-public-key")));
        String url = provider.buildAuthorizeUrl("csrf-state");
        assertTrue(url.startsWith("https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?"));
        assertTrue(url.contains("app_id=2021000000000000"));
        assertTrue(url.contains("oauth-callback"));
    }

    @Test
    void alipay_missing_public_key_throws_and_fetch_supports_open_id() throws Exception {
        AlipayOAuthProvider missingPub = new AlipayOAuthProvider();
        ReflectionTestUtils.setField(missingPub, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(missingPub, "loginOAuthSecrets", secretsWith(Map.of(
                LoginOAuthSecrets.ALIPAY_APP_ID, "2021000000000000",
                LoginOAuthSecrets.ALIPAY_APP_PRIVATE_KEY, "dummy-private-key")));
        BizException ex = assertThrows(BizException.class, () -> missingPub.buildAuthorizeUrl("s1"));
        assertEquals("4403", ex.getCode());

        AlipayClient mockClient = mock(AlipayClient.class);
        AlipaySystemOauthTokenResponse tokenResp = new AlipaySystemOauthTokenResponse();
        tokenResp.setAccessToken("ali-tk-1");
        tokenResp.setOpenId("ali-open-001");
        when(mockClient.execute(any(AlipaySystemOauthTokenRequest.class))).thenReturn(tokenResp);

        AlipayUserInfoShareResponse userResp = new AlipayUserInfoShareResponse();
        userResp.setNickName("测试支付宝");
        userResp.setAvatar("https://example.com/a.png");
        when(mockClient.execute(any(AlipayUserInfoShareRequest.class), eq("ali-tk-1"))).thenReturn(userResp);

        AlipayOAuthProvider.ClientParams[] captured = new AlipayOAuthProvider.ClientParams[1];
        AlipayOAuthProvider provider = new AlipayOAuthProvider(p -> {
            captured[0] = p;
            return mockClient;
        });
        ReflectionTestUtils.setField(provider, "oauthProperties", redirectOnlyProperties());
        ReflectionTestUtils.setField(provider, "loginOAuthSecrets", secretsWith(Map.of(
                LoginOAuthSecrets.ALIPAY_APP_ID, "2021000000000000",
                LoginOAuthSecrets.ALIPAY_APP_PRIVATE_KEY, "-----BEGIN PRIVATE KEY-----\nAAA\n-----END PRIVATE KEY-----",
                LoginOAuthSecrets.ALIPAY_PUBLIC_KEY, "-----BEGIN PUBLIC KEY-----\nBBB\n-----END PUBLIC KEY-----")));

        OAuthUserInfo info = provider.fetchUserInfo("auth-code");
        assertEquals("ali-open-001", info.getOpenId());
        assertEquals("测试支付宝", info.getNickname());
        assertEquals("AAA", captured[0].appPrivateKey());
        assertEquals("BBB", captured[0].alipayPublicKey());
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
        ReflectionTestUtils.setField(provider, "idpBaseUrl", "https://chat.bgssai.cn");
        String url = provider.buildAuthorizeUrl("csrf-state");
        assertTrue(url.startsWith("https://chat.bgssai.cn/oauth/authorize?"));
        assertTrue(url.contains("client_id=bgssai-short"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("scope=profile%20email"));
        assertTrue(url.contains("oauth-callback"));
        assertTrue(url.contains("state=csrf-state"));
    }

    @Test
    void channels_require_complete_credentials_and_reject_weibo() {
        OAuthChannelsService partial = new OAuthChannelsService();
        ReflectionTestUtils.setField(partial, "secrets", secretsWith(Map.of(
                LoginOAuthSecrets.WECHAT_APP_ID, "wx1",
                LoginOAuthSecrets.WECHAT_APP_SECRET, "wxs",
                LoginOAuthSecrets.ALIPAY_APP_ID, "ali1",
                LoginOAuthSecrets.ALIPAY_APP_PRIVATE_KEY, "aliPriv")));
        assertEquals(List.of("wechat"), partial.list().getChannels());

        OAuthChannelsService full = new OAuthChannelsService();
        ReflectionTestUtils.setField(full, "secrets", secretsWith(Map.ofEntries(
                Map.entry(LoginOAuthSecrets.WECHAT_APP_ID, "wx1"),
                Map.entry(LoginOAuthSecrets.WECHAT_APP_SECRET, "wxs"),
                Map.entry(LoginOAuthSecrets.ALIPAY_APP_ID, "ali1"),
                Map.entry(LoginOAuthSecrets.ALIPAY_APP_PRIVATE_KEY, "aliPriv"),
                Map.entry(LoginOAuthSecrets.ALIPAY_PUBLIC_KEY, "aliPub"),
                Map.entry(LoginOAuthSecrets.DOUYIN_CLIENT_KEY, "dy1"),
                Map.entry(LoginOAuthSecrets.DOUYIN_CLIENT_SECRET, "dys"),
                Map.entry(LoginOAuthSecrets.BAIDU_CLIENT_ID, "bd1"),
                Map.entry(LoginOAuthSecrets.BAIDU_CLIENT_SECRET, "bds"),
                Map.entry(LoginOAuthSecrets.BGSSAI_CLIENT_ID, "bg1"),
                Map.entry(LoginOAuthSecrets.BGSSAI_CLIENT_SECRET, "bgs"))));
        List<String> channels = full.list().getChannels();
        assertEquals(List.of("wechat", "alipay", "douyin", "baidu", "bgssai"), channels);
        assertFalse(channels.contains("weibo"));

        OAuthProviderFactory factory = new OAuthProviderFactory(List.of(
                new WechatOAuthProvider(), new AlipayOAuthProvider(),
                new DouyinOAuthProvider(), new BaiduOAuthProvider(), new BgssaiOAuthProvider()));
        BizException ex = assertThrows(BizException.class, () -> factory.get("weibo"));
        assertEquals("4402", ex.getCode());
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

