package com.bgssai.shortai.useraimodel.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.aimodel.ProductModelAlias;
import com.bgssai.shortai.billing.UsageScope;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.UserAiModelKeyMapper;
import com.bgssai.shortai.mapper.UserAiSettingMapper;
import com.bgssai.shortai.mapper.UserAiSavedCredentialMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.UserAiCredentialData;
import com.bgssai.shortai.model.UserAiSavedCredential;
import com.bgssai.shortai.model.UserAiModelKey;
import com.bgssai.shortai.model.UserAiModelKeyExample;
import com.bgssai.shortai.model.UserAiSetting;
import com.bgssai.shortai.model.UserAiSettingExample;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.useraimodel.dto.SavedCredentialRequest;
import com.bgssai.shortai.useraimodel.dto.CreateUserAiModelKeyItem;
import com.bgssai.shortai.useraimodel.dto.CreateUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.dto.CreateUserAiModelKeyResponse;
import com.bgssai.shortai.useraimodel.dto.DeleteUserAiModelKeyResponse;
import com.bgssai.shortai.useraimodel.dto.TestUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.dto.TestUserAiModelKeyResponse;
import com.bgssai.shortai.useraimodel.dto.ToggleUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.dto.UpdateUserAiKeyModeRequest;
import com.bgssai.shortai.useraimodel.dto.UpdateUserAiKeyModeResponse;
import com.bgssai.shortai.useraimodel.dto.UpdateUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyIdRequest;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyItem;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyItemResponse;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyListResponse;

@Service
public class UserAiModelKeyService {

    private static final Logger log = LoggerFactory.getLogger(UserAiModelKeyService.class);

    private static final List<String> PLATFORM_ABILITIES = List.of(
            "TEXT", "IMAGE", "VIDEO", "VOICE", "ASR", "MUSIC");

    @Autowired
    private UserAiModelKeyMapper userAiModelKeyMapper;

    @Autowired
    private UserAiSettingMapper userAiSettingMapper;

    @Autowired
    private ExternalApiKeyBalanceClient externalApiKeyBalanceClient;

    @Autowired
    private SavedCredentialRegistry registry;

    @Autowired
    private UserAiSavedCredentialMapper savedCredentialMapper;

    private final HttpClient probeHttp = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String currentUserOrScopeUserId() {
        String userId = UserContext.getUserId();
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        UsageScope scope = UsageScope.current();
        String scoped = scope == null ? null : scope.userId();
        return scoped == null || scoped.isBlank() ? null : scoped.trim();
    }

    private String requireCurrentUser() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "no user in context");
        }
        return userId.trim();
    }

    public UserAiModelKeyListResponse listForCurrentUser(String modalityType, String abilityType) {
        String userId = requireCurrentUser();
        log.info("查询用户模型密钥列表 userId={} modalityType={} abilityType={}", userId, modalityType, abilityType);
        List<UserAiSavedCredential> saved = registry.prepare(userId);
        listEnabledRows(userId, null);
        UserAiModelKeyExample ex = new UserAiModelKeyExample();
        UserAiModelKeyExample.Criteria criteria = ex.createCriteria()
                .andUserIdEqualTo(userId)
                .andDelFlagEqualTo(0);
        if (modalityType != null && !modalityType.isBlank()) {
            criteria.andModalityTypeEqualTo(modalityType.trim().toUpperCase(Locale.ROOT));
        }
        if (abilityType != null && !abilityType.isBlank()) {
            criteria.andAbilityTypeEqualTo(abilityType.trim().toUpperCase(Locale.ROOT));
        }
        ex.setOrderByClause("ability_type asc, is_default_model desc, id asc");
        List<UserAiModelKey> rows = userAiModelKeyMapper.selectByExample(ex);
        List<UserAiModelKeyItem> items = new ArrayList<>();
        if (rows != null) {
            for (UserAiModelKey row : rows) {
                migrateLegacyProvider(row);
                items.add(toItem(row));
            }
        }
        var credentialItems = saved.stream().map(UserAiModelKeyService::credentialItem).toList();
        var balanceRows = new ArrayList<UserAiCredentialData>(rows == null ? List.of() : rows);
        var balanceItems = new ArrayList<>(items);
        balanceRows.addAll(saved);
        balanceItems.addAll(credentialItems);
        externalApiKeyBalanceClient.fill(balanceRows, balanceItems);
        Set<String> activePlatform = activePlatformAbilities(userId);
        for (String ability : PLATFORM_ABILITIES) {
            if (!platformRowVisible(ability, modalityType, abilityType)) {
                continue;
            }
            items.add(platformItem(ability, activePlatform.contains(ability)));
        }
        items.sort((left, right) -> {
            int byAbility = Integer.compare(platformAbilityOrder(left.getAbilityType()),
                    platformAbilityOrder(right.getAbilityType()));
            if (byAbility != 0) {
                return byAbility;
            }
            int byPlatform = Integer.compare(
                    Integer.valueOf(1).equals(right.getPlatformModel()) ? 1 : 0,
                    Integer.valueOf(1).equals(left.getPlatformModel()) ? 1 : 0);
            if (byPlatform != 0) {
                return byPlatform;
            }
            return Integer.compare(
                    Integer.valueOf(1).equals(right.getDefaultModel()) ? 1 : 0,
                    Integer.valueOf(1).equals(left.getDefaultModel()) ? 1 : 0);
        });
        UserAiModelKeyListResponse resp = new UserAiModelKeyListResponse();
        // Kept for response compatibility; there is no exclusive mode anymore.
        resp.setOnlyUserKey(0);
        resp.setKeyList(items);
        resp.setCredentialList(credentialItems);
        resp.setProviderPresetList(UserAiModelSupport.buildProviderPresets());
        return resp;
    }

    @Transactional
    public CreateUserAiModelKeyResponse create(CreateUserAiModelKeyRequest req) {
        String userId = requireCurrentUser();
        if (req == null) throw new BizException(CommonErrorCode.INVALID_PARAM, "request required");
        List<UserAiSavedCredential> saved = registry.prepare(userId);
        boolean legacy = req.getApiKey() != null && !req.getApiKey().isBlank();
        UserAiSavedCredential credential = null;
        if (!legacy) {
            if (req.getProviderCode() == null || req.getProviderCode().isBlank()) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "provider_code required");
            }
            String provider = UserAiModelSupport.normalizeProvider(req.getProviderCode());
            credential = SavedCredentialRegistry.active(saved, provider);
        }
        UserAiModelSupport.OwnedProviderBinding bound = UserAiModelSupport.bindOwnedProvider(
                legacy ? req.getProviderCode() : credential.getProviderCode(),
                legacy ? req.getBaseUrl() : credential.getBaseUrl());
        String providerCode = bound.providerCode();
        String baseUrl = bound.baseUrl();
        String plainKey = legacy ? secretValue(req.getApiKey(), null) : credential.getApiKey();
        if (plainKey.length() > 512) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "api_key too long");
        }
        String plainSecret = legacy ? secretValue(req.getSecretKey(), null) : credential.getSecretKey();
        if (plainSecret != null && plainSecret.length() > 512) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "secret_key too long");
        }
        if (plainSecret != null && plainSecret.isBlank()) {
            plainSecret = null;
        }
        if (UserAiModelSupport.PROVIDER_KLING.equals(providerCode)) {
            KlingJwtSupport.resolveBearerToken(plainKey, plainSecret, baseUrl);
        }

        String keyName = legacy ? normalizeKeyName(req.getKeyName()) : credential.getKeyName();
        boolean activate = !Boolean.FALSE.equals(req.getActivateOnCreate());
        List<CreateUserAiModelKeyItem> pending = new ArrayList<>();
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            for (CreateUserAiModelKeyItem item : req.getItems()) {
                if (item != null) {
                    pending.add(item);
                }
            }
        } else {
            CreateUserAiModelKeyItem single = new CreateUserAiModelKeyItem();
            single.setModalityType(req.getModalityType());
            single.setAbilityType(req.getAbilityType());
            single.setModelCode(req.getModelCode());
            single.setModelName(req.getModelName());
            pending.add(single);
        }
        if (pending.isEmpty() || pending.size() > 20) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "items count invalid");
        }

        log.info("创建用户模型密钥 userId={} providerCode={} count={}", userId, providerCode, pending.size());
        Date now = new Date();
        List<UserAiModelKey> createdRows = new ArrayList<>();
        List<UserAiModelKeyItem> createdItems = new ArrayList<>();
        Map<String, String> enabledKeyByAbility = new LinkedHashMap<>();
        for (CreateUserAiModelKeyItem item : pending) {
            String ability = UserAiModelSupport.normalizeAbility(item.getModalityType(), item.getAbilityType());
            String modality = UserAiModelSupport.modalityForAbility(ability);
            String modelCode = item.getModelCode() == null ? "" : item.getModelCode().trim();
            if (modelCode.isBlank() || modelCode.length() > 128) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "model_code invalid");
            }
            String modelName = item.getModelName() == null || item.getModelName().isBlank()
                    ? modelCode : item.getModelName().trim();
            if (modelName.length() > 128) {
                modelName = modelName.substring(0, 128);
            }

            UserAiModelKey row = new UserAiModelKey();
            row.setKeyId(ShortIds.id(UserAiModelSupport.USER_KEY_PREFIX));
            row.setUserId(userId);
            row.setKeyName(keyName == null || keyName.isEmpty() ? null : keyName);
            row.setModalityType(modality);
            row.setAbilityType(ability);
            row.setProviderCode(providerCode);
            row.setModelCode(modelCode);
            row.setModelName(modelName);
            row.setBaseUrl(baseUrl);
            row.setApiKey(plainKey);
            row.setSecretKey(plainSecret);
            if (credential == null) {
                credential = SavedCredentialRegistry.findMatching(saved, row);
                if (credential == null) {
                    boolean first = saved.stream().noneMatch(c -> providerCode.equals(c.getProviderCode()));
                    credential = registry.insert(row, first);
                    saved.add(credential);
                }
            }
            row.setCredentialId(credential.getCredentialId());
            row.setEnabled(0);
            row.setDefaultModel(0);
            row.setDelFlag(0);
            row.setCreator(userId);
            row.setModifier(userId);
            row.setGmtCreate(now);
            row.setGmtModified(now);
            userAiModelKeyMapper.insertSelective(row);
            if (activate) {
                enabledKeyByAbility.put(ability, row.getKeyId());
            }
            createdRows.add(row);
            createdItems.add(toItem(row));
        }
        Set<String> enabledIds = new LinkedHashSet<>(enabledKeyByAbility.values());
        for (Map.Entry<String, String> winner : enabledKeyByAbility.entrySet()) {
            claimAbility(userId, winner.getKey(), winner.getValue());
        }
        for (UserAiModelKeyItem created : createdItems) {
            if (enabledIds.contains(created.getKeyId())) {
                created.setEnabled(1);
                created.setDefaultModel(1);
            } else {
                created.setEnabled(0);
                created.setDefaultModel(0);
            }
        }
        if (legacy && activate) {
            registry.inferSelection(userId, providerCode, registry.models(userId), saved);
        }
        externalApiKeyBalanceClient.fill(createdRows, createdItems);

        log.info("创建用户模型密钥完成 userId={} createdCount={}", userId, createdItems.size());

        CreateUserAiModelKeyResponse resp = new CreateUserAiModelKeyResponse();
        resp.setOnlyUserKey(0);
        resp.setCreatedCount(createdItems.size());
        resp.setKeyList(createdItems);
        return resp;
    }

    @Transactional
    public UserAiModelKeyItemResponse update(UpdateUserAiModelKeyRequest req) {
        String userId = requireCurrentUser();
        if (req == null || req.getKeyId() == null || req.getKeyId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "key_id required");
        }
        var saved = registry.prepare(userId);
        UserAiModelKey existing = requireOwnedRow(userId, req.getKeyId());
        boolean modelOnly = Boolean.TRUE.equals(req.getProviderModelSelection());
        if (modelOnly && req.getProviderCode() != null && !existing.getProviderCode().equals(req.getProviderCode())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请为新供应商另建模型");
        }
        if (modelOnly && (req.getBaseUrl() != null || req.getApiKey() != null || req.getSecretKey() != null || req.getKeyName() != null)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请在密钥管理中更新凭据");
        }
        String rawProvider = req.getProviderCode() == null || req.getProviderCode().isBlank()
                ? existing.getProviderCode() : req.getProviderCode();
        String rawBase = req.getBaseUrl() == null || req.getBaseUrl().isBlank()
                ? existing.getBaseUrl() : req.getBaseUrl();
        UserAiModelSupport.OwnedProviderBinding bound = UserAiModelSupport.bindOwnedProvider(rawProvider, rawBase);
        String providerCode = bound.providerCode();
        String ability = req.getAbilityType() == null || req.getAbilityType().isBlank()
                ? (req.getModalityType() == null || req.getModalityType().isBlank()
                        ? existing.getAbilityType()
                        : UserAiModelSupport.normalizeAbility(req.getModalityType(), null))
                : UserAiModelSupport.normalizeAbility(req.getModalityType(), req.getAbilityType());
        String modality = UserAiModelSupport.modalityForAbility(ability);
        String baseUrl = bound.baseUrl();
        String modelCode = req.getModelCode() == null || req.getModelCode().isBlank()
                ? existing.getModelCode() : req.getModelCode().trim();
        if (modelCode.length() > 128) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "model_code too long");
        }
        String modelName = req.getModelName() == null || req.getModelName().isBlank()
                ? existing.getModelName() : req.getModelName().trim();
        if (modelName.length() > 128) {
            modelName = modelName.substring(0, 128);
        }

        log.info("更新用户模型密钥 userId={} keyId={} abilityType={} modelCode={}",
                userId, existing.getKeyId(), ability, modelCode);

        UserAiModelKey patch = new UserAiModelKey();
        patch.setId(existing.getId());
        patch.setProviderCode(providerCode);
        patch.setAbilityType(ability);
        patch.setModalityType(modality);
        patch.setModelCode(modelCode);
        patch.setModelName(modelName);
        patch.setKeyName(normalizeKeyName(req.getKeyName()));
        patch.setBaseUrl(baseUrl);
        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            String plainKey = req.getApiKey().trim();
            if (plainKey.length() > 512) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "api_key too long");
            }
            patch.setApiKey(plainKey);
        }
        if (req.getSecretKey() != null && !req.getSecretKey().isBlank()) {
            String plainSecret = req.getSecretKey().trim();
            if (plainSecret.length() > 512) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "secret_key too long");
            }
            patch.setSecretKey(plainSecret);
        }
        if (UserAiModelSupport.PROVIDER_KLING.equals(providerCode)) {
            String effectiveAk = patch.getApiKey() != null ? patch.getApiKey() : existing.getApiKey();
            String effectiveSk = patch.getSecretKey() != null ? patch.getSecretKey() : existing.getSecretKey();
            KlingJwtSupport.resolveBearerToken(effectiveAk, effectiveSk, baseUrl);
        }
        if (modelOnly) {
            if (!ability.equals(existing.getAbilityType())) throw new BizException(CommonErrorCode.INVALID_PARAM, "请为新能力另建模型");
            for (var member : registry.models(userId)) {
                if (!sameModel(member, existing)) continue;
                var update = new UserAiModelKey();
                update.setId(member.getId());
                update.setModelCode(modelCode);
                update.setModelName(modelName);
                update.setModifier(userId);
                update.setGmtModified(new Date());
                userAiModelKeyMapper.updateByPrimaryKeySelective(update);
            }
            return new UserAiModelKeyItemResponse(describe(requireOwnedRow(userId, existing.getKeyId())));
        }

        var previousCredential = SavedCredentialRegistry.require(saved, existing.getCredentialId());
        var effective = new UserAiModelKey();
        effective.setUserId(userId);
        effective.setProviderCode(providerCode);
        effective.setBaseUrl(baseUrl);
        effective.setApiKey(patch.getApiKey() == null ? existing.getApiKey() : patch.getApiKey());
        effective.setSecretKey(patch.getSecretKey() == null ? existing.getSecretKey() : patch.getSecretKey());
        String effectiveName = patch.getKeyName() == null ? existing.getKeyName() : patch.getKeyName();
        effective.setKeyName(effectiveName == null || effectiveName.isEmpty() ? null : effectiveName);
        var associated = SavedCredentialRegistry.findMatching(saved, effective);
        if (associated == null) {
            boolean nameOnly = previousCredential.getProviderCode().equals(providerCode)
                    && SavedCredentialRegistry.sameConnection(previousCredential, effective);
            boolean shared = registry.models(userId).stream().anyMatch(row -> !row.getKeyId().equals(existing.getKeyId())
                    && previousCredential.getCredentialId().equals(row.getCredentialId()));
            if (nameOnly && !shared) {
                previousCredential.setKeyName(effective.getKeyName());
                savedCredentialMapper.update(previousCredential);
                associated = previousCredential;
            } else {
                associated = registry.insert(effective, saved.stream().noneMatch(c -> providerCode.equals(c.getProviderCode())));
                saved.add(associated);
            }
        }
        patch.setCredentialId(associated.getCredentialId());
        if (!SavedCredentialRegistry.sameConnection(existing, associated) || !existing.getProviderCode().equals(providerCode)) {
            existing.setModelCode(modelCode);
            existing.setModelName(modelName);
            existing.setModalityType(modality);
            existing.setAbilityType(ability);
            var updated = replaceBinding(existing, associated);
            registry.inferSelection(userId, providerCode, registry.models(userId), saved);
            return new UserAiModelKeyItemResponse(describe(requireOwnedRow(userId, updated.getKeyId())));
        }
        patch.setModifier(userId);
        patch.setGmtModified(new Date());
        userAiModelKeyMapper.updateByPrimaryKeySelective(patch);
        if (Integer.valueOf(1).equals(existing.getEnabled()) && !ability.equals(existing.getAbilityType())) {
            claimAbility(userId, ability, existing.getKeyId());
        }

        UserAiModelKey updated = requireOwnedRow(userId, existing.getKeyId());
        log.info("更新用户模型密钥完成 userId={} keyId={}", userId, existing.getKeyId());
        return new UserAiModelKeyItemResponse(describe(updated));
    }

    @Transactional
    public UserAiModelKeyItemResponse toggle(ToggleUserAiModelKeyRequest req) {
        String userId = requireCurrentUser();
        if (req == null || req.getKeyId() == null || req.getKeyId().isBlank() || req.getEnabled() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "key_id and enabled required");
        }
        var saved = registry.prepare(userId);
        int enabled = Integer.valueOf(1).equals(req.getEnabled()) ? 1 : 0;
        String platformAbility = ProductModelAlias.abilityOfCode(req.getKeyId());
        if (platformAbility != null) {
            log.info("切换自建模型启停 userId={} abilityType={} enabled={}", userId, platformAbility, enabled);
            if (enabled == 1) {
                selectPlatform(userId, platformAbility);
            } else {
                suppressPlatform(userId, platformAbility);
            }
            log.info("切换自建模型启停完成 userId={} abilityType={} enabled={}", userId, platformAbility, enabled);
            return new UserAiModelKeyItemResponse(platformItem(platformAbility, enabled == 1));
        }
        UserAiModelKey existing = requireOwnedRow(userId, req.getKeyId());
        log.info("切换用户模型密钥启停 userId={} keyId={} enabled={}", userId, existing.getKeyId(), enabled);

        if (enabled == 1) {
            if (Boolean.TRUE.equals(req.getProviderModelSelection())) {
                var active = SavedCredentialRegistry.active(saved, existing.getProviderCode());
                if (!active.getCredentialId().equals(existing.getCredentialId())) {
                    existing = replaceBinding(existing, active);
                }
            }
            claimAbility(userId, existing.getAbilityType(), existing.getKeyId());
            if (!Boolean.TRUE.equals(req.getProviderModelSelection())) {
                registry.inferSelection(userId, existing.getProviderCode(), registry.models(userId), saved);
            }
        } else {
            UserAiModelKey patch = new UserAiModelKey();
            patch.setId(existing.getId());
            patch.setEnabled(0);
            if (Integer.valueOf(1).equals(existing.getDefaultModel())) {
                patch.setDefaultModel(0);
            }
            patch.setModifier(userId);
            patch.setGmtModified(new Date());
            userAiModelKeyMapper.updateByPrimaryKeySelective(patch);
            if (Integer.valueOf(1).equals(existing.getDefaultModel())) {
                promoteFallbackDefault(userId, existing.getAbilityType(), existing.getKeyId());
            }
        }
        UserAiModelKey updated = requireOwnedRow(userId, existing.getKeyId());
        log.info("切换用户模型密钥启停完成 userId={} keyId={} enabled={}", userId, existing.getKeyId(), enabled);
        return new UserAiModelKeyItemResponse(describe(updated));
    }

    @Transactional
    public UserAiModelKeyItemResponse setDefault(UserAiModelKeyIdRequest req) {
        String userId = requireCurrentUser();
        if (req == null || req.getKeyId() == null || req.getKeyId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "key_id required");
        }
        var saved = registry.prepare(userId);
        UserAiModelKey target = requireOwnedRow(userId, req.getKeyId());
        if (Boolean.TRUE.equals(req.getProviderModelSelection())) {
            var current = SavedCredentialRegistry.active(saved, target.getProviderCode());
            if (!current.getCredentialId().equals(target.getCredentialId())) target = replaceBinding(target, current);
        } else if (!Integer.valueOf(1).equals(target.getEnabled())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "请先启用该模型密钥后再设为默认");
        }
        log.info("设置用户默认模型密钥 userId={} keyId={} abilityType={}",
                userId, target.getKeyId(), target.getAbilityType());
        claimAbility(userId, target.getAbilityType(), target.getKeyId());

        UserAiModelKey updated = requireOwnedRow(userId, target.getKeyId());
        log.info("设置用户默认模型密钥完成 userId={} keyId={}", userId, target.getKeyId());
        return new UserAiModelKeyItemResponse(describe(updated));
    }

    @Transactional
    public DeleteUserAiModelKeyResponse delete(UserAiModelKeyIdRequest req) {
        String userId = requireCurrentUser();
        if (req == null || req.getKeyId() == null || req.getKeyId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "key_id required");
        }
        registry.prepare(userId);
        UserAiModelKey existing = requireOwnedRow(userId, req.getKeyId());
        log.info("删除用户模型密钥 userId={} keyId={} abilityType={}",
                userId, existing.getKeyId(), existing.getAbilityType());

        var members = Boolean.TRUE.equals(req.getProviderModelSelection()) ? registry.models(userId).stream()
                .filter(row -> sameModel(row, existing)).toList() : List.of(existing);
        boolean wasDefault = false;
        for (var member : members) {
            wasDefault |= Integer.valueOf(1).equals(member.getDefaultModel());
            retire(member);
        }
        if (wasDefault) promoteFallbackDefault(userId, existing.getAbilityType(), existing.getKeyId());
        log.info("删除用户模型密钥完成 userId={} keyId={}", userId, existing.getKeyId());
        return new DeleteUserAiModelKeyResponse(1, existing.getKeyId());
    }

    public TestUserAiModelKeyResponse test(TestUserAiModelKeyRequest req) {
        String userId = requireCurrentUser();
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "request required");
        }
        UserAiModelKey saved = null;
        String rawCode;
        String rawBase;
        String plainKey;
        String plainSecret;
        String modelCode;
        if (req.getKeyId() != null && !req.getKeyId().isBlank()) {
            saved = requireOwnedRow(userId, req.getKeyId());
            rawCode = req.getProviderCode() != null && !req.getProviderCode().isBlank()
                    ? req.getProviderCode() : saved.getProviderCode();
            rawBase = req.getBaseUrl() != null && !req.getBaseUrl().isBlank()
                    ? req.getBaseUrl() : saved.getBaseUrl();
            plainKey = req.getApiKey() != null && !req.getApiKey().isBlank()
                    ? req.getApiKey().trim()
                    : saved.getApiKey();
            plainSecret = req.getSecretKey() != null && !req.getSecretKey().isBlank()
                    ? req.getSecretKey().trim()
                    : saved.getSecretKey();
            modelCode = req.getModelCode() != null && !req.getModelCode().isBlank()
                    ? req.getModelCode().trim()
                    : saved.getModelCode();
        } else {
            if (req.getApiKey() == null || req.getApiKey().isBlank()) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "api_key required");
            }
            rawCode = req.getProviderCode();
            rawBase = req.getBaseUrl();
            plainKey = req.getApiKey().trim();
            plainSecret = req.getSecretKey() == null ? null : req.getSecretKey().trim();
            modelCode = req.getModelCode() == null ? "" : req.getModelCode().trim();
        }
        UserAiModelSupport.OwnedProviderBinding bound = UserAiModelSupport.bindOwnedProvider(rawCode, rawBase);
        String providerCode = bound.providerCode();
        String baseUrl = bound.baseUrl();
        log.info("检测用户模型密钥连通性 userId={} keyId={} providerCode={} modelCode={}",
                userId, saved == null ? "-" : saved.getKeyId(), providerCode, modelCode);

        TestUserAiModelKeyResponse result = probeProviderConnectivity(
                providerCode, baseUrl, plainKey, plainSecret, modelCode);
        if (saved != null) {
            UserAiModelKey patch = new UserAiModelKey();
            patch.setId(saved.getId());
            patch.setLastCheckStatus(result.getStatus());
            patch.setLastCheckTime(new Date());
            if (!providerCode.equals(saved.getProviderCode())) {
                patch.setProviderCode(providerCode);
                patch.setBaseUrl(baseUrl);
            }
            patch.setModifier(userId);
            patch.setGmtModified(new Date());
            userAiModelKeyMapper.updateByPrimaryKeySelective(patch);
        }
        log.info("检测用户模型密钥连通性完成 userId={} status={} ok={}", userId, result.getStatus(), result.getOk());
        return result;
    }

    /**
     * Kept for older clients only. There is no exclusive mode anymore: enabled own-key
     * models stay selectable and everything else uses the BGSSAI built-in models, so
     * this always reports the mixed mode value 0 and persists nothing.
     */
    public UpdateUserAiKeyModeResponse updateMode(UpdateUserAiKeyModeRequest req) {
        String userId = requireCurrentUser();
        if (req == null || req.getOnlyUserKey() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "only_user_key required");
        }
        log.info("模型密钥模式切换请求按混合模式处理 userId={}", userId);
        return new UpdateUserAiKeyModeResponse(0);
    }

    public List<UserAiModelKey> listEnabledRows(String userId, String abilityType) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        try {
            UserAiModelKeyExample ex = new UserAiModelKeyExample();
            UserAiModelKeyExample.Criteria c = ex.createCriteria()
                    .andUserIdEqualTo(userId.trim())
                    .andEnabledEqualTo(1)
                    .andDelFlagEqualTo(0);
            if (abilityType != null && !abilityType.isBlank()) {
                c.andAbilityTypeEqualTo(abilityType.trim().toUpperCase(Locale.ROOT));
            }
            ex.setOrderByClause("is_default_model desc, id asc");
            List<UserAiModelKey> rows = userAiModelKeyMapper.selectByExample(ex);
            rows = retainSingleEnabled(userId.trim(), rows);
            for (UserAiModelKey row : rows) {
                migrateLegacyProvider(row);
            }
            return rows;
        } catch (DataAccessException ex) {
            log.warn("读取 user_ai_model_key 失败 userId={} abilityType={}", userId, abilityType);
            return Collections.emptyList();
        }
    }

    public AiModelConfig enabledUserModel(String userId, String abilityType) {
        if (userId == null || userId.isBlank() || abilityType == null || abilityType.isBlank()) {
            return null;
        }
        List<UserAiModelKey> rows = listEnabledRows(userId, abilityType);
        if (rows.isEmpty()) {
            return null;
        }
        return UserAiModelSupport.toTransientModelConfig(rows.get(0));
    }

    public Set<String> activePlatformAbilities(String userId) {
        Set<String> active = new LinkedHashSet<>(PLATFORM_ABILITIES);
        if (userId == null || userId.isBlank()) {
            return active;
        }
        String uid = userId.trim();
        for (UserAiModelKey row : listEnabledRows(uid, null)) {
            if (row.getAbilityType() != null) {
                active.remove(row.getAbilityType().trim().toUpperCase(Locale.ROOT));
            }
        }
        try {
            active.removeAll(readSuppressed(uid));
        } catch (DataAccessException ex) {
            log.warn("读取自建模型开关失败 userId={}", uid);
        }
        return active;
    }

    public AiModelConfig resolveUserModel(String userId, String abilityType, String requestedCode) {
        if (userId == null || userId.isBlank() || abilityType == null || abilityType.isBlank()) {
            return null;
        }
        String ability = abilityType.trim().toUpperCase(Locale.ROOT);
        if ("COMPOSE".equals(ability)) {
            return null;
        }
        List<UserAiModelKey> rows = listEnabledRows(userId, ability);
        String code = requestedCode == null ? "" : requestedCode.trim();
        if (UserAiModelSupport.isUserKeyCode(code)) {
            for (UserAiModelKey row : rows) {
                if (code.equalsIgnoreCase(row.getKeyId())) {
                    return UserAiModelSupport.toTransientModelConfig(row);
                }
            }
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    "所选自有密钥模型未启用或不存在，请到账号设置检查" + UserAiModelSupport.abilityLabel(ability) + "模型密钥");
        }
        if (!code.isBlank() && !isPlatformAlias(code)) {
            for (UserAiModelKey row : rows) {
                if (code.equalsIgnoreCase(row.getModelCode())) {
                    return UserAiModelSupport.toTransientModelConfig(row);
                }
            }
        }
        // No own-key model selected: the caller falls back to the BGSSAI built-in models.
        return null;
    }

    public void assertAbilityReady(String userId, String abilityType, String requestedCode) {
        if (userId == null || userId.isBlank()) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "请先登录后使用自有模型密钥");
        }
        AiModelConfig resolved = resolveUserModel(userId, abilityType, requestedCode);
        if (resolved == null) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                    "请先在账号设置配置并启用"
                            + UserAiModelSupport.abilityLabel(abilityType) + "模型密钥");
        }
    }

    public record UserKeyCredentials(String apiKey, String secretKey) {
    }

    public String requirePlainApiKey(String userId, String keyId) {
        return requirePlainCredentials(userId, keyId).apiKey();
    }

    public UserKeyCredentials requirePlainCredentials(String userId, String keyId) {
        if (userId == null || userId.isBlank() || keyId == null || keyId.isBlank()) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "自有模型密钥配置缺失");
        }
        UserAiModelKeyExample ex = new UserAiModelKeyExample();
        ex.createCriteria()
                .andUserIdEqualTo(userId.trim())
                .andKeyIdEqualTo(keyId.trim())
                .andEnabledEqualTo(1)
                .andDelFlagEqualTo(0);
        ex.selectColumns("id", "key_id", "api_key", "secret_key", "is_enabled", "del_flag");
        UserAiModelKey row = userAiModelKeyMapper.selectOneByExample(ex);
        if (row == null || row.getApiKey() == null || row.getApiKey().isBlank()) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "自有模型密钥已停用或不存在，请检查账号设置");
        }
        String sk = row.getSecretKey() == null ? "" : row.getSecretKey().trim();
        return new UserKeyCredentials(row.getApiKey().trim(), sk);
    }

    public String displayNameOfKeyId(String userId, String keyId) {
        if (!UserAiModelSupport.isUserKeyCode(keyId) || userId == null || userId.isBlank()) {
            return null;
        }
        try {
            UserAiModelKeyExample ex = new UserAiModelKeyExample();
            ex.createCriteria().andUserIdEqualTo(userId.trim()).andKeyIdEqualTo(keyId.trim());
            ex.selectColumns("id", "model_name", "model_code");
            UserAiModelKey row = userAiModelKeyMapper.selectOneByExample(ex);
            if (row == null) {
                return null;
            }
            return row.getModelName() != null && !row.getModelName().isBlank()
                    ? row.getModelName() : row.getModelCode();
        } catch (DataAccessException ex) {
            return null;
        }
    }

    private void claimAbility(String userId, String abilityType, String keepKeyId) {
        disableOtherUserKeys(userId, abilityType, keepKeyId);
    }

    private void selectPlatform(String userId, String ability) {
        disableOtherUserKeys(userId, ability, null);
        Set<String> suppressed = readSuppressed(userId);
        if (suppressed.remove(ability)) {
            saveSuppressed(userId, suppressed);
        }
    }

    private void suppressPlatform(String userId, String ability) {
        Set<String> suppressed = readSuppressed(userId);
        if (suppressed.add(ability)) {
            saveSuppressed(userId, suppressed);
        }
    }

    private void disableOtherUserKeys(String userId, String abilityType, String keepKeyId) {
        Date now = new Date();
        UserAiModelKey clear = new UserAiModelKey();
        clear.setEnabled(0);
        clear.setDefaultModel(0);
        clear.setModifier(userId);
        clear.setGmtModified(now);
        UserAiModelKeyExample ex = new UserAiModelKeyExample();
        UserAiModelKeyExample.Criteria criteria = ex.createCriteria()
                .andUserIdEqualTo(userId)
                .andAbilityTypeEqualTo(abilityType)
                .andDelFlagEqualTo(0);
        if (keepKeyId != null && !keepKeyId.isBlank()) {
            criteria.andKeyIdNotEqualTo(keepKeyId);
        }
        userAiModelKeyMapper.updateByExampleSelective(clear, ex);
        if (keepKeyId == null || keepKeyId.isBlank()) {
            return;
        }
        UserAiModelKey keep = new UserAiModelKey();
        keep.setEnabled(1);
        keep.setDefaultModel(1);
        keep.setModifier(userId);
        keep.setGmtModified(now);
        UserAiModelKeyExample keepEx = new UserAiModelKeyExample();
        keepEx.createCriteria()
                .andUserIdEqualTo(userId)
                .andKeyIdEqualTo(keepKeyId)
                .andDelFlagEqualTo(0);
        userAiModelKeyMapper.updateByExampleSelective(keep, keepEx);
    }

    private List<UserAiModelKey> retainSingleEnabled(String userId, List<UserAiModelKey> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, UserAiModelKey> keep = new LinkedHashMap<>();
        List<UserAiModelKey> drop = new ArrayList<>();
        for (UserAiModelKey row : rows) {
            String ability = row.getAbilityType() == null ? "" : row.getAbilityType();
            if (!keep.containsKey(ability)) {
                keep.put(ability, row);
            } else {
                drop.add(row);
            }
        }
        Date now = new Date();
        try {
            for (UserAiModelKey row : drop) {
                UserAiModelKey patch = new UserAiModelKey();
                patch.setId(row.getId());
                patch.setEnabled(0);
                patch.setDefaultModel(0);
                patch.setModifier(userId);
                patch.setGmtModified(now);
                userAiModelKeyMapper.updateByPrimaryKeySelective(patch);
            }
            for (UserAiModelKey row : keep.values()) {
                if (!Integer.valueOf(1).equals(row.getDefaultModel())) {
                    UserAiModelKey patch = new UserAiModelKey();
                    patch.setId(row.getId());
                    patch.setDefaultModel(1);
                    patch.setModifier(userId);
                    patch.setGmtModified(now);
                    userAiModelKeyMapper.updateByPrimaryKeySelective(patch);
                    row.setDefaultModel(1);
                }
            }
        } catch (DataAccessException ex) {
            log.warn("收口同类型启用模型失败 userId={}", userId);
        }
        return new ArrayList<>(keep.values());
    }

    private boolean platformRowVisible(String ability, String modalityType, String abilityType) {
        if (abilityType != null && !abilityType.isBlank()
                && !ability.equalsIgnoreCase(abilityType.trim())) {
            return false;
        }
        if (modalityType != null && !modalityType.isBlank()) {
            String modality = UserAiModelSupport.modalityForAbility(ability);
            return modality.equalsIgnoreCase(modalityType.trim());
        }
        return true;
    }

    private static int platformAbilityOrder(String ability) {
        int index = ability == null ? -1 : PLATFORM_ABILITIES.indexOf(ability.trim().toUpperCase(Locale.ROOT));
        return index < 0 ? PLATFORM_ABILITIES.size() : index;
    }

    private UserAiModelKeyItem platformItem(String ability, boolean enabled) {
        ProductModelAlias.Face face = ProductModelAlias.canonical(ability);
        UserAiModelKeyItem item = new UserAiModelKeyItem();
        item.setKeyId(face.code());
        item.setModalityType(UserAiModelSupport.modalityForAbility(ability));
        item.setAbilityType(ability);
        item.setProviderCode("BGSSAI");
        item.setProviderName("BGSSAI");
        item.setModelCode(face.code());
        item.setModelName(face.name());
        item.setEnabled(enabled ? 1 : 0);
        item.setDefaultModel(enabled ? 1 : 0);
        item.setPlatformModel(1);
        return item;
    }

    private UserAiSetting loadSetting(String userId) {
        UserAiSettingExample ex = new UserAiSettingExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(0);
        return userAiSettingMapper.selectOneByExample(ex);
    }

    private Set<String> readSuppressed(String userId) {
        UserAiSetting row = loadSetting(userId);
        Set<String> suppressed = new LinkedHashSet<>();
        if (row == null || row.getSuppressedPlatformAbilities() == null
                || row.getSuppressedPlatformAbilities().isBlank()) {
            return suppressed;
        }
        for (String part : row.getSuppressedPlatformAbilities().split(",")) {
            String ability = part.trim().toUpperCase(Locale.ROOT);
            if (PLATFORM_ABILITIES.contains(ability)) {
                suppressed.add(ability);
            }
        }
        return suppressed;
    }

    private void saveSuppressed(String userId, Set<String> suppressed) {
        String value = String.join(",", suppressed);
        UserAiSetting existing = loadSetting(userId);
        Date now = new Date();
        if (existing == null) {
            UserAiSetting row = new UserAiSetting();
            row.setUserId(userId);
            row.setOnlyUserKey(0);
            row.setSuppressedPlatformAbilities(value);
            row.setDelFlag(0);
            row.setCreator(userId);
            row.setModifier(userId);
            row.setGmtCreate(now);
            row.setGmtModified(now);
            userAiSettingMapper.insertSelective(row);
            return;
        }
        UserAiSetting patch = new UserAiSetting();
        patch.setId(existing.getId());
        patch.setSuppressedPlatformAbilities(value);
        patch.setModifier(userId);
        patch.setGmtModified(now);
        userAiSettingMapper.updateByPrimaryKeySelective(patch);
    }

    private void promoteFallbackDefault(String userId, String abilityType, String excludeKeyId) {
        UserAiModelKeyExample ex = new UserAiModelKeyExample();
        ex.createCriteria()
                .andUserIdEqualTo(userId)
                .andAbilityTypeEqualTo(abilityType)
                .andEnabledEqualTo(1)
                .andDelFlagEqualTo(0);
        ex.setOrderByClause("id asc");
        List<UserAiModelKey> rows = userAiModelKeyMapper.selectByExample(ex);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (UserAiModelKey candidate : rows) {
            if (!Objects.equals(candidate.getKeyId(), excludeKeyId)) {
                UserAiModelKey patch = new UserAiModelKey();
                patch.setId(candidate.getId());
                patch.setDefaultModel(1);
                patch.setModifier(userId);
                patch.setGmtModified(new Date());
                userAiModelKeyMapper.updateByPrimaryKeySelective(patch);
                return;
            }
        }
    }

    private UserAiModelKey requireOwnedRow(String userId, String keyId) {
        UserAiModelKeyExample ex = new UserAiModelKeyExample();
        ex.createCriteria()
                .andUserIdEqualTo(userId)
                .andKeyIdEqualTo(keyId.trim())
                .andDelFlagEqualTo(0);
        UserAiModelKey row = userAiModelKeyMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "模型密钥不存在");
        }
        return row;
    }

    private TestUserAiModelKeyResponse probeProviderConnectivity(String providerCode, String baseUrl,
            String plainKey, String plainSecret, String modelCode) {
        try {
            if (UserAiModelSupport.PROVIDER_MINIMAX.equals(providerCode)) {
                return probeMiniMax(baseUrl, plainKey);
            }
            if (UserAiModelSupport.PROVIDER_KLING.equals(providerCode)) {
                return probeKling(baseUrl, plainKey, plainSecret);
            }
            if (UserAiModelSupport.PROVIDER_PIXVERSE.equals(providerCode)) {
                return probePixVerse(baseUrl, plainKey);
            }
            String url = UserAiModelSupport.connectivityProbeUrl(providerCode, baseUrl);
            if (UserAiModelSupport.PROVIDER_ANTHROPIC.equals(providerCode)) {
                return probeAnthropicModels(url, plainKey, modelCode);
            }
            return probeBearerModels(url, plainKey, modelCode);
        } catch (BizException ex) {
            return failedProbe(ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failedProbe("连通检测已中断");
        } catch (Exception ex) {
            return failedProbe("无法连接到模型接口地址，请检查网络或地址");
        }
    }

    private TestUserAiModelKeyResponse probeAnthropicModels(String url, String plainKey, String modelCode)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("x-api-key", plainKey)
                .header("Authorization", "Bearer " + plainKey)
                .header("anthropic-version", "2023-06-01")
                .GET()
                .build();
        return readModelsProbe(request, modelCode);
    }

    private TestUserAiModelKeyResponse probeBearerModels(String url, String plainKey, String modelCode)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + plainKey)
                .GET()
                .build();
        return readModelsProbe(request, modelCode);
    }

    private TestUserAiModelKeyResponse readModelsProbe(HttpRequest request, String modelCode) throws Exception {
        HttpResponse<String> httpResp = probeHttp.send(request, HttpResponse.BodyHandlers.ofString());
        int status = httpResp.statusCode();
        if (status < 200 || status >= 300) {
            return failedHttpProbe(status, "");
        }
        List<String> discovered = extractDiscoveredModels(httpResp.body());
        boolean matched = modelCode == null || modelCode.isBlank() || discovered.isEmpty()
                || discovered.contains(modelCode);
        return okProbe(
                matched ? "连接成功" : "密钥验证通过，返回目录中未包含 " + modelCode + "，可按实际模型编码调整",
                matched, discovered);
    }

    private TestUserAiModelKeyResponse probeMiniMax(String baseUrl, String plainKey) throws Exception {
        String root = UserAiModelSupport.stripTrailingPath(baseUrl, "/v1");
        HttpRequest request = HttpRequest.newBuilder(URI.create(root + "/v1/get_voice"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + plainKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"voice_type\":\"system\"}", StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> httpResp = probeHttp.send(request, HttpResponse.BodyHandlers.ofString());
        int status = httpResp.statusCode();
        Map<String, Object> parsed = ShortJson.map(httpResp.body());
        if (status >= 200 && status < 300 && isMiniMaxRespOk(parsed)) {
            return okProbe("连接成功", true, Collections.emptyList());
        }
        String errMsg = nestedString(parsed, "base_resp", "status_msg");
        return failedHttpProbe(status, errMsg);
    }

    private TestUserAiModelKeyResponse probeKling(String baseUrl, String plainKey, String plainSecret)
            throws Exception {
        String root = UserAiModelSupport.stripTrailingPath(baseUrl, "/v1");
        String jwt = KlingJwtSupport.resolveBearerToken(plainKey, plainSecret, root);
        HttpRequest request = HttpRequest.newBuilder(URI.create(root + "/v1/videos/text2video?pageNum=1&pageSize=1"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();
        HttpResponse<String> httpResp = probeHttp.send(request, HttpResponse.BodyHandlers.ofString());
        int status = httpResp.statusCode();
        Map<String, Object> parsed = ShortJson.map(httpResp.body());
        Object code = parsed.get("code");
        if (status >= 200 && status < 300 && (code == null || ShortJson.integer(code, -1) == 0)) {
            return okProbe("连接成功", true, Collections.emptyList());
        }
        return failedHttpProbe(status, ShortJson.text(parsed.get("message")));
    }

    private TestUserAiModelKeyResponse probePixVerse(String baseUrl, String plainKey) throws Exception {
        String root = UserAiModelSupport.stripTrailingPath(baseUrl, "/openapi/v2");
        HttpRequest request = HttpRequest.newBuilder(URI.create(root + "/openapi/v2/account/balance"))
                .timeout(Duration.ofSeconds(10))
                .header("API-KEY", plainKey)
                .header("Ai-trace-id", UUID.randomUUID().toString())
                .GET()
                .build();
        HttpResponse<String> httpResp = probeHttp.send(request, HttpResponse.BodyHandlers.ofString());
        int status = httpResp.statusCode();
        Map<String, Object> parsed = ShortJson.map(httpResp.body());
        Object errCode = parsed.get("ErrCode");
        if (status >= 200 && status < 300 && (errCode == null || ShortJson.integer(errCode, -1) == 0)) {
            return okProbe("连接成功", true, Collections.emptyList());
        }
        return failedHttpProbe(status, ShortJson.text(parsed.get("ErrMsg")));
    }

    private static boolean isMiniMaxRespOk(Map<String, Object> parsed) {
        Object raw = parsed.get("base_resp");
        if (!(raw instanceof Map<?, ?> base)) {
            return true;
        }
        Object code = base.get("status_code");
        return code == null || ShortJson.integer(code, -1) == 0;
    }

    private static String nestedString(Map<String, Object> parsed, String outer, String inner) {
        Object raw = parsed.get(outer);
        if (raw instanceof Map<?, ?> map) {
            return ShortJson.text(map.get(inner));
        }
        return "";
    }

    private static TestUserAiModelKeyResponse okProbe(String message, boolean matched, List<String> discovered) {
        TestUserAiModelKeyResponse resp = new TestUserAiModelKeyResponse();
        resp.setOk(true);
        resp.setStatus("OK");
        resp.setModelMatched(matched);
        resp.setDiscoveredModels(discovered);
        resp.setMessage(message);
        return resp;
    }

    private static TestUserAiModelKeyResponse failedHttpProbe(int status, String upstreamMsg) {
        if (status == 401 || status == 403) {
            return failedProbe("API Key 无效或无访问权限（HTTP " + status + "）");
        }
        if (status == 429) {
            return failedProbe("请求过于频繁或账号额度受限（HTTP 429）");
        }
        if (upstreamMsg != null && !upstreamMsg.isBlank()) {
            return failedProbe("上游认证未通过：" + upstreamMsg);
        }
        return failedProbe("上游网关返回 HTTP " + status);
    }

    private static TestUserAiModelKeyResponse failedProbe(String message) {
        TestUserAiModelKeyResponse resp = new TestUserAiModelKeyResponse();
        resp.setOk(false);
        resp.setStatus("FAILED");
        resp.setModelMatched(false);
        resp.setDiscoveredModels(Collections.emptyList());
        resp.setMessage(message);
        return resp;
    }

    private static List<String> extractDiscoveredModels(String jsonBody) {
        if (jsonBody == null || jsonBody.isBlank()) {
            return Collections.emptyList();
        }
        Map<String, Object> parsed = ShortJson.map(jsonBody);
        Object raw = parsed.get("data");
        if (!(raw instanceof List<?> list)) {
            raw = parsed.get("models");
        }
        if (!(raw instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                String id = ShortJson.text(map.get("id"));
                if (!id.isBlank()) {
                    ids.add(id);
                }
            }
            if (ids.size() >= 100) {
                break;
            }
        }
        return ids;
    }

    private static boolean isPlatformAlias(String code) {
        String lower = code.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("bgssai-") || "ffmpeg-compose-v1".equals(lower);
    }

    private void migrateLegacyProvider(UserAiModelKey row) {
        if (row == null || row.getProviderCode() == null) {
            return;
        }
        String canonical = UserAiModelSupport.canonicalizeOwnedProvider(row.getProviderCode(), row.getBaseUrl());
        if (canonical.equals(row.getProviderCode())) {
            return;
        }
        row.setProviderCode(canonical);
        if (row.getId() == null) {
            return;
        }
        try {
            UserAiModelKey patch = new UserAiModelKey();
            patch.setId(row.getId());
            patch.setProviderCode(canonical);
            patch.setModifier(row.getUserId());
            patch.setGmtModified(new Date());
            userAiModelKeyMapper.updateByPrimaryKeySelective(patch);
            log.info("迁移用户模型供应商编码 keyId={} providerCode={}", row.getKeyId(), canonical);
        } catch (RuntimeException ex) {
            log.warn("迁移用户模型供应商编码失败 keyId={}", row.getKeyId());
        }
    }

    private UserAiModelKeyItem describe(UserAiModelKey row) {
        UserAiModelKeyItem item = toItem(row);
        externalApiKeyBalanceClient.fill(List.of(row), List.of(item));
        return item;
    }

    private static String normalizeKeyName(String name) {
        if (name == null) return null;
        String trimmed = name.strip();
        if (trimmed.codePointCount(0, trimmed.length()) > 64) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "密钥名称最多 64 个字符");
        }
        return trimmed;
    }

    @Transactional
    public UserAiModelKeyItem createCredential(SavedCredentialRequest req) {
        String user = requireCurrentUser();
        var saved = registry.prepare(user);
        var row = credentialInput(user, req, null);
        boolean first = saved.stream().noneMatch(c -> c.getProviderCode().equals(row.getProviderCode()));
        return credentialItem(registry.insert(row, first));
    }

    private UserAiSavedCredential credentialInput(String user, SavedCredentialRequest req, UserAiSavedCredential old) {
        String requested = req.getProviderCode();
        if (old == null && (requested == null || requested.isBlank())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "provider_code required");
        }
        String provider = old == null || requested != null
                ? UserAiModelSupport.normalizeProvider(requested) : old.getProviderCode();
        if (old != null && !old.getProviderCode().equals(provider)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "密钥供应商不能更改");
        }
        String base = old != null && req.getBaseUrl() == null ? old.getBaseUrl() : req.getBaseUrl();
        var row = new UserAiSavedCredential();
        row.setUserId(user);
        row.setProviderCode(provider);
        row.setBaseUrl(UserAiModelSupport.normalizeBaseUrl(base, provider));
        String name = normalizeKeyName(req.getKeyName() == null && old != null ? old.getKeyName() : req.getKeyName());
        row.setKeyName(name == null || name.isEmpty() ? null : name);
        row.setApiKey(secretValue(req.getApiKey(), old == null ? null : old.getApiKey()));
        row.setSecretKey(secretValue(req.getSecretKey(), old == null ? null : old.getSecretKey()));
        if (row.getApiKey() == null) throw new BizException(CommonErrorCode.INVALID_PARAM, "api_key required");
        if (UserAiModelSupport.PROVIDER_KLING.equals(row.getProviderCode())) {
            KlingJwtSupport.resolveBearerToken(row.getApiKey(), row.getSecretKey(), row.getBaseUrl());
        }
        return row;
    }

    private static String secretValue(String value, String retained) {
        if (value == null || value.isBlank()) return retained;
        String trimmed = value.trim();
        if (trimmed.equals(UserAiModelSupport.maskApiKey(retained)) || trimmed.contains("****")) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请输入完整密钥，留空可保留原值");
        }
        if (trimmed.length() > 512) throw new BizException(CommonErrorCode.INVALID_PARAM, "密钥最多 512 个字符");
        return trimmed;
    }

    @Transactional
    public UserAiModelKeyItem activateCredential(SavedCredentialRequest req) {
        String user = requireCurrentUser();
        var saved = registry.prepare(user);
        var chosen = SavedCredentialRegistry.require(saved, req.getCredentialId());
        savedCredentialMapper.clearActive(user, chosen.getProviderCode());
        chosen.setActive(1);
        savedCredentialMapper.update(chosen);
        for (var model : registry.models(user)) {
            if (chosen.getProviderCode().equals(model.getProviderCode()) && !chosen.getCredentialId().equals(model.getCredentialId())) {
                replaceBinding(model, chosen);
            }
        }
        return credentialItem(chosen);
    }

    @Transactional
    public UserAiModelKeyItem updateCredential(SavedCredentialRequest req) {
        String user = requireCurrentUser();
        var old = SavedCredentialRegistry.require(registry.prepare(user), req.getCredentialId());
        var replacement = credentialInput(user, req, old);
        replacement.setCredentialId(old.getCredentialId());
        replacement.setActive(old.getActive());
        boolean rotated = !SavedCredentialRegistry.sameConnection(replacement, old);
        if (!rotated) {
            replacement.setLastCheckStatus(old.getLastCheckStatus());
            replacement.setLastCheckTime(old.getLastCheckTime());
        }
        savedCredentialMapper.update(replacement);
        for (var model : registry.models(user)) {
            if (!old.getCredentialId().equals(model.getCredentialId())) continue;
            if (rotated) {
                replaceBinding(model, replacement);
            } else {
                var patch = new UserAiModelKey();
                patch.setId(model.getId());
                patch.setKeyName(replacement.getKeyName() == null ? "" : replacement.getKeyName());
                patch.setModifier(user);
                patch.setGmtModified(new Date());
                userAiModelKeyMapper.updateByPrimaryKeySelective(patch);
            }
        }
        return credentialItem(replacement);
    }

    @Transactional
    public boolean deleteCredential(SavedCredentialRequest req) {
        String user = requireCurrentUser();
        var credential = SavedCredentialRegistry.require(registry.prepare(user), req.getCredentialId());
        if (registry.models(user).stream().anyMatch(m -> credential.getCredentialId().equals(m.getCredentialId()))) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "仍有模型使用这把密钥，请先切换该供应商密钥或移除关联模型");
        }
        savedCredentialMapper.delete(user, credential.getCredentialId());
        return true;
    }

    public TestUserAiModelKeyResponse testCredential(SavedCredentialRequest req) {
        String user = requireCurrentUser();
        var credential = SavedCredentialRegistry.require(registry.prepare(user), req.getCredentialId());
        var result = probeProviderConnectivity(credential.getProviderCode(), credential.getBaseUrl(),
                credential.getApiKey(), credential.getSecretKey(), "");
        savedCredentialMapper.recordCheck(credential, result.getStatus(), new Date());
        return result;
    }

    private static boolean sameModel(UserAiModelKey left, UserAiModelKey right) {
        return Objects.equals(left.getAbilityType(), right.getAbilityType())
                && Objects.equals(left.getModelCode(), right.getModelCode())
                && Objects.equals(left.getProviderCode(), right.getProviderCode());
    }

    // A queued job refers to the exact umk id. Never overwrite its connection to a new credential.
    private UserAiModelKey replaceBinding(UserAiModelKey previous, UserAiSavedCredential credential) {
        var replacement = registry.models(previous.getUserId()).stream()
                .filter(row -> !row.getKeyId().equals(previous.getKeyId()) && sameModel(row, previous)
                        && credential.getCredentialId().equals(row.getCredentialId())
                        && SavedCredentialRegistry.sameConnection(row, credential))
                .findFirst().orElse(null);
        if (replacement == null) {
            replacement = new UserAiModelKey();
            replacement.setUserId(previous.getUserId());
            replacement.setKeyId(ShortIds.id(UserAiModelSupport.USER_KEY_PREFIX));
            replacement.setModalityType(previous.getModalityType());
            replacement.setAbilityType(previous.getAbilityType());
            replacement.setProviderCode(credential.getProviderCode());
            replacement.setModelCode(previous.getModelCode());
            replacement.setModelName(previous.getModelName());
            replacement.setCredentialId(credential.getCredentialId());
            replacement.setKeyName(credential.getKeyName());
            replacement.setBaseUrl(credential.getBaseUrl());
            replacement.setApiKey(credential.getApiKey());
            replacement.setSecretKey(credential.getSecretKey());
            replacement.setEnabled(0);
            replacement.setDefaultModel(0);
            replacement.setDelFlag(0);
            replacement.setCreator(previous.getUserId());
            replacement.setModifier(previous.getUserId());
            replacement.setGmtCreate(new Date());
            replacement.setGmtModified(new Date());
            userAiModelKeyMapper.insertSelective(replacement);
        }
        retire(previous);
        if (Integer.valueOf(1).equals(previous.getEnabled())) {
            claimAbility(previous.getUserId(), previous.getAbilityType(), replacement.getKeyId());
        }
        return replacement;
    }

    private void retire(UserAiModelKey model) {
        var patch = new UserAiModelKey();
        patch.setId(model.getId());
        patch.setApiKey("");
        patch.setSecretKey("");
        patch.setEnabled(0);
        patch.setDefaultModel(0);
        patch.setDelFlag(1);
        patch.setModifier(model.getUserId());
        patch.setGmtModified(new Date());
        userAiModelKeyMapper.updateByPrimaryKeySelective(patch);
    }

    private static UserAiModelKeyItem credentialItem(UserAiSavedCredential credential) {
        var item = credentialFields(credential);
        item.setActive(Integer.valueOf(1).equals(credential.getActive()));
        return item;
    }

    private static UserAiModelKeyItem toItem(UserAiModelKey row) {
        var item = credentialFields(row);
        item.setKeyId(row.getKeyId());
        item.setModalityType(row.getModalityType());
        item.setAbilityType(row.getAbilityType());
        item.setModelCode(row.getModelCode());
        item.setModelName(row.getModelName());
        item.setEnabled(row.getEnabled());
        item.setDefaultModel(row.getDefaultModel());
        return item;
    }

    private static UserAiModelKeyItem credentialFields(UserAiCredentialData row) {
        var item = new UserAiModelKeyItem();
        String providerCode = UserAiModelSupport.canonicalizeOwnedProvider(row.getProviderCode(), row.getBaseUrl());
        item.setCredentialId(row.getCredentialId());
        item.setProviderCode(providerCode);
        item.setProviderName(UserAiModelSupport.providerName(providerCode));
        item.setKeyName(row.getKeyName());
        item.setBaseUrl(row.getBaseUrl());
        item.setApiKeyMasked(UserAiModelSupport.maskApiKey(row.getApiKey()));
        item.setSecretKeyMasked(UserAiModelSupport.maskApiKey(row.getSecretKey()));
        item.setLastCheckStatus(row.getLastCheckStatus());
        item.setLastCheckTime(row.getLastCheckTime());
        return item;
    }
}
