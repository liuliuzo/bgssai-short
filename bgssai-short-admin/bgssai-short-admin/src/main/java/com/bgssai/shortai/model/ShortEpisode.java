package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortEpisode implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String episodeId;

    private String workId;

    private String userId;

    private Integer episodeNo;

    private String episodeTitle;

    private String episodeSynopsis;

    private String endingHook;

    private String scriptStatus;

    private String storyboardStatus;

    private String dubbingStatus;

    private String composeStatus;

    private String reviewStatus;

    private String filmAssetId;

    private Integer filmDurationSec;

    private String sourceRefJson;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getEpisodeNo() {
        return episodeNo;
    }

    public void setEpisodeNo(Integer episodeNo) {
        this.episodeNo = episodeNo;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public void setEpisodeTitle(String episodeTitle) {
        this.episodeTitle = episodeTitle == null ? null : episodeTitle.trim();
    }

    public String getEpisodeSynopsis() {
        return episodeSynopsis;
    }

    public void setEpisodeSynopsis(String episodeSynopsis) {
        this.episodeSynopsis = episodeSynopsis == null ? null : episodeSynopsis.trim();
    }

    public String getEndingHook() {
        return endingHook;
    }

    public void setEndingHook(String endingHook) {
        this.endingHook = endingHook == null ? null : endingHook.trim();
    }

    public String getScriptStatus() {
        return scriptStatus;
    }

    public void setScriptStatus(String scriptStatus) {
        this.scriptStatus = scriptStatus == null ? null : scriptStatus.trim();
    }

    public String getStoryboardStatus() {
        return storyboardStatus;
    }

    public void setStoryboardStatus(String storyboardStatus) {
        this.storyboardStatus = storyboardStatus == null ? null : storyboardStatus.trim();
    }

    public String getDubbingStatus() {
        return dubbingStatus;
    }

    public void setDubbingStatus(String dubbingStatus) {
        this.dubbingStatus = dubbingStatus == null ? null : dubbingStatus.trim();
    }

    public String getComposeStatus() {
        return composeStatus;
    }

    public void setComposeStatus(String composeStatus) {
        this.composeStatus = composeStatus == null ? null : composeStatus.trim();
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus == null ? null : reviewStatus.trim();
    }

    public String getFilmAssetId() {
        return filmAssetId;
    }

    public void setFilmAssetId(String filmAssetId) {
        this.filmAssetId = filmAssetId == null ? null : filmAssetId.trim();
    }

    public Integer getFilmDurationSec() {
        return filmDurationSec;
    }

    public void setFilmDurationSec(Integer filmDurationSec) {
        this.filmDurationSec = filmDurationSec;
    }

    public String getSourceRefJson() {
        return sourceRefJson;
    }

    public void setSourceRefJson(String sourceRefJson) {
        this.sourceRefJson = sourceRefJson == null ? null : sourceRefJson.trim();
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
