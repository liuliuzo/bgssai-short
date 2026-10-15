package com.bgssai.shortai.billing;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.WorkAccessService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import org.springframework.web.bind.annotation.*;
@RestController
public class UsageReceiptController {
 private final UsageReceiptService service;private final WorkAccessService access;
 public UsageReceiptController(UsageReceiptService service,WorkAccessService access){this.service=service;this.access=access;}
 @NeedAop
 @GetMapping("/bgssai/user/ai-usage/list")
 public ApiResponse list(@RequestParam(name="work_ref",required=false)String workRef,@RequestParam(name="task_ref",required=false)String taskRef){
  return Responses.ok(java.util.Map.of("items",service.list(access.requireUser(),workRef,taskRef)));
 }
}
