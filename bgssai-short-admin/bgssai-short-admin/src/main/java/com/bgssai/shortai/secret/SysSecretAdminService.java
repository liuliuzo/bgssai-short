package com.bgssai.shortai.secret;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

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
 * Admin CRUD for business third-party secrets in sys_secret (AI keys first).
 */
@Service
public class SysSecretAdminService {

    private final DataSource dataSource;

    public SysSecretAdminService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ApiResponse list(String keyPrefix) {
        requireAdmin();
        String prefix = keyPrefix == null || keyPrefix.isBlank() ? "ai." : keyPrefix.trim();
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT secret_key, secret_value, description, enabled, updated_at, updated_by "
                + "FROM sys_secret WHERE secret_key LIKE ? ORDER BY secret_key ASC";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    String value = rs.getString("secret_value");
                    item.put("secret_key", rs.getString("secret_key"));
                    item.put("description", rs.getString("description"));
                    item.put("enabled", rs.getInt("enabled"));
                    item.put("configured_flag", value != null && !value.isBlank() ? 1 : 0);
                    item.put("secret_value_masked", mask(value));
                    item.put("updated_at", rs.getTimestamp("updated_at"));
                    item.put("updated_by", rs.getString("updated_by"));
                    list.add(item);
                }
            }
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "list sys_secret failed");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("secret_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse update(String secretKey, String secretValue, Integer enabled, String description) {
        requireAdmin();
        if (secretKey == null || secretKey.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "secret_key required");
        }
        String key = secretKey.trim();
        if (!key.startsWith("ai.")) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "admin AI secrets page only edits ai.* keys");
        }
        String admin = UserContext.getUserId() == null ? "admin" : UserContext.getUserId();
        boolean exists = exists(key);
        if (!exists) {
            insert(key, secretValue == null ? "" : secretValue.trim(),
                    description == null ? key : description,
                    enabled == null ? 1 : enabled, admin);
        } else {
            if (secretValue != null) {
                updateValue(key, secretValue.trim(), admin);
            }
            if (enabled != null) {
                updateEnabled(key, enabled, admin);
            }
            if (description != null) {
                updateDescription(key, description, admin);
            }
        }
        String current = read(key);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("secret_key", key);
        result.put("configured_flag", current != null && !current.isBlank() ? 1 : 0);
        return Responses.ok(result);
    }

    public ApiResponse testConnection(String secretKey) {
        requireAdmin();
        if (secretKey == null || secretKey.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "secret_key required");
        }
        String key = secretKey.trim();
        String value = read(key);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("secret_key", key);
        boolean ok = value != null && !value.isBlank();
        result.put("ok", ok);
        result.put("configured_flag", ok ? 1 : 0);
        result.put("message", ok ? "密钥已配置（未向供应商发起真实扣费探测）" : "密钥为空或未启用，写入后再测");
        return Responses.ok(result);
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

    private void insert(String key, String value, String description, int enabled, String admin) {
        String sql = "INSERT INTO sys_secret (secret_key, secret_value, description, enabled, updated_by) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, description);
            ps.setInt(4, enabled);
            ps.setString(5, admin);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "insert sys_secret failed");
        }
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
