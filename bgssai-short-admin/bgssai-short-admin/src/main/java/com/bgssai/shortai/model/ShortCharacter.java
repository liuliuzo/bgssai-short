package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortCharacter implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String characterId;

    private String workId;

    private String userId;

    private String characterName;

    private String characterRole;

    private String cardStatus;

    private String appearancePrompt;

    private Long consistencySeed;

    private String refImageAssetId;

    private String defaultOutfitCode;

    private String outfitJson;

    private String voiceId;

    private String voiceSpeed;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId == null ? null : characterId.trim();
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

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName == null ? null : characterName.trim();
    }

    public String getCharacterRole() {
        return characterRole;
    }

    public void setCharacterRole(String characterRole) {
        this.characterRole = characterRole == null ? null : characterRole.trim();
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

    public Long getConsistencySeed() {
        return consistencySeed;
    }

    public void setConsistencySeed(Long consistencySeed) {
        this.consistencySeed = consistencySeed;
    }

    public String getRefImageAssetId() {
        return refImageAssetId;
    }

    public void setRefImageAssetId(String refImageAssetId) {
        this.refImageAssetId = refImageAssetId == null ? null : refImageAssetId.trim();
    }

    public String getDefaultOutfitCode() {
        return defaultOutfitCode;
    }

    public void setDefaultOutfitCode(String defaultOutfitCode) {
        this.defaultOutfitCode = defaultOutfitCode == null ? null : defaultOutfitCode.trim();
    }

    public String getOutfitJson() {
        return outfitJson;
    }

    public void setOutfitJson(String outfitJson) {
        this.outfitJson = outfitJson == null ? null : outfitJson.trim();
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId == null ? null : voiceId.trim();
    }

    public String getVoiceSpeed() {
        return voiceSpeed;
    }

    public void setVoiceSpeed(String voiceSpeed) {
        this.voiceSpeed = voiceSpeed == null ? null : voiceSpeed.trim();
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
