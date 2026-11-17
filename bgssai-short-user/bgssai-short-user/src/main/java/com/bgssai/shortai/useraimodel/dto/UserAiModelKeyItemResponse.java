package com.bgssai.shortai.useraimodel.dto;

public class UserAiModelKeyItemResponse {
    private UserAiModelKeyItem item;

    public UserAiModelKeyItemResponse() {
    }

    public UserAiModelKeyItemResponse(UserAiModelKeyItem item) {
        this.item = item;
    }

    public UserAiModelKeyItem getItem() {
        return item;
    }

    public void setItem(UserAiModelKeyItem item) {
        this.item = item;
    }
}
