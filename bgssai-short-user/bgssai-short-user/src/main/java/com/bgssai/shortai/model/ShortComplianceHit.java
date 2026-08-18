package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortComplianceHit implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String hitId;

    private String workId;

    private String episodeId;

    private String shotId;

    private String checkScope;

    private String riskLevel;

    private String riskCategory;

    private String hitText;

    private String suggestion;

    private Integer sceneNo;

    private Integer lineNo;

    private Integer shotNo;

    private Date createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHitId() {
        return hitId;
    }

    public void setHitId(String hitId) {
        this.hitId = hitId == null ? null : hitId.trim();
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

    public String getShotId() {
        return shotId;
    }

    public void setShotId(String shotId) {
        this.shotId = shotId == null ? null : shotId.trim();
    }

    public String getCheckScope() {
        return checkScope;
    }

    public void setCheckScope(String checkScope) {
        this.checkScope = checkScope == null ? null : checkScope.trim();
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel == null ? null : riskLevel.trim();
    }

    public String getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory == null ? null : riskCategory.trim();
    }

    public String getHitText() {
        return hitText;
    }

    public void setHitText(String hitText) {
        this.hitText = hitText == null ? null : hitText.trim();
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion == null ? null : suggestion.trim();
    }

    public Integer getSceneNo() {
        return sceneNo;
    }

    public void setSceneNo(Integer sceneNo) {
        this.sceneNo = sceneNo;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public Integer getShotNo() {
        return shotNo;
    }

    public void setShotNo(Integer shotNo) {
        this.shotNo = shotNo;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

}
