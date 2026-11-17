package com.bgssai.shortai.useraimodel.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.UserAiProviderKeyMapper;
import com.bgssai.shortai.model.UserAiProviderKey;
import com.bgssai.shortai.model.UserAiProviderKeyExample;
import com.bgssai.shortai.useraimodel.dto.UpsertUserAiProviderKeyRequest;
import com.bgssai.shortai.useraimodel.dto.UserAiProviderKeyItem;
import com.bgssai.shortai.useraimodel.service.UserAiProviderCatalog.ProviderSpec;

/**
 * 用户自有厂商 API Key 管理与运行期解析服务。
 */
@Service
public class UserAiProviderKeyService {

    private static final Logger log = LoggerFactory.getLogger(UserAiProviderKeyService.class);

    public record ResolvedProviderKey(
            String userId,
            String providerCode,
            String apiKey,
            String accessKey,
            String secretKey,
            String baseUrl,
            boolean enabled
    ) {
        public String effectiveBaseUrl() {
            return UserAiProviderCatalog.resolveEffectiveBaseUrl(baseUrl, providerCode);
        }

        public String resolveKlingBearerToken() {
            return UserAiProviderCatalog.resolveKlingBearerToken(apiKey, accessKey, secretKey);
        }
    }

    private final UserAiProviderKeyMapper userAiProviderKeyMapper;

    @Autowired
    public UserAiProviderKeyService(UserAiProviderKeyMapper userAiProviderKeyMapper) {
        this.userAiProviderKeyMapper = userAiProviderKeyMapper;
    }

    public List<UserAiProviderKeyItem> listForCurrentUser() {
        return listByUserId(requireCurrentUserId());
    }

    public List<UserAiProviderKeyItem> listByUserId(String userId) {
        Map<String, UserAiProviderKey> stored = loadMapByUserId(userId);
        List<UserAiProviderKeyItem> items = new ArrayList<>();
        for (ProviderSpec spec : UserAiProviderCatalog.all()) {
            UserAiProviderKey row = stored.get(spec.providerCode());
            items.add(toItem(spec, row));
        }
        return items;
    }

    public UserAiProviderKeyItem upsert(UpsertUserAiProviderKeyRequest req) {
        return upsertForUser(requireCurrentUserId(), req);
    }

    public UserAiProviderKeyItem upsertForUser(String userId, UpsertUserAiProviderKeyRequest req) {
        if (userId == null || userId.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "未登录");
        }
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请求参数不能为空");
        }
        String providerCode = UserAiProviderCatalog.requireSupportedProviderCode(req.getProviderCode());
        ProviderSpec spec = UserAiProviderCatalog.get(providerCode);

        String rawApiKey = trimToNull(req.getApiKey());
        String rawAccessKey = trimToNull(req.getAccessKey());
        String rawSecretKey = trimToNull(req.getSecretKey());
        rejectMaskValues(rawApiKey, rawAccessKey, rawSecretKey);

        boolean baseUrlSubmitted = req.getBaseUrl() != null;
        String validatedBaseUrl = baseUrlSubmitted ? UserAiProviderCatalog.validateBaseUrl(req.getBaseUrl()) : null;

        UserAiProviderKey existing = selectOneByUserAndProvider(userId, providerCode);

        String finalApiKey;
        String finalAccessKey;
        String finalSecretKey;

        boolean hasNewApiKey = hasText(rawApiKey);
        boolean hasNewAk = hasText(rawAccessKey);
        boolean hasNewSk = hasText(rawSecretKey);

        if (!spec.supportsAkSk()) {
            if (hasNewAk || hasNewSk) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, providerCode + " 仅支持 api_key 配置");
            }
            if (hasNewApiKey) {
                finalApiKey = rawApiKey;
            } else if (existing != null && hasText(existing.getApiKey())) {
                finalApiKey = existing.getApiKey();
            } else {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "请填写 api_key");
            }
            finalAccessKey = null;
            finalSecretKey = null;
        } else {
            if (!hasNewApiKey && !hasNewAk && !hasNewSk) {
                if (existing == null || !hasUsableCredential(existing)) {
                    throw new BizException(CommonErrorCode.INVALID_PARAM,
                            "可灵需填写 api_key 或同时填写 access_key 与 secret_key");
                }
                finalApiKey = trimToNull(existing.getApiKey());
                finalAccessKey = trimToNull(existing.getAccessKey());
                finalSecretKey = trimToNull(existing.getSecretKey());
            } else if (hasNewApiKey && !hasNewAk && !hasNewSk) {
                finalApiKey = rawApiKey;
                finalAccessKey = null;
                finalSecretKey = null;
            } else {
                finalAccessKey = hasNewAk
                        ? rawAccessKey
                        : (existing != null ? trimToNull(existing.getAccessKey()) : null);
                finalSecretKey = hasNewSk
                        ? rawSecretKey
                        : (existing != null ? trimToNull(existing.getSecretKey()) : null);
                if (!hasText(finalAccessKey) || !hasText(finalSecretKey)) {
                    throw new BizException(CommonErrorCode.INVALID_PARAM,
                            "可灵 AK/SK 模式需同时提供 access_key 和 secret_key");
                }
                finalApiKey = hasNewApiKey ? rawApiKey : null;
            }
        }

        String finalBaseUrl = baseUrlSubmitted
                ? validatedBaseUrl
                : (existing != null ? trimToNull(existing.getBaseUrl()) : null);
        Byte finalEnabled;
        if (req.getEnabled() != null) {
            finalEnabled = (byte) (req.getEnabled() ? 1 : 0);
        } else if (existing != null && existing.getEnabled() != null) {
            finalEnabled = existing.getEnabled();
        } else {
            finalEnabled = (byte) 1;
        }

        Date now = new Date();
        if (existing == null) {
            UserAiProviderKey row = new UserAiProviderKey();
            row.setUserId(userId);
            row.setProviderCode(providerCode);
            row.setApiKey(finalApiKey);
            row.setAccessKey(finalAccessKey);
            row.setSecretKey(finalSecretKey);
            row.setBaseUrl(finalBaseUrl);
            row.setEnabled(finalEnabled);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            userAiProviderKeyMapper.insertSelective(row);
            return toItem(spec, row);
        }

        existing.setApiKey(finalApiKey);
        existing.setAccessKey(finalAccessKey);
        existing.setSecretKey(finalSecretKey);
        existing.setBaseUrl(finalBaseUrl);
        existing.setEnabled(finalEnabled);
        existing.setUpdatedAt(now);
        userAiProviderKeyMapper.updateByPrimaryKey(existing);
        return toItem(spec, existing);
    }

    public Map<String, Object> deleteByProviderCode(String providerCode) {
        return deleteForUser(requireCurrentUserId(), providerCode);
    }

    public Map<String, Object> deleteForUser(String userId, String rawProviderCode) {
        if (userId == null || userId.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "未登录");
        }
        String providerCode = UserAiProviderCatalog.requireSupportedProviderCode(rawProviderCode);
        UserAiProviderKeyExample ex = new UserAiProviderKeyExample();
        ex.createCriteria().andUserIdEqualTo(userId).andProviderCodeEqualTo(providerCode);
        int affected = userAiProviderKeyMapper.deleteByExample(ex);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider_code", providerCode);
        result.put("deleted", affected > 0);
        result.put("configured", false);
        return result;
    }

    public Optional<ResolvedProviderKey> findActiveKey(String userId, String rawProviderCode) {
        if (!hasText(userId) || !hasText(rawProviderCode)) {
            return Optional.empty();
        }
        String canonical = UserAiProviderCatalog.normalizeRuntimeProviderCode(rawProviderCode);
        if (!UserAiProviderCatalog.isSupported(canonical)) {
            return Optional.empty();
        }
        UserAiProviderKey row;
        try {
            UserAiProviderKeyExample ex = new UserAiProviderKeyExample();
            ex.createCriteria()
                    .andUserIdEqualTo(userId.trim())
                    .andProviderCodeEqualTo(canonical)
                    .andEnabledEqualTo((byte) 1);
            row = userAiProviderKeyMapper.selectOneByExample(ex);
        } catch (DataAccessException e) {
            log.warn("读取 user_ai_provider_key 失败 userId={} provider={}: {}", userId, canonical, e.getMessage());
            return Optional.empty();
        }
        return toResolved(row);
    }

    public boolean hasActiveKey(String userId, String rawProviderCode) {
        return findActiveKey(userId, rawProviderCode).isPresent();
    }

    public List<ResolvedProviderKey> listActiveKeysForUser(String userId) {
        if (!hasText(userId)) {
            return List.of();
        }
        List<UserAiProviderKey> rows;
        try {
            UserAiProviderKeyExample ex = new UserAiProviderKeyExample();
            ex.createCriteria().andUserIdEqualTo(userId.trim()).andEnabledEqualTo((byte) 1);
            rows = userAiProviderKeyMapper.selectByExample(ex);
        } catch (DataAccessException e) {
            log.warn("读取 user_ai_provider_key 列表失败 userId={}: {}", userId, e.getMessage());
            return List.of();
        }
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ResolvedProviderKey> resolved = new ArrayList<>();
        for (UserAiProviderKey row : rows) {
            toResolved(row).ifPresent(resolved::add);
        }
        return resolved;
    }

    public Map<String, ResolvedProviderKey> activeKeyMap(String userId) {
        Map<String, ResolvedProviderKey> map = new LinkedHashMap<>();
        for (ResolvedProviderKey key : listActiveKeysForUser(userId)) {
            map.put(key.providerCode(), key);
        }
        return map;
    }

    public boolean hasAnyActiveProviderKey(String userId) {
        return !listActiveKeysForUser(userId).isEmpty();
    }

    private Optional<ResolvedProviderKey> toResolved(UserAiProviderKey row) {
        if (row == null || !isRowEnabled(row) || !hasUsableCredential(row)) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedProviderKey(
                trimToNull(row.getUserId()),
                UserAiProviderCatalog.normalizeProviderCode(row.getProviderCode()),
                trimToNull(row.getApiKey()),
                trimToNull(row.getAccessKey()),
                trimToNull(row.getSecretKey()),
                trimToNull(row.getBaseUrl()),
                true
        ));
    }

    private Map<String, UserAiProviderKey> loadMapByUserId(String userId) {
        if (!hasText(userId)) {
            return Map.of();
        }
        List<UserAiProviderKey> rows;
        try {
            UserAiProviderKeyExample ex = new UserAiProviderKeyExample();
            ex.createCriteria().andUserIdEqualTo(userId.trim());
            rows = userAiProviderKeyMapper.selectByExample(ex);
        } catch (DataAccessException e) {
            log.warn("查询 user_ai_provider_key 失败 userId={}: {}", userId, e.getMessage());
            return Map.of();
        }
        Map<String, UserAiProviderKey> map = new LinkedHashMap<>();
        if (rows != null) {
            for (UserAiProviderKey row : rows) {
                map.put(UserAiProviderCatalog.normalizeProviderCode(row.getProviderCode()), row);
            }
        }
        return map;
    }

    private UserAiProviderKey selectOneByUserAndProvider(String userId, String providerCode) {
        UserAiProviderKeyExample ex = new UserAiProviderKeyExample();
        ex.createCriteria().andUserIdEqualTo(userId.trim()).andProviderCodeEqualTo(providerCode);
        return userAiProviderKeyMapper.selectOneByExample(ex);
    }

    private UserAiProviderKeyItem toItem(ProviderSpec spec, UserAiProviderKey row) {
        UserAiProviderKeyItem item = new UserAiProviderKeyItem();
        item.setProviderCode(spec.providerCode());
        item.setProviderName(spec.providerName());
        item.setDefaultBaseUrl(spec.defaultBaseUrl());
        item.setSupportsAkSk(spec.supportsAkSk());

        if (row == null || !hasUsableCredential(row)) {
            item.setAuthMode("NONE");
            item.setConfigured(false);
            item.setEnabled(false);
            item.setHasSecretKey(false);
            return item;
        }

        boolean hasApiKey = hasText(row.getApiKey());
        boolean hasAk = hasText(row.getAccessKey());
        boolean hasSk = hasText(row.getSecretKey());

        item.setConfigured(true);
        item.setEnabled(isRowEnabled(row));
        item.setHasSecretKey(hasSk);
        item.setBaseUrl(trimToNull(row.getBaseUrl()));
        item.setUpdatedAt(row.getUpdatedAt());

        if (hasAk && hasSk) {
            String maskedAk = UserAiModelSupport.maskApiKey(row.getAccessKey());
            item.setAuthMode("AK_SK");
            item.setMaskedAccessKey(maskedAk);
            item.setMaskedKey(hasApiKey ? UserAiModelSupport.maskApiKey(row.getApiKey()) : maskedAk);
        } else {
            item.setAuthMode("API_KEY");
            item.setMaskedKey(hasApiKey ? UserAiModelSupport.maskApiKey(row.getApiKey()) : null);
            item.setMaskedAccessKey(hasAk ? UserAiModelSupport.maskApiKey(row.getAccessKey()) : null);
        }
        return item;
    }

    private static String requireCurrentUserId() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId.trim();
    }

    private static void rejectMaskValues(String... values) {
        for (String val : values) {
            if (UserAiProviderCatalog.containsMaskPattern(val)) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "不能提交脱敏占位密钥");
            }
        }
    }

    private static boolean hasUsableCredential(UserAiProviderKey row) {
        return row != null && (hasText(row.getApiKey())
                || (hasText(row.getAccessKey()) && hasText(row.getSecretKey())));
    }

    private static boolean isRowEnabled(UserAiProviderKey row) {
        return row != null && row.getEnabled() != null && row.getEnabled() == 1;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasText(String raw) {
        return raw != null && !raw.trim().isEmpty();
    }
}
