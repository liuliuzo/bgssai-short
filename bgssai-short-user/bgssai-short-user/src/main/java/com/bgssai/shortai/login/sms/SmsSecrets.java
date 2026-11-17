package com.bgssai.shortai.login.sms;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 腾讯云短信国内（+86）与国际/港澳台（非 +86 E.164）配置读取（sys_secret）。
 */
@Component
public class SmsSecrets {

    public static final String SECRET_ID = "bgssai.sms.secret-id";
    public static final String SECRET_KEY = "bgssai.sms.secret-key";
    public static final String SDK_APP_ID = "bgssai.sms.sdk-app-id";
    public static final String SIGN_NAME = "bgssai.sms.sign-name";
    public static final String TEMPLATE_ID = "bgssai.sms.template-id";

    public static final String DOMESTIC_ENDPOINT = "sms.endpoint";
    public static final String DOMESTIC_REGION = "sms.region";
    public static final String DOMESTIC_PRODUCT_LABEL = "sms.product_label";
    public static final String DOMESTIC_TEMPLATE_PARAM_COUNT = "sms.template_param_count";

    public static final String INTL_ENABLED = "sms.intl.enabled";
    public static final String INTL_SECRET_ID = "sms.intl.secret_id";
    public static final String INTL_SECRET_KEY = "sms.intl.secret_key";
    public static final String INTL_ENDPOINT = "sms.intl.endpoint";
    public static final String INTL_REGION = "sms.intl.region";
    public static final String INTL_SDK_APP_ID = "sms.intl.sdk_app_id";
    public static final String INTL_SIGN_NAME = "sms.intl.sign_name";
    public static final String INTL_SENDER_ID = "sms.intl.sender_id";
    public static final String INTL_TEMPLATE_ID = "sms.intl.template_id";
    public static final String INTL_TEMPLATE_PARAM_COUNT = "sms.intl.template_param_count";
    public static final String INTL_PRODUCT_LABEL = "sms.intl.product_label";

    public static final String DEFAULT_ENDPOINT = "sms.tencentcloudapi.com";
    public static final String DEFAULT_REGION = "ap-guangzhou";

    private final Function<String, String> lookup;

    @Autowired
    public SmsSecrets(javax.sql.DataSource dataSource) {
        this.lookup = key -> read(dataSource, key);
    }

    public SmsSecrets(Function<String, String> lookup) {
        this.lookup = lookup;
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

    public String secretId() { return firstNonBlank(value(SECRET_ID), value("sms.secret_id")); }
    public String secretKey() { return firstNonBlank(value(SECRET_KEY), value("sms.secret_key")); }
    public String sdkAppId() { return firstNonBlank(value(SDK_APP_ID), value("sms.sdk_app_id")); }
    public String signName() { return firstNonBlank(value(SIGN_NAME), value("sms.sign_name")); }
    public String templateId() { return firstNonBlank(value(TEMPLATE_ID), value("sms.template_id")); }

    public String endpoint() { return firstNonBlank(value(DOMESTIC_ENDPOINT), DEFAULT_ENDPOINT); }
    public String region() { return firstNonBlank(value(DOMESTIC_REGION), DEFAULT_REGION); }
    public String productLabel() { return value(DOMESTIC_PRODUCT_LABEL); }
    public String templateParamCount() { return value(DOMESTIC_TEMPLATE_PARAM_COUNT); }

    public boolean intlEnabled() {
        String raw = value(INTL_ENABLED);
        return "1".equals(raw) || "true".equalsIgnoreCase(raw);
    }

    public String intlSecretId() { return value(INTL_SECRET_ID); }
    public String intlSecretKey() { return value(INTL_SECRET_KEY); }
    public String intlEndpoint() { return firstNonBlank(value(INTL_ENDPOINT), DEFAULT_ENDPOINT); }
    public String intlRegion() { return firstNonBlank(value(INTL_REGION), DEFAULT_REGION); }
    public String intlSdkAppId() { return value(INTL_SDK_APP_ID); }
    public String intlSignName() { return value(INTL_SIGN_NAME); }
    public String intlSenderId() { return value(INTL_SENDER_ID); }
    public String intlTemplateId() { return value(INTL_TEMPLATE_ID); }
    public String intlProductLabel() { return value(INTL_PRODUCT_LABEL); }
    public String intlTemplateParamCount() { return value(INTL_TEMPLATE_PARAM_COUNT); }

    private String value(String secretKey) {
        String raw = lookup.apply(secretKey);
        return raw == null ? "" : raw.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }
}
