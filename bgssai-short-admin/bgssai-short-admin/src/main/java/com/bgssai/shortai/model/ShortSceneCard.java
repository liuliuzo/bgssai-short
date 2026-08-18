package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortSceneCard implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String sceneCardId;

    private String workId;

    private String userId;

    private String cardType;

    private String cardName;

    private String cardStatus;

    private String appearancePrompt;

    private String refImageAssetId;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSceneCardId() {
        return sceneCardId;
    }

    public void setSceneCardId(String sceneCardId) {
        this.sceneCardId = sceneCardId == null ? null : sceneCardId.trim();
    }

    public String getWorkId() {
        return workId;
    }

    public void setWorkId(String workId) {
        this.workId = workId == null ? null : workId.trim();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType == null ? null : cardType.trim();
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName == null ? null : cardName.trim();
    }

    public String getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(String cardStatus) {
        this.cardStatus = cardStatus == null ? null : cardStatus.trim();
    }

    public String getAppearancePrompt() {
        return appearancePrompt;
    }

    public void setAppearancePrompt(String appearancePrompt) {
        this.appearancePrompt = appearancePrompt == null ? null : appearancePrompt.trim();
    }

    public String getRefImageAssetId() {
        return refImageAssetId;
    }

    public void setRefImageAssetId(String refImageAssetId) {
        this.refImageAssetId = refImageAssetId == null ? null : refImageAssetId.trim();
    }

    public Integer getDeletedFlag() {
        return deletedFlag;
    }

    public void setDeletedFlag(Integer deletedFlag) {
        this.deletedFlag = deletedFlag;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

}
