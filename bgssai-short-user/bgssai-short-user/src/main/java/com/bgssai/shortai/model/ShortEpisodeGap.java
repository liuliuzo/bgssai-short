package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortEpisodeGap implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String gapId;

    private String workId;

    private String fromEpisodeId;

    private String toEpisodeId;

    private String gapDesc;

    private String severity;

    private String handleStatus;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGapId() {
        return gapId;
    }

    public void setGapId(String gapId) {
        this.gapId = gapId == null ? null : gapId.trim();
    }

    public String getWorkId() {
        return workId;
    }

    public void setWorkId(String workId) {
        this.workId = workId == null ? null : workId.trim();
    }

    public String getFromEpisodeId() {
        return fromEpisodeId;
    }

    public void setFromEpisodeId(String fromEpisodeId) {
        this.fromEpisodeId = fromEpisodeId == null ? null : fromEpisodeId.trim();
    }

    public String getToEpisodeId() {
        return toEpisodeId;
    }

    public void setToEpisodeId(String toEpisodeId) {
        this.toEpisodeId = toEpisodeId == null ? null : toEpisodeId.trim();
    }

    public String getGapDesc() {
        return gapDesc;
    }

    public void setGapDesc(String gapDesc) {
        this.gapDesc = gapDesc == null ? null : gapDesc.trim();
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity == null ? null : severity.trim();
    }

    public String getHandleStatus() {
        return handleStatus;
    }

    public void setHandleStatus(String handleStatus) {
        this.handleStatus = handleStatus == null ? null : handleStatus.trim();
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
