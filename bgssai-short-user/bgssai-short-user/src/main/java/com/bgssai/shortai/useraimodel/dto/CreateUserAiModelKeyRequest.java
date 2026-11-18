package com.bgssai.shortai.useraimodel.dto;

import java.util.List;

public class CreateUserAiModelKeyRequest {
    private String providerCode;
    private String baseUrl;
    private String apiKey;
    private String secretKey;
    private String modalityType;
    private String abilityType;
    private String modelCode;
    private String modelName;
    private String keyName;
    private Integer onlyUserKey;
    private Boolean activateOnCreate;
    private List<CreateUserAiModelKeyItem> items;

    public Boolean getActivateOnCreate() {
        return activateOnCreate;
    }

    public void setActivateOnCreate(Boolean activateOnCreate) {
        this.activateOnCreate = activateOnCreate;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
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

    public Integer getOnlyUserKey() {
        return onlyUserKey;
    }

    public void setOnlyUserKey(Integer onlyUserKey) {
        this.onlyUserKey = onlyUserKey;
    }

    public List<CreateUserAiModelKeyItem> getItems() {
        return items;
    }

    public void setItems(List<CreateUserAiModelKeyItem> items) {
        this.items = items;
    }
}
