package com.bgssai.shortai.login.sms;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SmsSecrets {

    public static final String SECRET_ID = "bgssai.sms.secret-id";
    public static final String SECRET_KEY = "bgssai.sms.secret-key";
    public static final String SDK_APP_ID = "bgssai.sms.sdk-app-id";
    public static final String SIGN_NAME = "bgssai.sms.sign-name";
    public static final String TEMPLATE_ID = "bgssai.sms.template-id";

    private final Function<String, String> lookup;

    @Autowired
    public SmsSecrets(javax.sql.DataSource dataSource) {
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

    public String secretId() { return value(SECRET_ID); }
    public String secretKey() { return value(SECRET_KEY); }
    public String sdkAppId() { return value(SDK_APP_ID); }
    public String signName() { return value(SIGN_NAME); }
    public String templateId() { return value(TEMPLATE_ID); }

    private String value(String secretKey) {
        String raw = lookup.apply(secretKey);
        return raw == null ? "" : raw.trim();
    }
}
