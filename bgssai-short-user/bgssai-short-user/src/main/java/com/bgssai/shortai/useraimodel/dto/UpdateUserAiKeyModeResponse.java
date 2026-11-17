package com.bgssai.shortai.useraimodel.dto;

public class UpdateUserAiKeyModeResponse {
    private Integer onlyUserKey;

    public UpdateUserAiKeyModeResponse() {
    }

    public UpdateUserAiKeyModeResponse(Integer onlyUserKey) {
        this.onlyUserKey = onlyUserKey;
    }

    public Integer getOnlyUserKey() {
        return onlyUserKey;
    }

    public void setOnlyUserKey(Integer onlyUserKey) {
        this.onlyUserKey = onlyUserKey;
    }
}
