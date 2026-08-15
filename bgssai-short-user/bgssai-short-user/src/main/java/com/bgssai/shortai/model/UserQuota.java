package com.bgssai.shortai.model;

import java.util.Date;

public class UserQuota {
    private Long id;
    private String userId;
    private Integer balanceCent;
    private Integer monthlyLimitCent;
    private Integer extraBalanceCent;
    private Integer monthSpentCent;
    private Date monthResetDate;
    private Integer delete;
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

    public Integer getBalanceCent() {
        return balanceCent;
    }

    public void setBalanceCent(Integer balanceCent) {
        this.balanceCent = balanceCent;
    }

    public Integer getMonthlyLimitCent() {
        return monthlyLimitCent;
    }

    public void setMonthlyLimitCent(Integer monthlyLimitCent) {
        this.monthlyLimitCent = monthlyLimitCent;
    }

    public Integer getExtraBalanceCent() {
        return extraBalanceCent;
    }

    public void setExtraBalanceCent(Integer extraBalanceCent) {
        this.extraBalanceCent = extraBalanceCent;
    }

    public Integer getMonthSpentCent() {
        return monthSpentCent;
    }

    public void setMonthSpentCent(Integer monthSpentCent) {
        this.monthSpentCent = monthSpentCent;
    }

    public Date getMonthResetDate() {
        return monthResetDate;
    }

    public void setMonthResetDate(Date monthResetDate) {
        this.monthResetDate = monthResetDate;
    }

    public Integer getDelete() {
        return delete;
    }

    public void setDelete(Integer delete) {
        this.delete = delete;
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
