package com.bgssai.shortai.useraimodel.dto;

import java.util.List;

public class ProviderPresetItem {
    private String providerCode;
    private String providerName;
    private String authType;
    private String defaultBaseUrl;
    private List<ProviderAbilityPreset> abilities;

    public ProviderPresetItem() {
    }

    public ProviderPresetItem(String providerCode, String providerName, String authType,
            String defaultBaseUrl, List<ProviderAbilityPreset> abilities) {
        this.providerCode = providerCode;
        this.providerName = providerName;
        this.authType = authType;
        this.defaultBaseUrl = defaultBaseUrl;
        this.abilities = abilities;
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

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public void setDefaultBaseUrl(String defaultBaseUrl) {
        this.defaultBaseUrl = defaultBaseUrl;
    }

    public List<ProviderAbilityPreset> getAbilities() {
        return abilities;
    }

    public void setAbilities(List<ProviderAbilityPreset> abilities) {
        this.abilities = abilities;
    }
}
