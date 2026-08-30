package com.bgssai.shortai.model;

import java.util.Date;

public class SubscriptionPlan {
    private Long id;
    private String planCode;
    private String planName;
    private Integer priceCent;
    private Integer monthlyBalanceCent;
    private Integer monthlyLimitCent;
    private Integer active;
    private Integer sortOrder;
    private Integer delFlag;
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

    public Integer getPriceCent() {
        return priceCent;
    }

    public void setPriceCent(Integer priceCent) {
        this.priceCent = priceCent;
    }

    public Integer getMonthlyBalanceCent() {
        return monthlyBalanceCent;
    }

    public void setMonthlyBalanceCent(Integer monthlyBalanceCent) {
        this.monthlyBalanceCent = monthlyBalanceCent;
    }

    public Integer getMonthlyLimitCent() {
        return monthlyLimitCent;
    }

    public void setMonthlyLimitCent(Integer monthlyLimitCent) {
        this.monthlyLimitCent = monthlyLimitCent;
    }

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
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
