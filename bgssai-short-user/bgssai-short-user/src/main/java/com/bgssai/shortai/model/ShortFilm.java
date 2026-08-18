package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortFilm implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String filmId;

    private String episodeId;

    private String workId;

    private String userId;

    private String filmAssetId;

    private Integer filmDurationSec;

    private String composeStatus;

    private Integer expiredFlag;

    private Integer subtitleStaleFlag;

    private String composeTaskId;

    private Date composedTime;

    private String exportJson;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilmId() {
        return filmId;
    }

    public void setFilmId(String filmId) {
        this.filmId = filmId == null ? null : filmId.trim();
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

    public String getComposeStatus() {
        return composeStatus;
    }

    public void setComposeStatus(String composeStatus) {
        this.composeStatus = composeStatus == null ? null : composeStatus.trim();
    }

    public Integer getExpiredFlag() {
        return expiredFlag;
    }

    public void setExpiredFlag(Integer expiredFlag) {
        this.expiredFlag = expiredFlag;
    }

    public Integer getSubtitleStaleFlag() {
        return subtitleStaleFlag;
    }

    public void setSubtitleStaleFlag(Integer subtitleStaleFlag) {
        this.subtitleStaleFlag = subtitleStaleFlag;
    }

    public String getComposeTaskId() {
        return composeTaskId;
    }

    public void setComposeTaskId(String composeTaskId) {
        this.composeTaskId = composeTaskId == null ? null : composeTaskId.trim();
    }

    public Date getComposedTime() {
        return composedTime;
    }

    public void setComposedTime(Date composedTime) {
        this.composedTime = composedTime;
    }

    public String getExportJson() {
        return exportJson;
    }

    public void setExportJson(String exportJson) {
        this.exportJson = exportJson == null ? null : exportJson.trim();
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
