package com.bgssai.shortai.model;

import java.util.Date;

public class WebsiteSubscriptionPlan {
    private Long id;
    private String planCode;
    private String planName;
    private Integer monthlyPriceCent;
    private Integer yearlyPriceCent;
    private Integer fiveHourLimitTokens;
    private Integer weeklyLimitTokens;
    private Integer sortNo;
    private Boolean enabled;
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

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode == null ? null : planCode.trim();
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName == null ? null : planName.trim();
    }

    public Integer getMonthlyPriceCent() {
        return monthlyPriceCent;
    }

    public void setMonthlyPriceCent(Integer monthlyPriceCent) {
        this.monthlyPriceCent = monthlyPriceCent;
    }

    public Integer getYearlyPriceCent() {
        return yearlyPriceCent;
    }

    public void setYearlyPriceCent(Integer yearlyPriceCent) {
        this.yearlyPriceCent = yearlyPriceCent;
    }

    public Integer getFiveHourLimitTokens() {
        return fiveHourLimitTokens;
    }

    public void setFiveHourLimitTokens(Integer fiveHourLimitTokens) {
        this.fiveHourLimitTokens = fiveHourLimitTokens;
    }

    public Integer getWeeklyLimitTokens() {
        return weeklyLimitTokens;
    }

    public void setWeeklyLimitTokens(Integer weeklyLimitTokens) {
        this.weeklyLimitTokens = weeklyLimitTokens;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
