package com.bgssai.shortai.centralledger.dto;

import java.util.Date;

/**
 * 本产品自己卖出的一笔订阅，上报到中央账本 bgssai_website.external_subscription 的载体。
 * 同一 externalOrderNo 反复上报只落一行（创建、续费、到期、退款都走它）。
 */
public class OwnSubscriptionReport {

    /** 产品侧用户标识（本产品自己的账号 ID 字符串）。 */
    private String externalUserId;

    /** 产品侧订单号，与产品编码一起构成中央账本的幂等键。 */
    private String externalOrderNo;

    /** 已绑定 BGSSAI 账号时的官网 user_id；未绑定留空，日后回填。 */
    private String websiteUserId;

    private String planCode;

    private String planName;

    /** 实收金额（分）。赠送 / 试用填 0。 */
    private Integer amountCent;

    /** ACTIVE / EXPIRED / CANCELED / REFUNDED，空视为 ACTIVE。 */
    private String status;

    private Date periodStart;

    private Date periodEnd;

    private String remark;

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public String getExternalOrderNo() {
        return externalOrderNo;
    }

    public void setExternalOrderNo(String externalOrderNo) {
        this.externalOrderNo = externalOrderNo;
    }

    public String getWebsiteUserId() {
        return websiteUserId;
    }

    public void setWebsiteUserId(String websiteUserId) {
        this.websiteUserId = websiteUserId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Integer getAmountCent() {
        return amountCent;
    }

    public void setAmountCent(Integer amountCent) {
        this.amountCent = amountCent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Date periodStart) {
        this.periodStart = periodStart;
    }

    public Date getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Date periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
