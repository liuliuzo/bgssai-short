package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortScript implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String scriptId;

    private String episodeId;

    private String workId;

    private String userId;

    private Integer scriptVersion;

    private String scriptStatus;

    private String sceneJson;

    private Integer estimatedTotalSec;

    private String rhythmJson;

    private Integer totalScore;

    private Integer forceReadyFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId == null ? null : scriptId.trim();
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

    public Integer getScriptVersion() {
        return scriptVersion;
    }

    public void setScriptVersion(Integer scriptVersion) {
        this.scriptVersion = scriptVersion;
    }

    public String getScriptStatus() {
        return scriptStatus;
    }

    public void setScriptStatus(String scriptStatus) {
        this.scriptStatus = scriptStatus == null ? null : scriptStatus.trim();
    }

    public String getSceneJson() {
        return sceneJson;
    }

    public void setSceneJson(String sceneJson) {
        this.sceneJson = sceneJson == null ? null : sceneJson.trim();
    }

    public Integer getEstimatedTotalSec() {
        return estimatedTotalSec;
    }

    public void setEstimatedTotalSec(Integer estimatedTotalSec) {
        this.estimatedTotalSec = estimatedTotalSec;
    }

    public String getRhythmJson() {
        return rhythmJson;
    }

    public void setRhythmJson(String rhythmJson) {
        this.rhythmJson = rhythmJson == null ? null : rhythmJson.trim();
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getForceReadyFlag() {
        return forceReadyFlag;
    }

    public void setForceReadyFlag(Integer forceReadyFlag) {
        this.forceReadyFlag = forceReadyFlag;
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
