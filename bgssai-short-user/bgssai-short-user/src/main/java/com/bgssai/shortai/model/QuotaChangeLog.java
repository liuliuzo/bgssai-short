package com.bgssai.shortai.model;

import java.util.Date;

public class QuotaChangeLog {
    private Long id;
    private String userId;
    private String operatorUserId;
    private String changeType;
    private Integer balanceDeltaCent;
    private Integer extraDeltaCent;
    private Integer balanceAfterCent;
    private Integer extraAfterCent;
    private Integer monthlyLimitCent;
    private String remark;
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

    public String getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(String operatorUserId) {
        this.operatorUserId = operatorUserId == null ? null : operatorUserId.trim();
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType == null ? null : changeType.trim();
    }

    public Integer getBalanceDeltaCent() {
        return balanceDeltaCent;
    }

    public void setBalanceDeltaCent(Integer balanceDeltaCent) {
        this.balanceDeltaCent = balanceDeltaCent;
    }

    public Integer getExtraDeltaCent() {
        return extraDeltaCent;
    }

    public void setExtraDeltaCent(Integer extraDeltaCent) {
        this.extraDeltaCent = extraDeltaCent;
    }

    public Integer getBalanceAfterCent() {
        return balanceAfterCent;
    }

    public void setBalanceAfterCent(Integer balanceAfterCent) {
        this.balanceAfterCent = balanceAfterCent;
    }

    public Integer getExtraAfterCent() {
        return extraAfterCent;
    }

    public void setExtraAfterCent(Integer extraAfterCent) {
        this.extraAfterCent = extraAfterCent;
    }

    public Integer getMonthlyLimitCent() {
        return monthlyLimitCent;
    }

    public void setMonthlyLimitCent(Integer monthlyLimitCent) {
        this.monthlyLimitCent = monthlyLimitCent;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
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
