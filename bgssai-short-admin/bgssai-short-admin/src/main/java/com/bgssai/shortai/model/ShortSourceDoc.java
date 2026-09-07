package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortSourceDoc implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String sourceId;

    private String workId;

    private String userId;

    private String sourceType;

    private String sourceTitle;

    private Integer charCount;

    private Integer chapterCount;

    private Integer rightsConfirmFlag;

    private Date rightsConfirmTime;

    private String importStatus;

    private Integer adaptedFlag;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId == null ? null : sourceId.trim();
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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType == null ? null : sourceType.trim();
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle == null ? null : sourceTitle.trim();
    }

    public Integer getCharCount() {
        return charCount;
    }

    public void setCharCount(Integer charCount) {
        this.charCount = charCount;
    }

    public Integer getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(Integer chapterCount) {
        this.chapterCount = chapterCount;
    }

    public Integer getRightsConfirmFlag() {
        return rightsConfirmFlag;
    }

    public void setRightsConfirmFlag(Integer rightsConfirmFlag) {
        this.rightsConfirmFlag = rightsConfirmFlag;
    }

    public Date getRightsConfirmTime() {
        return rightsConfirmTime;
    }

    public void setRightsConfirmTime(Date rightsConfirmTime) {
        this.rightsConfirmTime = rightsConfirmTime;
    }

    public String getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(String importStatus) {
        this.importStatus = importStatus == null ? null : importStatus.trim();
    }

    public Integer getAdaptedFlag() {
        return adaptedFlag;
    }

    public void setAdaptedFlag(Integer adaptedFlag) {
        this.adaptedFlag = adaptedFlag;
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
