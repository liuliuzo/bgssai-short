package com.bgssai.shortai.production.dto;

public class ToggleCreatorReq {

    private String userId;
    private Integer enabledFlag;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getEnabledFlag() {
        return enabledFlag;
    }

    public void setEnabledFlag(Integer enabledFlag) {
        this.enabledFlag = enabledFlag;
    }
}
