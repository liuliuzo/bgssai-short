package com.bgssai.shortai.subscription.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.subscription.service.GetMySubscriptionService;

@RestController
public class GetMySubscriptionController {

    @Autowired
    private GetMySubscriptionService getMySubscriptionService;

    @NeedAop
    @GetMapping("/bgssai/user/subscription/me")
    public ApiResponse handle() {
        return getMySubscriptionService.me();
    }
}
