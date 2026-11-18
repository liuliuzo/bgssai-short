package com.bgssai.shortai.useraimodel.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.UserAiModelKeyMapper;
import com.bgssai.shortai.mapper.UserAiSavedCredentialMapper;
import com.bgssai.shortai.model.UserAiCredentialData;
import com.bgssai.shortai.model.UserAiModelKey;
import com.bgssai.shortai.model.UserAiSavedCredential;
import com.bgssai.shortai.model.UserAiSavedCredentialExample;
import com.bgssai.shortai.model.UserAiModelKeyExample;
import com.bgssai.shortai.production.support.ShortIds;

@Service
public class SavedCredentialRegistry {
    private final UserAiSavedCredentialMapper credentials;
    private final UserAiModelKeyMapper models;

    public SavedCredentialRegistry(UserAiSavedCredentialMapper credentials, UserAiModelKeyMapper models) {
        this.credentials = credentials;
        this.models = models;
    }

    List<UserAiModelKey> models(String userId) {
        var example = new UserAiModelKeyExample();
        example.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(0);
        example.setOrderByClause("id asc");
        return models.selectByExample(example);
    }

    // This short transaction can also join the caller's mutation transaction.
    @Transactional
    public List<UserAiSavedCredential> prepare(String userId) {
        credentials.lockOwner(userId);
        List<UserAiSavedCredential> saved = new ArrayList<>(credentials.selectByExample(new UserAiSavedCredentialExample(userId)));
        var rows = models(userId);
        Set<String> imported = new LinkedHashSet<>();
        for (var row : rows) {
            if (row.getCredentialId() != null) continue;
            String provider = UserAiModelSupport.canonicalizeOwnedProvider(row.getProviderCode(), row.getBaseUrl());
            row.setProviderCode(provider);
            var credential = findMatching(saved, row);
            if (credential == null) {
                credential = insert(row, false);
                saved.add(credential);
            }
            var patch = new UserAiModelKey();
            patch.setId(row.getId());
            patch.setProviderCode(provider);
            patch.setCredentialId(credential.getCredentialId());
            models.updateByPrimaryKeySelective(patch);
            row.setCredentialId(credential.getCredentialId());
            imported.add(provider);
        }
        for (String provider : imported) {
            inferSelection(userId, provider, rows, saved);
        }
        return saved;
    }

    void inferSelection(String userId, String provider, List<UserAiModelKey> rows, List<UserAiSavedCredential> saved) {
        var candidates = saved.stream().filter(c -> provider.equals(c.getProviderCode())).toList();
        var selected = rows.stream().filter(r -> provider.equals(r.getProviderCode()) && Integer.valueOf(1).equals(r.getEnabled()))
                .map(UserAiModelKey::getCredentialId).distinct().toList();
        String winner = selected.size() == 1 ? selected.getFirst()
                : selected.isEmpty() && candidates.size() == 1 ? candidates.getFirst().getCredentialId() : null;
        credentials.clearActive(userId, provider);
        for (var item : candidates) {
            item.setActive(null);
            if (item.getCredentialId().equals(winner)) {
                item.setActive(1);
                credentials.update(item);
            }
        }
    }

    static UserAiSavedCredential require(List<UserAiSavedCredential> saved, String id) {
        return saved.stream().filter(c -> c.getCredentialId().equals(id)).findFirst()
                .orElseThrow(() -> new BizException(CommonErrorCode.INVALID_PARAM, "密钥不存在"));
    }

    static UserAiSavedCredential active(List<UserAiSavedCredential> saved, String provider) {
        return saved.stream().filter(c -> provider.equals(c.getProviderCode()) && Integer.valueOf(1).equals(c.getActive()))
                .findFirst().orElseThrow(() -> new BizException(CommonErrorCode.RULE_REJECT, "请先在密钥管理中选择该供应商的当前密钥"));
    }

    static UserAiSavedCredential findMatching(List<UserAiSavedCredential> saved, UserAiCredentialData row) {
        return saved.stream().filter(c -> c.getProviderCode().equals(row.getProviderCode())
                && Objects.equals(c.getKeyName(), row.getKeyName()) && sameConnection(c, row)).findFirst().orElse(null);
    }

    static boolean sameConnection(UserAiCredentialData a, UserAiCredentialData b) {
        return Objects.equals(a.getBaseUrl(), b.getBaseUrl()) && Objects.equals(a.getApiKey(), b.getApiKey())
                && Objects.equals(a.getSecretKey(), b.getSecretKey());
    }

    UserAiSavedCredential insert(UserAiCredentialData source, boolean active) {
        var credential = new UserAiSavedCredential();
        credential.setCredentialId(ShortIds.id("cred-"));
        credential.setUserId(source.getUserId());
        credential.setProviderCode(source.getProviderCode());
        credential.setKeyName(source.getKeyName());
        credential.setBaseUrl(source.getBaseUrl());
        credential.setApiKey(source.getApiKey());
        credential.setSecretKey(source.getSecretKey());
        credential.setLastCheckStatus(source.getLastCheckStatus());
        credential.setLastCheckTime(source.getLastCheckTime());
        credential.setActive(active ? 1 : null);
        credentials.insert(credential);
        return credential;
    }
}
