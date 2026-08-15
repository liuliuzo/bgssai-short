package com.bgssai.shortai.quota.dto;

public class ReqAdjustQuota {
    private String userId;
    private Integer balanceDeltaCent;
    private Integer extraDeltaCent;
    private Integer monthlyLimitCent;
    private String remark;
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getBalanceDeltaCent() { return balanceDeltaCent; }
    public void setBalanceDeltaCent(Integer balanceDeltaCent) { this.balanceDeltaCent = balanceDeltaCent; }
    public Integer getExtraDeltaCent() { return extraDeltaCent; }
    public void setExtraDeltaCent(Integer extraDeltaCent) { this.extraDeltaCent = extraDeltaCent; }
    public Integer getMonthlyLimitCent() { return monthlyLimitCent; }
    public void setMonthlyLimitCent(Integer monthlyLimitCent) { this.monthlyLimitCent = monthlyLimitCent; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
