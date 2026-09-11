package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortShot implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String shotId;

    private String episodeId;

    private String workId;

    private String userId;

    private Integer shotNo;

    private Integer sceneNo;

    private String shotSize;

    private String cameraMove;

    private Integer shotDurationSec;

    private String shotDesc;

    private String dialogueText;

    private String emotion;

    private String lighting;

    private String characterRefJson;

    private String sceneCardId;

    private String propRefJson;

    private String extraPrompt;

    private String imagePrompt;

    private String previzText;

    private String blockingJson;

    private Integer axisFlag;

    private String previzLevel;

    private Integer staleFlag;

    private Integer promptVersion;

    private String shotStatus;

    private Integer blockedFlag;

    private String riskLevel;

    private String firstFrameAssetId;

    private String lastFrameAssetId;

    private String frameLinkMode;

    private String lastFramePrompt;

    private String videoAssetId;

    private Integer retryCount;

    private Integer costSpentCent;

    private Integer shotVersion;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShotId() {
        return shotId;
    }

    public void setShotId(String shotId) {
        this.shotId = shotId == null ? null : shotId.trim();
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId == null ? null : episodeId.trim();
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

    public Integer getShotNo() {
        return shotNo;
    }

    public void setShotNo(Integer shotNo) {
        this.shotNo = shotNo;
    }

    public Integer getSceneNo() {
        return sceneNo;
    }

    public void setSceneNo(Integer sceneNo) {
        this.sceneNo = sceneNo;
    }

    public String getShotSize() {
        return shotSize;
    }

    public void setShotSize(String shotSize) {
        this.shotSize = shotSize == null ? null : shotSize.trim();
    }

    public String getCameraMove() {
        return cameraMove;
    }

    public void setCameraMove(String cameraMove) {
        this.cameraMove = cameraMove == null ? null : cameraMove.trim();
    }

    public Integer getShotDurationSec() {
        return shotDurationSec;
    }

    public void setShotDurationSec(Integer shotDurationSec) {
        this.shotDurationSec = shotDurationSec;
    }

    public String getShotDesc() {
        return shotDesc;
    }

    public void setShotDesc(String shotDesc) {
        this.shotDesc = shotDesc == null ? null : shotDesc.trim();
    }

    public String getDialogueText() {
        return dialogueText;
    }

    public void setDialogueText(String dialogueText) {
        this.dialogueText = dialogueText == null ? null : dialogueText.trim();
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion == null ? null : emotion.trim();
    }

    public String getLighting() {
        return lighting;
    }

    public void setLighting(String lighting) {
        this.lighting = lighting == null ? null : lighting.trim();
    }

    public String getCharacterRefJson() {
        return characterRefJson;
    }

    public void setCharacterRefJson(String characterRefJson) {
        this.characterRefJson = characterRefJson == null ? null : characterRefJson.trim();
    }

    public String getSceneCardId() {
        return sceneCardId;
    }

    public void setSceneCardId(String sceneCardId) {
        this.sceneCardId = sceneCardId == null ? null : sceneCardId.trim();
    }

    public String getPropRefJson() {
        return propRefJson;
    }

    public void setPropRefJson(String propRefJson) {
        this.propRefJson = propRefJson == null ? null : propRefJson.trim();
    }

    public String getExtraPrompt() {
        return extraPrompt;
    }

    public void setExtraPrompt(String extraPrompt) {
        this.extraPrompt = extraPrompt == null ? null : extraPrompt.trim();
    }

    public String getImagePrompt() {
        return imagePrompt;
    }

    public void setImagePrompt(String imagePrompt) {
        this.imagePrompt = imagePrompt == null ? null : imagePrompt.trim();
    }

    public String getPrevizText() {
        return previzText;
    }

    public void setPrevizText(String previzText) {
        this.previzText = previzText == null ? null : previzText.trim();
    }

    public String getBlockingJson() {
        return blockingJson;
    }

    public void setBlockingJson(String blockingJson) {
        this.blockingJson = blockingJson == null ? null : blockingJson.trim();
    }

    public Integer getAxisFlag() {
        return axisFlag;
    }

    public void setAxisFlag(Integer axisFlag) {
        this.axisFlag = axisFlag;
    }

    public String getPrevizLevel() {
        return previzLevel;
    }

    public void setPrevizLevel(String previzLevel) {
        this.previzLevel = previzLevel == null ? null : previzLevel.trim();
    }

    public Integer getStaleFlag() {
        return staleFlag;
    }

    public void setStaleFlag(Integer staleFlag) {
        this.staleFlag = staleFlag;
    }

    public Integer getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(Integer promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getShotStatus() {
        return shotStatus;
    }

    public void setShotStatus(String shotStatus) {
        this.shotStatus = shotStatus == null ? null : shotStatus.trim();
    }

    public Integer getBlockedFlag() {
        return blockedFlag;
    }

    public void setBlockedFlag(Integer blockedFlag) {
        this.blockedFlag = blockedFlag;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel == null ? null : riskLevel.trim();
    }

    public String getFirstFrameAssetId() {
        return firstFrameAssetId;
    }

    public void setFirstFrameAssetId(String firstFrameAssetId) {
        this.firstFrameAssetId = firstFrameAssetId == null ? null : firstFrameAssetId.trim();
    }

    public String getLastFrameAssetId() {
        return lastFrameAssetId;
    }

    public void setLastFrameAssetId(String lastFrameAssetId) {
        this.lastFrameAssetId = lastFrameAssetId == null ? null : lastFrameAssetId.trim();
    }

    public String getFrameLinkMode() {
        return frameLinkMode;
    }

    public void setFrameLinkMode(String frameLinkMode) {
        this.frameLinkMode = frameLinkMode == null ? null : frameLinkMode.trim();
    }

    public String getLastFramePrompt() {
        return lastFramePrompt;
    }

    public void setLastFramePrompt(String lastFramePrompt) {
        this.lastFramePrompt = lastFramePrompt == null ? null : lastFramePrompt.trim();
    }

    public String getVideoAssetId() {
        return videoAssetId;
    }

    public void setVideoAssetId(String videoAssetId) {
        this.videoAssetId = videoAssetId == null ? null : videoAssetId.trim();
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getCostSpentCent() {
        return costSpentCent;
    }

    public void setCostSpentCent(Integer costSpentCent) {
        this.costSpentCent = costSpentCent;
    }

    public Integer getShotVersion() {
        return shotVersion;
    }

    public void setShotVersion(Integer shotVersion) {
        this.shotVersion = shotVersion;
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
