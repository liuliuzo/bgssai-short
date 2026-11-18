package com.bgssai.shortai.model;

public class UserAiModelKey extends UserAiCredentialData {
    private String keyId;
    private String modalityType;
    private String abilityType;
    private String modelCode;
    private String modelName;
    private Integer enabled;
    private Integer defaultModel;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId == null ? null : keyId.trim();
    }

    public String getModalityType() {
        return modalityType;
    }

    public void setModalityType(String modalityType) {
        this.modalityType = modalityType == null ? null : modalityType.trim();
    }

    public String getAbilityType() {
        return abilityType;
    }

    public void setAbilityType(String abilityType) {
        this.abilityType = abilityType == null ? null : abilityType.trim();
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode == null ? null : modelCode.trim();
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName == null ? null : modelName.trim();
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
}
