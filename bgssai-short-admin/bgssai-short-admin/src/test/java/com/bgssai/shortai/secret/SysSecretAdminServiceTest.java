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
import com.bgssai.shortai.secret.ui.UpdateSysSecretController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SysSecretAdminServiceTest {

    private static final class SecretRow {
        String key;
        String value;
        String keyName;
        String platformKeyName;
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
                        case "key_name" -> cur.keyName;
                        case "platform_key_name" -> cur.platformKeyName;
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
            if (sql.contains("SELECT secret_value, key_name, platform_key_name")) {
                SecretRow row = db.get(String.valueOf(params[0]));
                int[] count = { 0 };
                when(rs.next()).thenAnswer(i -> count[0]++ == 0 && row != null);
                when(rs.getString(anyString())).thenAnswer(i -> {
                    if (row == null) return null;
                    return switch ((String) i.getArgument(0)) {
                        case "secret_value" -> row.value;
                        case "key_name" -> row.keyName;
                        case "platform_key_name" -> row.platformKeyName;
                        default -> null;
                    };
                });
                when(rs.getInt("enabled")).thenAnswer(i -> row == null ? 0 : row.enabled);
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
                SecretRow row = new SecretRow(k, (String) params[1], (String) params[4], (int) params[5]);
                row.keyName = (String) params[2];
                row.platformKeyName = (String) params[3];
                db.put(k, row);
            } else if (sql.contains("SET secret_value")) {
                db.get((String) params[2]).value = (String) params[0];
            } else if (sql.contains("SET key_name")) {
                db.get((String) params[2]).keyName = (String) params[0];
            } else if (sql.contains("SET platform_key_name")) {
                db.get((String) params[2]).platformKeyName = (String) params[0];
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

    @Test
    @SuppressWarnings("unchecked")
    void probesPreservesAndRefreshesKeyNameAndPlatformKeyNameAcrossLifecycle() throws Exception {
        PlatformKeyNameProber prober = mock(PlatformKeyNameProber.class);
        when(prober.probe("ai.tokenhub_cn.api_key", "sk-th-live-12345678")).thenReturn("cn-primary-key");
        SysSecretAdminService aiService = new SysSecretAdminService(inMemoryDataSource(), prober);

        ObjectMapper snakeMapper = new ObjectMapper();
        snakeMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        UpdateSysSecretController controller = new UpdateSysSecretController();
        ReflectionTestUtils.setField(controller, "service", aiService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(snakeMapper))
                .build();

        mvc.perform(MockMvcRequestBuilders.post("/bgssai/admin/secrets/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret_key\":\"ai.tokenhub_cn.api_key\",\"secret_value\":\"sk-th-live-12345678\",\"key_name\":\"  Prod Gateway  \",\"enabled\":1}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.key_name").value("Prod Gateway"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.platform_key_name").value("cn-primary-key"));

        // Omitting secret_value, passing blank or masked secret_value, and toggling must preserve platform_key_name and key_name
        aiService.update("ai.tokenhub_cn.api_key", null, 0, "updated desc", null);
        aiService.update("ai.tokenhub_cn.api_key", "   ", 1, null, null);
        aiService.update("ai.tokenhub_cn.api_key", "sk-t****5678", 1, null, null);
        assertEquals("sk-th-live-12345678", db.get("ai.tokenhub_cn.api_key").value);
        assertEquals("cn-primary-key", db.get("ai.tokenhub_cn.api_key").platformKeyName);
        assertEquals("Prod Gateway", db.get("ai.tokenhub_cn.api_key").keyName);

        // Blank key_name clears key_name, while testConnection refreshes platform_key_name
        aiService.update("ai.tokenhub_cn.api_key", null, null, null, "   ");
        assertNull(db.get("ai.tokenhub_cn.api_key").keyName);

        when(prober.probe("ai.tokenhub_cn.api_key", "sk-th-live-12345678")).thenReturn("cn-refreshed-key");
        Map<String, Object> tested = (Map<String, Object>) aiService.testConnection("ai.tokenhub_cn.api_key").getResult();
        assertEquals(true, tested.get("ok"));
        assertEquals("cn-refreshed-key", tested.get("platform_key_name"));
        assertEquals("cn-refreshed-key", db.get("ai.tokenhub_cn.api_key").platformKeyName);

        // Exceeding 64 chars rejects cleanly
        String over64 = "a".repeat(65);
        assertEquals("50000001", assertThrows(BizException.class,
                () -> aiService.update("ai.tokenhub_cn.api_key", null, null, null, over64)).getCode());
    }
}
