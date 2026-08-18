package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.AiTaskService;

@RestController
public class BatchProgressController {

    @Autowired
    private AiTaskService service;

    @NeedAop
    @GetMapping("/bgssai/user/ai-tasks/batch-progress")
    public ApiResponse handle(@RequestParam(value = "batch_id", required = false) String batch_id) {
        return service.batchProgress(batch_id);
    }
}
