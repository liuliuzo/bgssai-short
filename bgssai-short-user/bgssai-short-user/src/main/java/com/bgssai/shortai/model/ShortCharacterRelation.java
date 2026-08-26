package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortCharacterRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String relationId;

    private String workId;

    private String userId;

    private String fromCharacterId;

    private String toCharacterId;

    private String relationType;

    private Integer asOfEpisodeNo;

    private String relationDesc;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRelationId() {
        return relationId;
    }

    public void setRelationId(String relationId) {
        this.relationId = relationId == null ? null : relationId.trim();
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

    public String getFromCharacterId() {
        return fromCharacterId;
    }

    public void setFromCharacterId(String fromCharacterId) {
        this.fromCharacterId = fromCharacterId == null ? null : fromCharacterId.trim();
    }

    public String getToCharacterId() {
        return toCharacterId;
    }

    public void setToCharacterId(String toCharacterId) {
        this.toCharacterId = toCharacterId == null ? null : toCharacterId.trim();
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType == null ? null : relationType.trim();
    }

    public Integer getAsOfEpisodeNo() {
        return asOfEpisodeNo;
    }

    public void setAsOfEpisodeNo(Integer asOfEpisodeNo) {
        this.asOfEpisodeNo = asOfEpisodeNo;
    }

    public String getRelationDesc() {
        return relationDesc;
    }

    public void setRelationDesc(String relationDesc) {
        this.relationDesc = relationDesc == null ? null : relationDesc.trim();
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
