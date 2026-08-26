package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortQualityThresholdLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String logId;

    private String workId;

    private String userId;

    private Integer oldThresholdScore;

    private Integer newThresholdScore;

    private String adjustReason;

    private Date createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId == null ? null : logId.trim();
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

    public Integer getOldThresholdScore() {
        return oldThresholdScore;
    }

    public void setOldThresholdScore(Integer oldThresholdScore) {
        this.oldThresholdScore = oldThresholdScore;
    }

    public Integer getNewThresholdScore() {
        return newThresholdScore;
    }

    public void setNewThresholdScore(Integer newThresholdScore) {
        this.newThresholdScore = newThresholdScore;
    }

    public String getAdjustReason() {
        return adjustReason;
    }

    public void setAdjustReason(String adjustReason) {
        this.adjustReason = adjustReason == null ? null : adjustReason.trim();
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

}
