package com.bgssai.shortai.model;

import java.util.Date;

public class WebsiteUserQuota {
    private Long id;
    private String userId;
    private String planCode;
    private Integer fiveHourLimitTokens;
    private Integer fiveHourUsedTokens;
    private Date fiveHourWindowStart;
    private Integer weeklyLimitTokens;
    private Integer weeklyUsedTokens;
    private Date weeklyAnchorAt;
    private Integer extraTokens;
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

    public Integer getFiveHourLimitTokens() {
        return fiveHourLimitTokens;
    }

    public void setFiveHourLimitTokens(Integer fiveHourLimitTokens) {
        this.fiveHourLimitTokens = fiveHourLimitTokens;
    }

    public Integer getFiveHourUsedTokens() {
        return fiveHourUsedTokens;
    }

    public void setFiveHourUsedTokens(Integer fiveHourUsedTokens) {
        this.fiveHourUsedTokens = fiveHourUsedTokens;
    }

    public Date getFiveHourWindowStart() {
        return fiveHourWindowStart;
    }

    public void setFiveHourWindowStart(Date fiveHourWindowStart) {
        this.fiveHourWindowStart = fiveHourWindowStart;
    }

    public Integer getWeeklyLimitTokens() {
        return weeklyLimitTokens;
    }

    public void setWeeklyLimitTokens(Integer weeklyLimitTokens) {
        this.weeklyLimitTokens = weeklyLimitTokens;
    }

    public Integer getWeeklyUsedTokens() {
        return weeklyUsedTokens;
    }

    public void setWeeklyUsedTokens(Integer weeklyUsedTokens) {
        this.weeklyUsedTokens = weeklyUsedTokens;
    }

    public Date getWeeklyAnchorAt() {
        return weeklyAnchorAt;
    }

    public void setWeeklyAnchorAt(Date weeklyAnchorAt) {
        this.weeklyAnchorAt = weeklyAnchorAt;
    }

    public Integer getExtraTokens() {
        return extraTokens;
    }

    public void setExtraTokens(Integer extraTokens) {
        this.extraTokens = extraTokens;
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
