package com.bgssai.shortai.secret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.ForbiddenException;

class SysSecretAdminServiceTest {

    private static final class SecretRow {
        String key;
        String value;
        String description;
        int enabled;
        Timestamp updatedAt = Timestamp.from(Instant.now());
        String updatedBy = "system";

        SecretRow(String key, String value, String description, int enabled) {
            this.key = key;
            this.value = value;
            this.description = description;
            this.enabled = enabled;
        }
    }

    private final Map<String, SecretRow> db = new LinkedHashMap<>();
    private SysSecretAdminService service;

    @BeforeEach
    void setup() throws Exception {
        UserContext.setUserRole(AdminAuthGuard.ADMIN_ROLE);
        UserContext.setUserId("u-admin-1");
        seed("bgssai.sms.secret-id", "AKID1234567890ABCD", "SecretId", 1);
        seed("bgssai.sms.secret-key", "KEY1234567890WXYZ", "SecretKey", 1);
        seed("bgssai.sms.sdk-app-id", "1401156379", "SdkAppId", 1);
        seed("bgssai.sms.sign-name", "昆山兵贵神速智能科技", "SignName", 1);
        seed("bgssai.sms.template-id", "2677885", "TemplateId", 1);
        seed("sms.endpoint", "sms.tencentcloudapi.com", "Endpoint", 1);
        seed("sms.region", "ap-guangzhou", "Region", 1);
        seed("sms.product_label", "Short", "ProductLabel", 1);
        seed("sms.template_param_count", "2", "ParamCount", 1);
        seed("sms.intl.enabled", "0", "IntlEnabled", 1);
        seed("sms.intl.secret_id", "", "IntlSecretId", 1);
        seed("sms.intl.secret_key", "", "IntlSecretKey", 1);
        seed("sms.intl.endpoint", "sms.tencentcloudapi.com", "IntlEndpoint", 1);
        seed("sms.intl.region", "ap-guangzhou", "IntlRegion", 1);
        seed("sms.intl.sdk_app_id", "", "IntlSdkAppId", 1);
        seed("sms.intl.sign_name", "", "IntlSignName", 1);
        seed("sms.intl.sender_id", "", "IntlSenderId", 1);
        seed("sms.intl.template_id", "", "IntlTemplateId", 1);
        seed("sms.intl.template_param_count", "2", "IntlParamCount", 1);
        seed("sms.intl.product_label", "Short", "IntlProductLabel", 1);
        service = new SysSecretAdminService(inMemoryDataSource());
    }

    @AfterEach
    void teardown() {
        UserContext.clear();
    }

    private void seed(String key, String value, String desc, int enabled) {
        db.put(key, new SecretRow(key, value, desc, enabled));
    }

    private DataSource inMemoryDataSource() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenAnswer(inv -> {
            Connection conn = mock(Connection.class);
            when(conn.prepareStatement(anyString())).thenAnswer(psInv -> prepare(psInv.getArgument(0)));
            return conn;
        });
        return ds;
    }

    private PreparedStatement prepare(String sql) throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        Object[] params = new Object[8];
        org.mockito.Mockito.doAnswer(inv -> {
            params[(int) inv.getArgument(0) - 1] = inv.getArgument(1);
            return null;
        }).when(ps).setString(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.nullable(String.class));
        org.mockito.Mockito.doAnswer(inv -> {
            params[(int) inv.getArgument(0) - 1] = inv.getArgument(1);
            return null;
        }).when(ps).setInt(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());

        when(ps.executeQuery()).thenAnswer(inv -> {
            ResultSet rs = mock(ResultSet.class);
            if (sql.contains("SECRET_KEY LIKE") || sql.contains("secret_key LIKE")) {
                String pattern = String.valueOf(params[0]);
                String prefix = pattern.endsWith("%") ? pattern.substring(0, pattern.length() - 1) : pattern;
                List<SecretRow> rows = new ArrayList<>();
                for (SecretRow row : db.values()) {
                    if (row.key.startsWith(prefix)) rows.add(row);
                }
                rows.sort(Comparator.comparing(r -> r.key));
                int[] idx = { -1 };
                when(rs.next()).thenAnswer(i -> ++idx[0] < rows.size());
                when(rs.getString(anyString())).thenAnswer(i -> {
                    SecretRow cur = rows.get(idx[0]);
                    return switch ((String) i.getArgument(0)) {
                        case "secret_key" -> cur.key;
                        case "secret_value" -> cur.value;
                        case "description" -> cur.description;
                        case "updated_by" -> cur.updatedBy;
                        default -> null;
                    };
                });
                when(rs.getInt("enabled")).thenAnswer(i -> rows.get(idx[0]).enabled);
                when(rs.getTimestamp("updated_at")).thenAnswer(i -> rows.get(idx[0]).updatedAt);
                return rs;
            }
            if (sql.contains("SELECT 1 FROM sys_secret")) {
                boolean found = db.containsKey(String.valueOf(params[0]));
                int[] count = { 0 };
                when(rs.next()).thenAnswer(i -> count[0]++ == 0 && found);
                return rs;
            }
            String key = String.valueOf(params[0]);
            SecretRow row = db.get(key);
            boolean hit = row != null && row.enabled == 1;
            int[] count = { 0 };
            when(rs.next()).thenAnswer(i -> count[0]++ == 0 && hit);
            when(rs.getString(1)).thenAnswer(i -> row == null ? null : row.value);
            return rs;
        });

        when(ps.executeUpdate()).thenAnswer(inv -> {
            if (sql.startsWith("INSERT INTO")) {
                String k = (String) params[0];
                seed(k, (String) params[1], (String) params[2], (int) params[3]);
            } else if (sql.contains("SET secret_value")) {
                db.get((String) params[2]).value = (String) params[0];
            } else if (sql.contains("SET enabled")) {
                db.get((String) params[2]).enabled = (int) params[0];
            } else if (sql.contains("SET description")) {
                db.get((String) params[2]).description = (String) params[0];
            }
            return 1;
        });
        return ps;
    }

    @Test
    @SuppressWarnings("unchecked")
    void listMasksSensitiveKeysWhileShowingNonSensitiveSmsValues() {
        ApiResponse domResp = service.list("bgssai.sms.");
        List<Map<String, Object>> domList = (List<Map<String, Object>>) ((Map<String, Object>) domResp.getResult()).get("secret_list");
        Map<String, Object> secretIdRow = domList.stream().filter(m -> "bgssai.sms.secret-id".equals(m.get("secret_key"))).findFirst().orElseThrow();
        assertEquals(true, secretIdRow.get("is_sensitive"));
        assertNull(secretIdRow.get("config_value"));
        assertEquals("AKID****ABCD", secretIdRow.get("secret_value_masked"));

        Map<String, Object> appIdRow = domList.stream().filter(m -> "bgssai.sms.sdk-app-id".equals(m.get("secret_key"))).findFirst().orElseThrow();
        assertEquals(false, appIdRow.get("is_sensitive"));
        assertEquals("1401156379", appIdRow.get("config_value"));
    }

    @Test
    void rejectsUnauthorizedPrefixAndInvalidSmsValues() {
        UserContext.setUserRole("USER");
        assertThrows(ForbiddenException.class, () -> service.list("sms."));

        UserContext.setUserRole(AdminAuthGuard.ADMIN_ROLE);
        assertEquals("4006", assertThrows(BizException.class, () -> service.update("other.key", "v", 1, null)).getCode());
        assertEquals("50000001", assertThrows(BizException.class, () -> service.update("sms.endpoint", "evil.example.com", 1, null)).getCode());
        assertEquals("50000001", assertThrows(BizException.class, () -> service.update("sms.region", "ap-singapore", 1, null)).getCode());
        assertEquals("50000001", assertThrows(BizException.class, () -> service.update("sms.template_param_count", "5", 1, null)).getCode());
        assertEquals("50000001", assertThrows(BizException.class, () -> service.update("sms.intl.enabled", "maybe", 1, null)).getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testsDomesticAndInternationalChannelReadinessWithoutFallback() {
        Map<String, Object> dom = (Map<String, Object>) service.testConnection("sms.domestic").getResult();
        assertEquals(true, dom.get("ok"));

        Map<String, Object> intlDisabled = (Map<String, Object>) service.testConnection("sms.intl").getResult();
        assertEquals(false, intlDisabled.get("ok"));

        service.update("sms.intl.enabled", "true", 1, null);
        service.update("sms.intl.endpoint", "sms.intl.tencentcloudapi.com", 1, null);
        service.update("sms.intl.region", "ap-singapore", 1, null);
        service.update("sms.intl.sdk_app_id", "1402000001", 1, null);
        service.update("sms.intl.template_id", "3000001", 1, null);

        // Optional sign_name left blank must still succeed
        Map<String, Object> intlReady = (Map<String, Object>) service.testConnection("sms.intl").getResult();
        assertEquals(true, intlReady.get("ok"));
        assertTrue(String.valueOf(intlReady.get("message")).contains("sms.intl.tencentcloudapi.com"));

        // Mismatched endpoint/region fails closed
        service.update("sms.intl.region", "ap-guangzhou", 1, null);
        Map<String, Object> badCombo = (Map<String, Object>) service.testConnection("sms.intl").getResult();
        assertEquals(false, badCombo.get("ok"));
    }
}
