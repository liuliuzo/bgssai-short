package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortSubtitle implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String subtitleId;

    private String episodeId;

    private String workId;

    private String userId;

    private Integer subtitleNo;

    private Integer startMs;

    private Integer endMs;

    private String subtitleText;

    private String shotId;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubtitleId() {
        return subtitleId;
    }

    public void setSubtitleId(String subtitleId) {
        this.subtitleId = subtitleId == null ? null : subtitleId.trim();
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

    public Integer getSubtitleNo() {
        return subtitleNo;
    }

    public void setSubtitleNo(Integer subtitleNo) {
        this.subtitleNo = subtitleNo;
    }

    public Integer getStartMs() {
        return startMs;
    }

    public void setStartMs(Integer startMs) {
        this.startMs = startMs;
    }

    public Integer getEndMs() {
        return endMs;
    }

    public void setEndMs(Integer endMs) {
        this.endMs = endMs;
    }

    public String getSubtitleText() {
        return subtitleText;
    }

    public void setSubtitleText(String subtitleText) {
        this.subtitleText = subtitleText == null ? null : subtitleText.trim();
    }

    public String getShotId() {
        return shotId;
    }

    public void setShotId(String shotId) {
        this.shotId = shotId == null ? null : shotId.trim();
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
