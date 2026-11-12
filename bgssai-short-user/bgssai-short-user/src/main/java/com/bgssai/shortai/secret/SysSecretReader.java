package com.bgssai.shortai.secret;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reads enabled rows from sys_secret. Empty string when missing or disabled.
 */
@Component
public class SysSecretReader {

    public static final String AI_OPENAI_COMPAT_API_KEY = "ai.openai_compat.api_key";

    /** @deprecated Prefer {@link #AI_TOKENHUB_CN_API_KEY}; kept for empty-fill migration. */
    public static final String AI_TENCENT_TOKENHUB_API_KEY = "ai.tencent_tokenhub.api_key";

    public static final String AI_TOKENHUB_CN_API_KEY = "ai.tokenhub_cn.api_key";

    public static final String AI_TOKENHUB_GLOBAL_API_KEY = "ai.tokenhub_global.api_key";

    public static final String AI_KLING_API_KEY = "ai.kling.api_key";

    public static final String AI_MINIMAX_API_KEY = "ai.minimax.api_key";

    public static final String AI_SEEDANCE_API_KEY = "ai.seedance.api_key";

    public static final String AI_VEO_API_KEY = "ai.veo.api_key";

    /** Empty until an approved image-to-video route exists. Not a fallback for other keys. */
    public static final String AI_IMAGE_TO_VIDEO_API_KEY = "ai.image_to_video.api_key";

    private final Function<String, String> lookup;

    @Autowired
    public SysSecretReader(javax.sql.DataSource dataSource) {
        this.lookup = key -> read(dataSource, key);
    }

    public SysSecretReader(Function<String, String> lookup) {
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
