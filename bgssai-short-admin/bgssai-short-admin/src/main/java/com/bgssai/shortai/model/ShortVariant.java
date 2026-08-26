package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortVariant implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String variantId;

    private String workId;

    private String episodeId;

    private String variantNode;

    private String targetId;

    private Integer variantNo;

    private String contentJson;

    private Integer score;

    private Integer selectedFlag;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId == null ? null : variantId.trim();
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

    public String getVariantNode() {
        return variantNode;
    }

    public void setVariantNode(String variantNode) {
        this.variantNode = variantNode == null ? null : variantNode.trim();
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId == null ? null : targetId.trim();
    }

    public Integer getVariantNo() {
        return variantNo;
    }

    public void setVariantNo(Integer variantNo) {
        this.variantNo = variantNo;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson == null ? null : contentJson.trim();
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getSelectedFlag() {
        return selectedFlag;
    }

    public void setSelectedFlag(Integer selectedFlag) {
        this.selectedFlag = selectedFlag;
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
