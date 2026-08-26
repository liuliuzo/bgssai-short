package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortStoryMap implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String mapId;

    private String workId;

    private String episodeId;

    private String userId;

    private String mapScope;

    private String mapJson;

    private Integer draftFlag;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId == null ? null : mapId.trim();
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

    public String getMapScope() {
        return mapScope;
    }

    public void setMapScope(String mapScope) {
        this.mapScope = mapScope == null ? null : mapScope.trim();
    }

    public String getMapJson() {
        return mapJson;
    }

    public void setMapJson(String mapJson) {
        this.mapJson = mapJson == null ? null : mapJson.trim();
    }

    public Integer getDraftFlag() {
        return draftFlag;
    }

    public void setDraftFlag(Integer draftFlag) {
        this.draftFlag = draftFlag;
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
