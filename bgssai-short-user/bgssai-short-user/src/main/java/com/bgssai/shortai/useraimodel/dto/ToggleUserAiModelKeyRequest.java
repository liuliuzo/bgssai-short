package com.bgssai.shortai.useraimodel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ToggleUserAiModelKeyRequest {
    @NotBlank(message = "key_id required")
    private String keyId;
    @NotNull(message = "enabled required")
    private Integer enabled;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
