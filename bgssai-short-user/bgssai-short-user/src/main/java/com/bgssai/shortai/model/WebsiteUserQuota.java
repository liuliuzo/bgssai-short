package com.bgssai.shortai.model;

import java.util.Date;

public class WebsiteUserQuota {
    private Long id;
    private String userId;
    private String planCode;
    private Integer fiveHourLimitPoints;
    private Integer fiveHourUsedPoints;
    private Date fiveHourWindowStart;
    private Integer weeklyLimitPoints;
    private Integer weeklyUsedPoints;
    private Date weeklyAnchorAt;
    private Integer extraPoints;
    private Integer quotaVersion;
    private Boolean delFlag;
    private String creator;
    private String modifier;
    private Date gmtCreate;
    private Date gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode == null ? null : planCode.trim();
    }

    public Integer getFiveHourLimitPoints() {
        return fiveHourLimitPoints;
    }

    public void setFiveHourLimitPoints(Integer fiveHourLimitPoints) {
        this.fiveHourLimitPoints = fiveHourLimitPoints;
    }

    public Integer getFiveHourUsedPoints() {
        return fiveHourUsedPoints;
    }

    public void setFiveHourUsedPoints(Integer fiveHourUsedPoints) {
        this.fiveHourUsedPoints = fiveHourUsedPoints;
    }

    public Date getFiveHourWindowStart() {
        return fiveHourWindowStart;
    }

    public void setFiveHourWindowStart(Date fiveHourWindowStart) {
        this.fiveHourWindowStart = fiveHourWindowStart;
    }

    public Integer getWeeklyLimitPoints() {
        return weeklyLimitPoints;
    }

    public void setWeeklyLimitPoints(Integer weeklyLimitPoints) {
        this.weeklyLimitPoints = weeklyLimitPoints;
    }

    public Integer getWeeklyUsedPoints() {
        return weeklyUsedPoints;
    }

    public void setWeeklyUsedPoints(Integer weeklyUsedPoints) {
        this.weeklyUsedPoints = weeklyUsedPoints;
    }

    public Date getWeeklyAnchorAt() {
        return weeklyAnchorAt;
    }

    public void setWeeklyAnchorAt(Date weeklyAnchorAt) {
        this.weeklyAnchorAt = weeklyAnchorAt;
    }

    public Integer getExtraPoints() {
        return extraPoints;
    }

    public void setExtraPoints(Integer extraPoints) {
        this.extraPoints = extraPoints;
    }

    public Integer getQuotaVersion() {
        return quotaVersion;
    }

    public void setQuotaVersion(Integer quotaVersion) {
        this.quotaVersion = quotaVersion;
    }

    public Boolean getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Boolean delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator == null ? null : creator.trim();
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier == null ? null : modifier.trim();
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

}
