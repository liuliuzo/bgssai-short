package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortScriptVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String versionId;

    private String scriptId;

    private Integer scriptVersion;

    private String sceneJson;

    private Integer totalScore;

    private String changeSummary;

    private Date createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId == null ? null : versionId.trim();
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId == null ? null : scriptId.trim();
    }

    public Integer getScriptVersion() {
        return scriptVersion;
    }

    public void setScriptVersion(Integer scriptVersion) {
        this.scriptVersion = scriptVersion;
    }

    public String getSceneJson() {
        return sceneJson;
    }

    public void setSceneJson(String sceneJson) {
        this.sceneJson = sceneJson == null ? null : sceneJson.trim();
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary == null ? null : changeSummary.trim();
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

}
