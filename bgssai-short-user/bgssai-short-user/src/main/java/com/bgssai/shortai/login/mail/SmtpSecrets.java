package com.bgssai.shortai.login.mail;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SmtpSecrets {

    public static final String HOST = "bgssai.smtp.host";
    public static final String PORT = "bgssai.smtp.port";
    public static final String USERNAME = "bgssai.smtp.username";
    public static final String PASSWORD = "bgssai.smtp.password";
    public static final String FROM_ADDRESS = "bgssai.smtp.from-address";
    public static final String SSL_ENABLE = "bgssai.smtp.ssl-enable";
    public static final String STARTTLS_ENABLE = "bgssai.smtp.starttls-enable";

    private final Function<String, String> lookup;

    @Autowired
    public SmtpSecrets(javax.sql.DataSource dataSource) {
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

    public String host() { return value(HOST); }
    public String port() { return value(PORT); }
    public String username() { return value(USERNAME); }
    public String password() { return value(PASSWORD); }
    public String fromAddress() { return value(FROM_ADDRESS); }
    public boolean sslEnable() { return "1".equals(value(SSL_ENABLE)) || "true".equalsIgnoreCase(value(SSL_ENABLE)); }
    public boolean starttlsEnable() { return "1".equals(value(STARTTLS_ENABLE)) || "true".equalsIgnoreCase(value(STARTTLS_ENABLE)); }

    private String value(String secretKey) {
        String raw = lookup.apply(secretKey);
        return raw == null ? "" : raw.trim();
    }
}
