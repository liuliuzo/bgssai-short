package com.bgssai.shortai.useraimodel.dto;

import java.util.Date;
import java.util.List;

public class UserAiModelKeyItem {
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    private String credentialId;

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    private String keyId;
    private String modalityType;
    private String abilityType;
    private String providerCode;
    private String providerName;
    private String modelCode;
    private String modelName;
    private String keyName;
    private String baseUrl;
    private String apiKeyMasked;
    private String secretKeyMasked;
    private Integer enabled;
    private Integer defaultModel;
    private Integer platformModel;
    private String lastCheckStatus;
    private Date lastCheckTime;
    private String balanceStatus;
    private List<ExternalApiKeyBalancePart> balanceList;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getModalityType() {
        return modalityType;
    }

    public void setModalityType(String modalityType) {
        this.modalityType = modalityType;
    }

    public String getAbilityType() {
        return abilityType;
    }

    public void setAbilityType(String abilityType) {
        this.abilityType = abilityType;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKeyMasked() {
        return apiKeyMasked;
    }

    public void setApiKeyMasked(String apiKeyMasked) {
        this.apiKeyMasked = apiKeyMasked;
    }

    public String getSecretKeyMasked() {
        return secretKeyMasked;
    }

    public void setSecretKeyMasked(String secretKeyMasked) {
        this.secretKeyMasked = secretKeyMasked;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(Integer defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Integer getPlatformModel() {
        return platformModel;
    }

    public void setPlatformModel(Integer platformModel) {
        this.platformModel = platformModel;
    }

    public String getLastCheckStatus() {
        return lastCheckStatus;
    }

    public void setLastCheckStatus(String lastCheckStatus) {
        this.lastCheckStatus = lastCheckStatus;
    }

    public Date getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(Date lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

    public String getBalanceStatus() {
        return balanceStatus;
    }

    public void setBalanceStatus(String balanceStatus) {
        this.balanceStatus = balanceStatus;
    }

    public List<ExternalApiKeyBalancePart> getBalanceList() {
        return balanceList;
    }

    public void setBalanceList(List<ExternalApiKeyBalancePart> balanceList) {
        this.balanceList = balanceList;
    }
}
