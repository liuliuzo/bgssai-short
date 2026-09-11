package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.production.dto.ProductionDtos;
import com.bgssai.shortai.production.service.AiTaskService;

@RestController
public class RecoverAiTaskController {

    @Autowired
    private AiTaskService service;

    @NeedAop
    @RateLimit(capacity = 20, refillPerMinute = 20, keyBy = RateLimit.Key.USER)
    @PostMapping("/bgssai/user/ai-tasks/recover")
    public ApiResponse handle(@RequestBody ProductionDtos.AiTaskReq req) {
        return service.recover(req);
    }
}
