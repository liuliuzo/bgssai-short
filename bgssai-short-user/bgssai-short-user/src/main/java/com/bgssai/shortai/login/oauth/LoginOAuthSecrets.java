package com.bgssai.shortai.login.oauth;

import java.util.function.Function;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 登录页微信 / 支付宝 / 抖音 / 百度 / BGSSAI 官网凭证。只读库（DML STEP LOGIN-OAUTH-1 种子 + 控制台可改），
 * 不读 application-*.properties（Standards §1）。缺失时返回空串，由 Provider 转「该登录方式暂不可用」。
 */
@Component
public class LoginOAuthSecrets {

    public static final String WECHAT_APP_ID = "bgssai.oauth.wechat.app-id";
    public static final String WECHAT_APP_SECRET = "bgssai.oauth.wechat.app-secret";
    public static final String ALIPAY_APP_ID = "bgssai.oauth.alipay.app-id";
    public static final String ALIPAY_APP_PRIVATE_KEY = "bgssai.oauth.alipay.app-private-key";
    public static final String ALIPAY_PUBLIC_KEY = "bgssai.oauth.alipay.alipay-public-key";
    public static final String DOUYIN_CLIENT_KEY = "bgssai.oauth.douyin.client-key";
    public static final String DOUYIN_CLIENT_SECRET = "bgssai.oauth.douyin.client-secret";
    public static final String BAIDU_CLIENT_ID = "bgssai.oauth.baidu.client-id";
    public static final String BAIDU_CLIENT_SECRET = "bgssai.oauth.baidu.client-secret";
    public static final String BGSSAI_CLIENT_ID = "bgssai.oauth.bgssai.client-id";
    public static final String BGSSAI_CLIENT_SECRET = "bgssai.oauth.bgssai.client-secret";

    private final Function<String, String> lookup;

    @Autowired
    public LoginOAuthSecrets(javax.sql.DataSource dataSource) {
        this.lookup = key -> read(dataSource, key);
    }

    private static String read(javax.sql.DataSource dataSource, String key) {
        String sql = "SELECT secret_value FROM sys_secret WHERE secret_key = ? AND enabled = 1";
        try (java.sql.Connection c = dataSource.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String v = rs.getString(1);
                    return v == null ? "" : v;
                }
            }
        } catch (java.sql.SQLException ignored) {
            return "";
        }
        return "";
    }

    LoginOAuthSecrets(Function<String, String> lookup) {
        this.lookup = lookup;
    }

    public String wechatAppId() {
        return value(WECHAT_APP_ID);
    }

    public String wechatAppSecret() {
        return value(WECHAT_APP_SECRET);
    }

    public String alipayAppId() {
        return value(ALIPAY_APP_ID);
    }

    public String alipayAppPrivateKey() {
        return value(ALIPAY_APP_PRIVATE_KEY);
    }

    public String alipayPublicKey() {
        return value(ALIPAY_PUBLIC_KEY);
    }

    public String douyinClientKey() {
        return value(DOUYIN_CLIENT_KEY);
    }

    public String douyinClientSecret() {
        return value(DOUYIN_CLIENT_SECRET);
    }

    public String baiduClientId() {
        return value(BAIDU_CLIENT_ID);
    }

    public String baiduClientSecret() {
        return value(BAIDU_CLIENT_SECRET);
    }

    public String bgssaiClientId() {
        return value(BGSSAI_CLIENT_ID);
    }

    public String bgssaiClientSecret() {
        return value(BGSSAI_CLIENT_SECRET);
    }

    private String value(String secretKey) {
        String raw = lookup.apply(secretKey);
        return raw == null ? "" : raw.trim();
    }
}

