package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortWork implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String workId;

    private String userId;

    private String workTitle;

    private String oneLinePitch;

    private String genre;

    private String workStatus;

    private Integer episodeCount;

    private Integer episodeDurationSec;

    private String visualStyle;

    private String aspectRatio;

    private String coverAssetId;

    private Integer costBudgetCent;

    private Integer costSpentCent;

    private Integer skipLevelCheckFlag;

    private Integer qualityThresholdScore;

    private String outlineJson;

    private String composeSettingJson;

    private Integer archivedFlag;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getWorkTitle() {
        return workTitle;
    }

    public void setWorkTitle(String workTitle) {
        this.workTitle = workTitle == null ? null : workTitle.trim();
    }

    public String getOneLinePitch() {
        return oneLinePitch;
    }

    public void setOneLinePitch(String oneLinePitch) {
        this.oneLinePitch = oneLinePitch == null ? null : oneLinePitch.trim();
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre == null ? null : genre.trim();
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(String workStatus) {
        this.workStatus = workStatus == null ? null : workStatus.trim();
    }

    public Integer getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(Integer episodeCount) {
        this.episodeCount = episodeCount;
    }

    public Integer getEpisodeDurationSec() {
        return episodeDurationSec;
    }

    public void setEpisodeDurationSec(Integer episodeDurationSec) {
        this.episodeDurationSec = episodeDurationSec;
    }

    public String getVisualStyle() {
        return visualStyle;
    }

    public void setVisualStyle(String visualStyle) {
        this.visualStyle = visualStyle == null ? null : visualStyle.trim();
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio == null ? null : aspectRatio.trim();
    }

    public String getCoverAssetId() {
        return coverAssetId;
    }

    public void setCoverAssetId(String coverAssetId) {
        this.coverAssetId = coverAssetId == null ? null : coverAssetId.trim();
    }

    public Integer getCostBudgetCent() {
        return costBudgetCent;
    }

    public void setCostBudgetCent(Integer costBudgetCent) {
        this.costBudgetCent = costBudgetCent;
    }

    public Integer getCostSpentCent() {
        return costSpentCent;
    }

    public void setCostSpentCent(Integer costSpentCent) {
        this.costSpentCent = costSpentCent;
    }

    public Integer getSkipLevelCheckFlag() {
        return skipLevelCheckFlag;
    }

    public void setSkipLevelCheckFlag(Integer skipLevelCheckFlag) {
        this.skipLevelCheckFlag = skipLevelCheckFlag;
    }

    public Integer getQualityThresholdScore() {
        return qualityThresholdScore;
    }

    public void setQualityThresholdScore(Integer qualityThresholdScore) {
        this.qualityThresholdScore = qualityThresholdScore;
    }

    public String getOutlineJson() {
        return outlineJson;
    }

    public void setOutlineJson(String outlineJson) {
        this.outlineJson = outlineJson == null ? null : outlineJson.trim();
    }

    public String getComposeSettingJson() {
        return composeSettingJson;
    }

    public void setComposeSettingJson(String composeSettingJson) {
        this.composeSettingJson = composeSettingJson == null ? null : composeSettingJson.trim();
    }

    public Integer getArchivedFlag() {
        return archivedFlag;
    }

    public void setArchivedFlag(Integer archivedFlag) {
        this.archivedFlag = archivedFlag;
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
