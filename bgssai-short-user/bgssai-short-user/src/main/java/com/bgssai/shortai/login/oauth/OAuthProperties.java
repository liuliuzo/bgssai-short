package com.bgssai.shortai.login.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FR-LOGIN-EXT 三方授权登录配置（bgssai.oauth.*）。三个平台的凭据按 Standards §1 以最终字面量
 * 写入各 profile，未开通环境留空；mock 关闭且凭据为空时该登录方式 fail-closed 返回 4403。
 */
@Component
@ConfigurationProperties(prefix = "bgssai.oauth")
public class OAuthProperties {

    private boolean mockEnabled;
    private Wechat wechat = new Wechat();
    private Alipay alipay = new Alipay();
    private Douyin douyin = new Douyin();

    public boolean isMockEnabled() { return mockEnabled; }
    public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
    public Wechat getWechat() { return wechat; }
    public void setWechat(Wechat wechat) { this.wechat = wechat; }
    public Alipay getAlipay() { return alipay; }
    public void setAlipay(Alipay alipay) { this.alipay = alipay; }
    public Douyin getDouyin() { return douyin; }
    public void setDouyin(Douyin douyin) { this.douyin = douyin; }

    public static class Wechat {
        private String appId;
        private String appSecret;
        private String redirectUri;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class Alipay {
        private String appId;
        private String appPrivateKey;
        private String alipayPublicKey;
        private String redirectUri;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAppPrivateKey() { return appPrivateKey; }
        public void setAppPrivateKey(String appPrivateKey) { this.appPrivateKey = appPrivateKey; }
        public String getAlipayPublicKey() { return alipayPublicKey; }
        public void setAlipayPublicKey(String alipayPublicKey) { this.alipayPublicKey = alipayPublicKey; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class Douyin {
        private String clientKey;
        private String clientSecret;
        private String redirectUri;

        public String getClientKey() { return clientKey; }
        public void setClientKey(String clientKey) { this.clientKey = clientKey; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }
}
