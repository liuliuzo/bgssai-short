package com.bgssai.shortai.subscriptionplan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReqSaveSubscriptionPlan {
    private Long id;
    private String planCode;
    private String planName;
    private Integer priceCent;
    private Integer monthlyBalanceCent;
    private Integer monthlyLimitCent;
    @JsonProperty("is_active")
    private Integer active;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Integer getPriceCent() { return priceCent; }
    public void setPriceCent(Integer priceCent) { this.priceCent = priceCent; }
    public Integer getMonthlyBalanceCent() { return monthlyBalanceCent; }
    public void setMonthlyBalanceCent(Integer monthlyBalanceCent) { this.monthlyBalanceCent = monthlyBalanceCent; }
    public Integer getMonthlyLimitCent() { return monthlyLimitCent; }
    public void setMonthlyLimitCent(Integer monthlyLimitCent) { this.monthlyLimitCent = monthlyLimitCent; }
    public Integer getActive() { return active; }
    public void setActive(Integer active) { this.active = active; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
