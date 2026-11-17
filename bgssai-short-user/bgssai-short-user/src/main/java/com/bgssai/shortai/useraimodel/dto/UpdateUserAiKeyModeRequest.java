package com.bgssai.shortai.useraimodel.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateUserAiKeyModeRequest {
    @NotNull(message = "only_user_key required")
    private Integer onlyUserKey;

    public Integer getOnlyUserKey() {
        return onlyUserKey;
    }

    public void setOnlyUserKey(Integer onlyUserKey) {
        this.onlyUserKey = onlyUserKey;
    }
}
