package com.bgssai.shortai.secret;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reads enabled rows from sys_secret. Empty string when missing or disabled.
 */
@Component
public class SysSecretReader {

    public static final String OBS_ACCESS_KEY = "obs.access_key";
    public static final String OBS_SECRET_KEY = "obs.secret_key";
    public static final String AI_OPENAI_COMPAT_API_KEY = "ai.openai_compat.api_key";

    private final Function<String, String> lookup;

    @Autowired
    public SysSecretReader(javax.sql.DataSource dataSource) {
        this.lookup = key -> read(dataSource, key);
    }

    SysSecretReader(Function<String, String> lookup) {
        this.lookup = lookup;
    }

    public String get(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            return "";
        }
        String raw = lookup.apply(secretKey.trim());
        return raw == null ? "" : raw.trim();
    }

    public boolean has(String secretKey) {
        return !get(secretKey).isEmpty();
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
}
