package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortQualityGate implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String gateId;

    private String workId;

    private String episodeId;

    private String userId;

    private String gateCode;

    private String gateStatus;

    private Integer score;

    private Integer thresholdScore;

    private Integer forcePassFlag;

    private String forcePassReason;

    private String resultJson;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGateId() {
        return gateId;
    }

    public void setGateId(String gateId) {
        this.gateId = gateId == null ? null : gateId.trim();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public String getGateCode() {
        return gateCode;
    }

    public void setGateCode(String gateCode) {
        this.gateCode = gateCode == null ? null : gateCode.trim();
    }

    public String getGateStatus() {
        return gateStatus;
    }

    public void setGateStatus(String gateStatus) {
        this.gateStatus = gateStatus == null ? null : gateStatus.trim();
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getThresholdScore() {
        return thresholdScore;
    }

    public void setThresholdScore(Integer thresholdScore) {
        this.thresholdScore = thresholdScore;
    }

    public Integer getForcePassFlag() {
        return forcePassFlag;
    }

    public void setForcePassFlag(Integer forcePassFlag) {
        this.forcePassFlag = forcePassFlag;
    }

    public String getForcePassReason() {
        return forcePassReason;
    }

    public void setForcePassReason(String forcePassReason) {
        this.forcePassReason = forcePassReason == null ? null : forcePassReason.trim();
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson == null ? null : resultJson.trim();
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
