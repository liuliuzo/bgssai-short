package com.bgssai.shortai.billing;
public class UsageReceiptRow {
 public Long id;
 public String userId, workRef, taskRef, hub, endpoint, keyFingerprint, model, requestId, receiptJson;
 public boolean reconciled;
 public long nextPollAt;
 public int delFlag;
 public String creator, modifier;
 public java.time.LocalDateTime gmtCreate, gmtModified;
}
