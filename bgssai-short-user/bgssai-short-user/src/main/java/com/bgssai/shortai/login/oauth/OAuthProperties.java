package com.bgssai.shortai.login.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FR-LOGIN-EXT 三方授权登录的环境项（{@code bgssai.oauth.*}）。
 * 只保留各环境 {@code redirect-uri}（基础设施，Standards §1 允许写 profile）。
 * AppID / Secret / 私钥一律走 {@link LoginOAuthSecrets}，不写 properties。
 */
@Component
@ConfigurationProperties(prefix = "bgssai.oauth")
public class OAuthProperties {

    private Wechat wechat = new Wechat();
    private Alipay alipay = new Alipay();
    private Douyin douyin = new Douyin();
    private Bgssai bgssai = new Bgssai();

    public Wechat getWechat() { return wechat; }
    public void setWechat(Wechat wechat) { this.wechat = wechat; }
    public Alipay getAlipay() { return alipay; }
    public void setAlipay(Alipay alipay) { this.alipay = alipay; }
    public Douyin getDouyin() { return douyin; }
    public void setDouyin(Douyin douyin) { this.douyin = douyin; }
    public Bgssai getBgssai() { return bgssai; }
    public void setBgssai(Bgssai bgssai) { this.bgssai = bgssai; }

    public static class Wechat {
        private String redirectUri;

        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class Alipay {
        private String redirectUri;

        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class Douyin {
        private String redirectUri;

        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class Bgssai {
        private String redirectUri;

        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }
}
