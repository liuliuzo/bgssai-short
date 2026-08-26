package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortForeshadow implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String foreshadowId;

    private String workId;

    private String userId;

    private Integer plantEpisodeNo;

    private Integer payoffEpisodeNo;

    private String foreshadowDesc;

    private String status;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getForeshadowId() {
        return foreshadowId;
    }

    public void setForeshadowId(String foreshadowId) {
        this.foreshadowId = foreshadowId == null ? null : foreshadowId.trim();
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

    public Integer getPlantEpisodeNo() {
        return plantEpisodeNo;
    }

    public void setPlantEpisodeNo(Integer plantEpisodeNo) {
        this.plantEpisodeNo = plantEpisodeNo;
    }

    public Integer getPayoffEpisodeNo() {
        return payoffEpisodeNo;
    }

    public void setPayoffEpisodeNo(Integer payoffEpisodeNo) {
        this.payoffEpisodeNo = payoffEpisodeNo;
    }

    public String getForeshadowDesc() {
        return foreshadowDesc;
    }

    public void setForeshadowDesc(String foreshadowDesc) {
        this.foreshadowDesc = foreshadowDesc == null ? null : foreshadowDesc.trim();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
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
