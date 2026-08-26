package com.bgssai.shortai.aimodel.dto;

public class ReqAiModel {

    private String modelId;
    private String abilityType;
    private String providerCode;
    private String modelCode;
    private String modelName;
    private String qualityLevel;
    private String priceUnit;
    private Integer unitPriceCent;
    private Integer defaultFlag;
    private Integer enabledFlag;
    private Integer timeoutSec;
    private Integer maxRetry;
    private String configJson;

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getAbilityType() { return abilityType; }
    public void setAbilityType(String abilityType) { this.abilityType = abilityType; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public Integer getUnitPriceCent() { return unitPriceCent; }
    public void setUnitPriceCent(Integer unitPriceCent) { this.unitPriceCent = unitPriceCent; }
    public Integer getDefaultFlag() { return defaultFlag; }
    public void setDefaultFlag(Integer defaultFlag) { this.defaultFlag = defaultFlag; }
    public Integer getEnabledFlag() { return enabledFlag; }
    public void setEnabledFlag(Integer enabledFlag) { this.enabledFlag = enabledFlag; }
    public Integer getTimeoutSec() { return timeoutSec; }
    public void setTimeoutSec(Integer timeoutSec) { this.timeoutSec = timeoutSec; }
    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
}
