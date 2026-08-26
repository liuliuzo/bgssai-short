package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortShotPreviz implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String previzId;

    private String shotId;

    private String episodeId;

    private String workId;

    private String userId;

    private String previzLevel;

    private String previzText;

    private String draftAssetId;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrevizId() {
        return previzId;
    }

    public void setPrevizId(String previzId) {
        this.previzId = previzId == null ? null : previzId.trim();
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

    public String getPrevizLevel() {
        return previzLevel;
    }

    public void setPrevizLevel(String previzLevel) {
        this.previzLevel = previzLevel == null ? null : previzLevel.trim();
    }

    public String getPrevizText() {
        return previzText;
    }

    public void setPrevizText(String previzText) {
        this.previzText = previzText == null ? null : previzText.trim();
    }

    public String getDraftAssetId() {
        return draftAssetId;
    }

    public void setDraftAssetId(String draftAssetId) {
        this.draftAssetId = draftAssetId == null ? null : draftAssetId.trim();
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
