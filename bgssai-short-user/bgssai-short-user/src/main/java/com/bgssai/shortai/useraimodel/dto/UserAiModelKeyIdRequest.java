package com.bgssai.shortai.useraimodel.dto;

import jakarta.validation.constraints.NotBlank;

public class UserAiModelKeyIdRequest {
    @NotBlank(message = "key_id required")
    private String keyId;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
}
