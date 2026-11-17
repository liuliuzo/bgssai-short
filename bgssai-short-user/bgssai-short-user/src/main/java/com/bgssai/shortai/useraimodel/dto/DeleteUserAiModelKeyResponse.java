package com.bgssai.shortai.useraimodel.dto;

public class DeleteUserAiModelKeyResponse {
    private Integer deleted;
    private String keyId;

    public DeleteUserAiModelKeyResponse() {
    }

    public DeleteUserAiModelKeyResponse(Integer deleted, String keyId) {
        this.deleted = deleted;
        this.keyId = keyId;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
}
