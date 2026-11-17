package com.bgssai.shortai.useraimodel.dto;

import java.util.Date;

/**
 * 用户自有厂商 API Key 脱敏视图。
 */
public class UserAiProviderKeyItem {
    private String providerCode;
    private String providerName;
    private String defaultBaseUrl;
    private boolean supportsAkSk;
    private String authMode;
    private boolean configured;
    private boolean enabled;
    private String maskedKey;
    private String maskedAccessKey;
    private boolean hasSecretKey;
    private String baseUrl;
    private Date updatedAt;

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getDefaultBaseUrl() { return defaultBaseUrl; }
    public void setDefaultBaseUrl(String defaultBaseUrl) { this.defaultBaseUrl = defaultBaseUrl; }

    public boolean isSupportsAkSk() { return supportsAkSk; }
    public void setSupportsAkSk(boolean supportsAkSk) { this.supportsAkSk = supportsAkSk; }

    public String getAuthMode() { return authMode; }
    public void setAuthMode(String authMode) { this.authMode = authMode; }

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getMaskedKey() { return maskedKey; }
    public void setMaskedKey(String maskedKey) { this.maskedKey = maskedKey; }

    public String getMaskedAccessKey() { return maskedAccessKey; }
    public void setMaskedAccessKey(String maskedAccessKey) { this.maskedAccessKey = maskedAccessKey; }

    public boolean isHasSecretKey() { return hasSecretKey; }
    public void setHasSecretKey(boolean hasSecretKey) { this.hasSecretKey = hasSecretKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
