package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.production.dto.ProductionDtos;
import com.bgssai.shortai.production.service.OfficialShotVideoService;

@RestController
public class OfficialShotVideoController {

    @Autowired
    private OfficialShotVideoService service;

    @NeedAop
    @RateLimit(capacity = 20, refillPerMinute = 20, keyBy = RateLimit.Key.USER)
    @PostMapping("/bgssai/user/ai-tasks/official-video/preview")
    public ApiResponse preview(@RequestBody ProductionDtos.AiTaskReq req) {
        return service.preview(req);
    }

    @NeedAop
    @RateLimit(capacity = 20, refillPerMinute = 20, keyBy = RateLimit.Key.USER)
    @PostMapping("/bgssai/user/ai-tasks/official-video/submit")
    public ApiResponse submit(@RequestBody ProductionDtos.AiTaskReq req) {
        return service.submit(req);
    }
}
