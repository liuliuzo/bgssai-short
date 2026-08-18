package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortAsset implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String assetId;

    private String userId;

    private String workId;

    private String episodeId;

    private String shotId;

    private String assetType;

    private String storageKey;

    private String fileUrl;

    private Integer fileSizeByte;

    private Integer durationSec;

    private Integer width;

    private Integer height;

    private String sourceTaskId;

    private Integer selectedFlag;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId == null ? null : assetId.trim();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public String getWorkId() {
        return workId;
    }

    public void setWorkId(String workId) {
        this.workId = workId == null ? null : workId.trim();
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId == null ? null : episodeId.trim();
    }

    public String getShotId() {
        return shotId;
    }

    public void setShotId(String shotId) {
        this.shotId = shotId == null ? null : shotId.trim();
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType == null ? null : assetType.trim();
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey == null ? null : storageKey.trim();
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl == null ? null : fileUrl.trim();
    }

    public Integer getFileSizeByte() {
        return fileSizeByte;
    }

    public void setFileSizeByte(Integer fileSizeByte) {
        this.fileSizeByte = fileSizeByte;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(String sourceTaskId) {
        this.sourceTaskId = sourceTaskId == null ? null : sourceTaskId.trim();
    }

    public Integer getSelectedFlag() {
        return selectedFlag;
    }

    public void setSelectedFlag(Integer selectedFlag) {
        this.selectedFlag = selectedFlag;
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
