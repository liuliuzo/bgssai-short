package com.bgssai.shortai.billing;
public record UsageScope(String userId, String workRef, String taskRef) {
 private static final ThreadLocal<UsageScope> CURRENT=new ThreadLocal<>();
 public static UsageScope current(){return CURRENT.get();}
 public static void set(String userId,String workRef,String taskRef){CURRENT.set(new UsageScope(userId,workRef,taskRef));}
 public static void clear(){CURRENT.remove();}
}
