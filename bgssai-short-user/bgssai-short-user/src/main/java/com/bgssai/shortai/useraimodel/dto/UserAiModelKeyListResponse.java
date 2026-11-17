package com.bgssai.shortai.useraimodel.dto;

import java.util.List;

public class UserAiModelKeyListResponse {
    private Integer onlyUserKey;
    private List<UserAiModelKeyItem> keyList;
    private List<ProviderPresetItem> providerPresetList;

    public Integer getOnlyUserKey() {
        return onlyUserKey;
    }

    public void setOnlyUserKey(Integer onlyUserKey) {
        this.onlyUserKey = onlyUserKey;
    }

    public List<UserAiModelKeyItem> getKeyList() {
        return keyList;
    }

    public void setKeyList(List<UserAiModelKeyItem> keyList) {
        this.keyList = keyList;
    }

    public List<ProviderPresetItem> getProviderPresetList() {
        return providerPresetList;
    }

    public void setProviderPresetList(List<ProviderPresetItem> providerPresetList) {
        this.providerPresetList = providerPresetList;
    }
}
