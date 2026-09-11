package com.bgssai.shortai.model;

public class ShortDebitOutboxEntry {
    private String outboxId;
    private String userId;
    private String websiteUserId;
    private Integer amount;
    private String usageKind;
    private Integer delivered;
    private Integer attempts;
    private java.util.Date nextAttemptAt;
    private java.util.Date leaseUntil;
    public String getOutboxId() { return outboxId; }
    public void setOutboxId(String value) { outboxId = value; }
    public String getUserId() { return userId; }
    public void setUserId(String value) { userId = value; }
    public String getWebsiteUserId() { return websiteUserId; }
    public void setWebsiteUserId(String value) { websiteUserId = value; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer value) { amount = value; }
    public String getUsageKind() { return usageKind; }
    public void setUsageKind(String value) { usageKind = value; }
    public Integer getDelivered() { return delivered; }
    public void setDelivered(Integer value) { delivered = value; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer value) { attempts = value; }
    public java.util.Date getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(java.util.Date value) { nextAttemptAt = value; }
    public java.util.Date getLeaseUntil() { return leaseUntil; }
    public void setLeaseUntil(java.util.Date value) { leaseUntil = value; }
}
