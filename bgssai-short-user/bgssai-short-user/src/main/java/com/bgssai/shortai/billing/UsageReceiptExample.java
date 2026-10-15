package com.bgssai.shortai.billing;
import java.util.Set;
public class UsageReceiptExample {
 public String userId, workRef, taskRef;
 public Long dueBefore;
 public boolean pending;
 public java.util.List<String> selectedColumns;
 public void selectColumns(String... columns) {
  Set<String> allowed=Set.of("id","user_id","work_ref","task_ref","hub","endpoint","key_fingerprint","model","request_id","receipt_json","reconciled","next_poll_at","del_flag","creator","modifier","gmt_create","gmt_modified");
  for(String col:columns) if(!allowed.contains(col)) throw new IllegalArgumentException("Invalid column");
  selectedColumns=java.util.List.of(columns);
 }
}
