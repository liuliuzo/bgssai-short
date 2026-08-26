package com.bgssai.shortai.usage.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.usage.service.UsageUserService;

@RestController
public class UsageSummaryController {

    @Autowired
    private UsageUserService service;

    @NeedAop
    @GetMapping("/bgssai/user/usage/summary")
    public ApiResponse handle(
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate) {
        return service.summary(startDate, endDate);
    }
}
