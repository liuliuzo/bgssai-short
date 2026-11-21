package com.bgssai.shortai.secret;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;

/**
 * Admin CRUD for business third-party secrets and SMS channel configs in sys_secret.
 */
@Service
public class SysSecretAdminService {

    private static final String DOMESTIC_ENDPOINT = "sms.tencentcloudapi.com";
    private static final String INTL_ENDPOINT = "sms.intl.tencentcloudapi.com";
    private static final Set<String> VALID_ENDPOINTS = Set.of(DOMESTIC_ENDPOINT, INTL_ENDPOINT);
    private static final Set<String> DOMESTIC_REGIONS = Set.of("ap-guangzhou", "ap-beijing", "ap-nanjing");
    private static final Set<String> INTL_REGIONS = Set.of("ap-singapore", "eu-frankfurt");
    private static final Set<String> ALL_VALID_REGIONS = Set.of(
            "ap-guangzhou", "ap-beijing", "ap-nanjing", "ap-singapore", "eu-frankfurt");
    private static final Set<String> NON_SENSITIVE_SMS_KEYS = Set.of(
            "bgssai.sms.sdk-app-id",
            "bgssai.sms.sign-name",
            "bgssai.sms.template-id",
            "sms.endpoint",
            "sms.region",
            "sms.product_label",
            "sms.template_param_count",
            "sms.intl.enabled",
            "sms.intl.endpoint",
            "sms.intl.region",
            "sms.intl.sdk_app_id",
            "sms.intl.sign_name",
            "sms.intl.sender_id",
            "sms.intl.template_id",
            "sms.intl.template_param_count",
            "sms.intl.product_label");

    private static final int MAX_KEY_NAME_LENGTH = 64;

    private record SecretMeta(String value, String keyName, String platformKeyName, int enabled) {
    }

    private final DataSource dataSource;
    private final PlatformKeyNameProber platformKeyNameProber;

    public SysSecretAdminService(DataSource dataSource) {
        this(dataSource, new PlatformKeyNameProber(null));
    }

    @Autowired
    public SysSecretAdminService(DataSource dataSource, PlatformKeyNameProber platformKeyNameProber) {
        this.dataSource = dataSource;
        this.platformKeyNameProber = platformKeyNameProber == null
                ? new PlatformKeyNameProber(null)
                : platformKeyNameProber;
    }

    public ApiResponse list(String keyPrefix) {
        requireAdmin();
        String prefix = keyPrefix == null || keyPrefix.isBlank() ? "ai." : keyPrefix.trim();
        if (!isAllowedPrefix(prefix)) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "unsupported secret key_prefix");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT secret_key, secret_value, key_name, platform_key_name, description, enabled, updated_at, updated_by "
                + "FROM sys_secret WHERE secret_key LIKE ? ORDER BY secret_key ASC";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("secret_key");
                    String value = rs.getString("secret_value");
                    boolean sensitive = isSensitiveKey(key);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("secret_key", key);
                    item.put("key_name", trimToNull(rs.getString("key_name")));
                    item.put("platform_key_name", trimToNull(rs.getString("platform_key_name")));
                    item.put("description", rs.getString("description"));
                    item.put("enabled", rs.getInt("enabled"));
                    item.put("configured_flag", value != null && !value.isBlank() ? 1 : 0);
                    item.put("is_sensitive", sensitive);
                    item.put("secret_value_masked", sensitive ? mask(value) : (value == null ? "" : value.trim()));
                    item.put("config_value", sensitive ? null : (value == null ? "" : value.trim()));
                    item.put("updated_at", rs.getTimestamp("updated_at"));
                    item.put("updated_by", rs.getString("updated_by"));
                    list.add(item);
                }
            }
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "list sys_secret failed");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("secret_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse update(String secretKey, String secretValue, Integer enabled, String description) {
        return update(secretKey, secretValue, enabled, description, null);
    }

    @Transactional
    public ApiResponse update(String secretKey, String secretValue, Integer enabled, String description, String keyName) {
        requireAdmin();
        if (secretKey == null || secretKey.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "secret_key required");
        }
        String key = secretKey.trim();
        if (!isAllowedPrefix(key)) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "admin secrets page does not edit key: " + key);
        }
        boolean sensitive = isSensitiveKey(key);
        boolean blankOrMasked = secretValue == null || secretValue.isBlank() || isMaskedSecret(secretValue);
        boolean allowSensitiveClear = sensitive && key.startsWith("sms.intl.") && secretValue != null && secretValue.isBlank();
        boolean hasNewValue = secretValue != null && (!sensitive || !blankOrMasked || allowSensitiveClear);
        String normalizedValue = hasNewValue ? normalizeAndValidateSecretValue(key, secretValue) : null;
        boolean hasNewNonMaskedValue = normalizedValue != null && !normalizedValue.isBlank() && !isMaskedSecret(normalizedValue);

        boolean keyNameSubmitted = keyName != null;
        String normalizedKeyName = keyNameSubmitted ? normalizeAndValidateKeyName(keyName) : null;
        Integer normalizedEnabled = enabled == null ? null : (enabled == 1 ? 1 : 0);
        String admin = UserContext.getUserId() == null ? "admin" : UserContext.getUserId();

        boolean exists = exists(key);
        if (!exists) {
            String initialValue = normalizedValue == null ? "" : normalizedValue;
            String initialPlatformKeyName = hasNewNonMaskedValue
                    ? platformKeyNameProber.probe(key, initialValue)
                    : null;
            insert(key, initialValue, normalizedKeyName, initialPlatformKeyName,
                    description == null ? key : description,
                    normalizedEnabled == null ? 1 : normalizedEnabled, admin);
        } else {
            if (hasNewValue) {
                updateValue(key, normalizedValue, admin);
            }
            if (hasNewNonMaskedValue) {
                String probedPlatformKeyName = platformKeyNameProber.probe(key, normalizedValue);
                updatePlatformKeyName(key, probedPlatformKeyName, admin);
            }
            if (keyNameSubmitted) {
                updateKeyName(key, normalizedKeyName, admin);
            }
            if (normalizedEnabled != null) {
                updateEnabled(key, normalizedEnabled, admin);
            }
            if (description != null) {
                updateDescription(key, description, admin);
            }
        }
        SecretMeta meta = readMeta(key);
        boolean configured = meta.enabled() == 1 && meta.value() != null && !meta.value().isBlank();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("secret_key", key);
        result.put("key_name", meta.keyName());
        result.put("platform_key_name", meta.platformKeyName());
        result.put("configured_flag", configured ? 1 : 0);
        return Responses.ok(result);
    }

    public ApiResponse testConnection(String secretKey) {
        requireAdmin();
        if (secretKey == null || secretKey.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "secret_key required");
        }
        String key = secretKey.trim();
        if ("sms.domestic".equals(key)) {
            return testDomesticSmsChannel(key);
        }
        if ("sms.intl".equals(key)) {
            return testIntlSmsChannel(key);
        }
        if (!isAllowedPrefix(key)) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "unsupported secret_key");
        }
        if ("sms.intl.sign_name".equals(key) || "sms.intl.sender_id".equals(key)) {
            String value = read(key);
            boolean configured = !value.isBlank();
            return testResult(key, true, configured ? 1 : 0, configured ? "已配置可选参数" : "可选参数（留空不影响国际通道发送）");
        }
        if ("sms.intl.secret_id".equals(key) || "sms.intl.secret_key".equals(key)) {
            String intlId = read("sms.intl.secret_id");
            String intlKey = read("sms.intl.secret_key");
            if (intlId.isBlank() && intlKey.isBlank()) {
                String fallbackKey = "sms.intl.secret_id".equals(key) ? "bgssai.sms.secret-id" : "bgssai.sms.secret-key";
                boolean domOk = !read(fallbackKey).isBlank();
                return testResult(key, domOk, 0, domOk ? "未单独覆盖，将复用国内密钥" : "国际未设置且国内密钥缺失");
            }
        }
        String value = read(key);
        boolean ok = value != null && !value.isBlank();
        String platformKeyName = null;
        if (ok) {
            String probed = platformKeyNameProber.probe(key, value);
            if (probed != null) {
                String admin = UserContext.getUserId() == null ? "admin" : UserContext.getUserId();
                updatePlatformKeyName(key, probed, admin);
                platformKeyName = probed;
            } else {
                platformKeyName = readMeta(key).platformKeyName();
            }
        }
        return testResult(key, ok, ok ? 1 : 0,
                ok ? "密钥已配置（未向供应商发起真实扣费探测）" : "密钥为空或未启用，写入后再测",
                platformKeyName);
    }

    private ApiResponse testDomesticSmsChannel(String key) {
        String secretId = firstNonBlank(read("bgssai.sms.secret-id"), read("sms.secret_id"));
        String secretKey = firstNonBlank(read("bgssai.sms.secret-key"), read("sms.secret_key"));
        String sdkAppId = firstNonBlank(read("bgssai.sms.sdk-app-id"), read("sms.sdk_app_id"));
        String signName = firstNonBlank(read("bgssai.sms.sign-name"), read("sms.sign_name"));
        String templateId = firstNonBlank(read("bgssai.sms.template-id"), read("sms.template_id"));
        String endpoint = firstNonBlank(read("sms.endpoint"), DOMESTIC_ENDPOINT);
        String region = firstNonBlank(read("sms.region"), "ap-guangzhou");
        String countRaw = firstNonBlank(read("sms.template_param_count"), "2");
        if (secretId.isBlank() || secretKey.isBlank() || sdkAppId.isBlank()
                || signName.isBlank() || templateId.isBlank()) {
            return testResult(key, false, 0, "国内短信通道缺少必填项（SecretId/SecretKey/SdkAppId/SignName/TemplateId）");
        }
        if (!isValidEndpointAndRegion(endpoint, region)) {
            return testResult(key, false, 1, "国内短信接入域名与地域不兼容：" + endpoint + " / " + region);
        }
        if (!Set.of("2", "3", "4").contains(countRaw)) {
            return testResult(key, false, 1, "国内模板参数数量必须为 2、3 或 4");
        }
        return testResult(key, true, 1, "国内短信通道就绪（" + endpoint + " / " + region + " / " + countRaw + " 参）");
    }

    private ApiResponse testIntlSmsChannel(String key) {
        String enabledRaw = read("sms.intl.enabled");
        if (!"1".equals(enabledRaw) && !"true".equalsIgnoreCase(enabledRaw)) {
            return testResult(key, false, 0, "国际短信通道未开启（sms.intl.enabled != 1，严格失败关闭，不回落国内）");
        }
        String intlId = read("sms.intl.secret_id");
        String intlKey = read("sms.intl.secret_key");
        if (intlId.isBlank() != intlKey.isBlank()) {
            return testResult(key, false, 0, "国际短信密钥仅配置单侧，需全配或全留空复用国内");
        }
        String effectiveId = intlId.isBlank() ? firstNonBlank(read("bgssai.sms.secret-id"), read("sms.secret_id")) : intlId;
        String effectiveKey = intlKey.isBlank() ? firstNonBlank(read("bgssai.sms.secret-key"), read("sms.secret_key")) : intlKey;
        String sdkAppId = read("sms.intl.sdk_app_id");
        String templateId = read("sms.intl.template_id");
        String endpoint = firstNonBlank(read("sms.intl.endpoint"), DOMESTIC_ENDPOINT);
        String region = firstNonBlank(read("sms.intl.region"), "ap-guangzhou");
        String countRaw = firstNonBlank(read("sms.intl.template_param_count"), "2");
        if (effectiveId.isBlank() || effectiveKey.isBlank() || sdkAppId.isBlank() || templateId.isBlank()) {
            return testResult(key, false, 0, "国际短信通道缺少必填项（密钥/sms.intl.sdk_app_id/sms.intl.template_id）");
        }
        if (!isValidEndpointAndRegion(endpoint, region)) {
            return testResult(key, false, 1, "国际短信接入域名与地域不兼容：" + endpoint + " / " + region);
        }
        if (!Set.of("2", "3", "4").contains(countRaw)) {
            return testResult(key, false, 1, "国际模板参数数量必须为 2、3 或 4");
        }
        return testResult(key, true, 1, "国际短信通道就绪（" + endpoint + " / " + region + " / " + countRaw + " 参）");
    }

    private static ApiResponse testResult(String key, boolean ok, int configuredFlag, String message) {
        return testResult(key, ok, configuredFlag, message, null);
    }

    private static ApiResponse testResult(String key, boolean ok, int configuredFlag, String message, String platformKeyName) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("secret_key", key);
        result.put("ok", ok);
        result.put("configured_flag", configuredFlag);
        result.put("message", message);
        if (platformKeyName != null) {
            result.put("platform_key_name", platformKeyName);
        }
        return Responses.ok(result);
    }

    private static String normalizeAndValidateSecretValue(String key, String raw) {
        String val = raw.trim();
        if ("sms.endpoint".equals(key) || "sms.intl.endpoint".equals(key)) {
            if (!VALID_ENDPOINTS.contains(val)) {
                throw new BizException(CommonErrorCode.INVALID_PARAM,
                        "接入域名仅支持 sms.tencentcloudapi.com 或 sms.intl.tencentcloudapi.com");
            }
            return val;
        }
        if ("sms.region".equals(key)) {
            if (!DOMESTIC_REGIONS.contains(val)) {
                throw new BizException(CommonErrorCode.INVALID_PARAM,
                        "国内短信地域仅支持 ap-guangzhou / ap-beijing / ap-nanjing");
            }
            return val;
        }
        if ("sms.intl.region".equals(key)) {
            if (!ALL_VALID_REGIONS.contains(val)) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "国际短信地域配置无效");
            }
            return val;
        }
        if ("sms.template_param_count".equals(key) || "sms.intl.template_param_count".equals(key)) {
            if (!Set.of("2", "3", "4").contains(val)) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "模板参数数量仅支持 2、3 或 4");
            }
            return val;
        }
        if ("sms.intl.enabled".equals(key)) {
            if ("1".equals(val) || "true".equalsIgnoreCase(val)) {
                return "1";
            }
            if ("0".equals(val) || "false".equalsIgnoreCase(val)) {
                return "0";
            }
            throw new BizException(CommonErrorCode.INVALID_PARAM, "国际短信开关仅支持 0 或 1");
        }
        return val;
    }

    private static boolean isValidEndpointAndRegion(String endpoint, String region) {
        if (DOMESTIC_ENDPOINT.equals(endpoint)) {
            return DOMESTIC_REGIONS.contains(region);
        }
        if (INTL_ENDPOINT.equals(endpoint)) {
            return INTL_REGIONS.contains(region);
        }
        return false;
    }

    private static boolean isAllowedPrefix(String value) {
        return value.startsWith("ai.")
                || value.startsWith("sms.")
                || value.startsWith("bgssai.sms.")
                || value.startsWith("bgssai.smtp.")
                || value.startsWith("bgssai.oauth.");
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return true;
        }
        if (NON_SENSITIVE_SMS_KEYS.contains(key)) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("secret")
                || lower.contains("password")
                || lower.contains("private")
                || lower.contains("api_key")
                || lower.contains("token")
                || lower.startsWith("ai.");
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private boolean exists(String key) {
        String sql = "SELECT 1 FROM sys_secret WHERE secret_key = ? LIMIT 1";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "exists sys_secret failed");
        }
    }

    private void insert(String key, String value, String keyName, String platformKeyName,
                        String description, int enabled, String admin) {
        String sql = "INSERT INTO sys_secret (secret_key, secret_value, key_name, platform_key_name, description, enabled, updated_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, keyName);
            ps.setString(4, platformKeyName);
            ps.setString(5, description);
            ps.setInt(6, enabled);
            ps.setString(7, admin);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "insert sys_secret failed");
        }
    }

    private void updateKeyName(String key, String keyName, String admin) {
        String sql = "UPDATE sys_secret SET key_name = ?, updated_by = ? WHERE secret_key = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, keyName);
            ps.setString(2, admin);
            ps.setString(3, key);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "update sys_secret key_name failed");
        }
    }

    private void updatePlatformKeyName(String key, String platformKeyName, String admin) {
        String sql = "UPDATE sys_secret SET platform_key_name = ?, updated_by = ? WHERE secret_key = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, platformKeyName);
            ps.setString(2, admin);
            ps.setString(3, key);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "update sys_secret platform_key_name failed");
        }
    }

    private SecretMeta readMeta(String key) {
        String sql = "SELECT secret_value, key_name, platform_key_name, enabled FROM sys_secret WHERE secret_key = ? LIMIT 1";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SecretMeta(
                            trimToNull(rs.getString("secret_value")),
                            trimToNull(rs.getString("key_name")),
                            trimToNull(rs.getString("platform_key_name")),
                            rs.getInt("enabled"));
                }
            }
        } catch (Exception ignored) {
            return new SecretMeta(null, null, null, 0);
        }
        return new SecretMeta(null, null, null, 0);
    }

    private static String normalizeAndValidateKeyName(String raw) {
        String trimmed = trimToNull(raw);
        if (trimmed != null && trimmed.length() > MAX_KEY_NAME_LENGTH) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "密钥名称最多 64 个字符");
        }
        return trimmed;
    }

    private static boolean isMaskedSecret(String value) {
        return value != null && value.contains("****");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void updateValue(String key, String value, String admin) {
        String sql = "UPDATE sys_secret SET secret_value = ?, updated_by = ? WHERE secret_key = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, admin);
            ps.setString(3, key);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "update sys_secret value failed");
        }
    }

    private void updateEnabled(String key, int enabled, String admin) {
        String sql = "UPDATE sys_secret SET enabled = ?, updated_by = ? WHERE secret_key = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enabled);
            ps.setString(2, admin);
            ps.setString(3, key);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "update sys_secret enabled failed");
        }
    }

    private void updateDescription(String key, String description, String admin) {
        String sql = "UPDATE sys_secret SET description = ?, updated_by = ? WHERE secret_key = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setString(2, admin);
            ps.setString(3, key);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "update sys_secret description failed");
        }
    }

    private String read(String key) {
        String sql = "SELECT secret_value FROM sys_secret WHERE secret_key = ? AND enabled = 1";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String v = rs.getString(1);
                    return v == null ? "" : v.trim();
                }
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String v = value.trim();
        if (v.length() <= 8) {
            return "****";
        }
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }

    private void requireAdmin() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can manage secrets");
        }
    }
}
