package com.bgssai.shortai.useraimodel.dto;

import java.util.List;

public class CreateUserAiModelKeyResponse {
    private Integer onlyUserKey;
    private Integer createdCount;
    private List<UserAiModelKeyItem> keyList;

    public Integer getOnlyUserKey() {
        return onlyUserKey;
    }

    public void setOnlyUserKey(Integer onlyUserKey) {
        this.onlyUserKey = onlyUserKey;
    }

    public Integer getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Integer createdCount) {
        this.createdCount = createdCount;
    }

    public List<UserAiModelKeyItem> getKeyList() {
        return keyList;
    }

    public void setKeyList(List<UserAiModelKeyItem> keyList) {
        this.keyList = keyList;
    }
}
