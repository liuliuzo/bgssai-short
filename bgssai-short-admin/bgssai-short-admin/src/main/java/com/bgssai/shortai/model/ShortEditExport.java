package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortEditExport implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String exportId;

    private String episodeId;

    private String workId;

    private String userId;

    private String projectFormat;

    private String jianyingVersion;

    private String mediaMode;

    private String draftDir;

    private String exportStatus;

    private Integer clipCount;

    private Long packageByte;

    private String assetId;

    private String packageName;

    private String missingJson;

    private String failReason;

    private Date finishedTime;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExportId() {
        return exportId;
    }

    public void setExportId(String exportId) {
        this.exportId = exportId == null ? null : exportId.trim();
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

    public String getProjectFormat() {
        return projectFormat;
    }

    public void setProjectFormat(String projectFormat) {
        this.projectFormat = projectFormat == null ? null : projectFormat.trim();
    }

    public String getJianyingVersion() {
        return jianyingVersion;
    }

    public void setJianyingVersion(String jianyingVersion) {
        this.jianyingVersion = jianyingVersion == null ? null : jianyingVersion.trim();
    }

    public String getMediaMode() {
        return mediaMode;
    }

    public void setMediaMode(String mediaMode) {
        this.mediaMode = mediaMode == null ? null : mediaMode.trim();
    }

    public String getDraftDir() {
        return draftDir;
    }

    public void setDraftDir(String draftDir) {
        this.draftDir = draftDir == null ? null : draftDir.trim();
    }

    public String getExportStatus() {
        return exportStatus;
    }

    public void setExportStatus(String exportStatus) {
        this.exportStatus = exportStatus == null ? null : exportStatus.trim();
    }

    public Integer getClipCount() {
        return clipCount;
    }

    public void setClipCount(Integer clipCount) {
        this.clipCount = clipCount;
    }

    public Long getPackageByte() {
        return packageByte;
    }

    public void setPackageByte(Long packageByte) {
        this.packageByte = packageByte;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId == null ? null : assetId.trim();
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName == null ? null : packageName.trim();
    }

    public String getMissingJson() {
        return missingJson;
    }

    public void setMissingJson(String missingJson) {
        this.missingJson = missingJson == null ? null : missingJson.trim();
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason == null ? null : failReason.trim();
    }

    public Date getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(Date finishedTime) {
        this.finishedTime = finishedTime;
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
