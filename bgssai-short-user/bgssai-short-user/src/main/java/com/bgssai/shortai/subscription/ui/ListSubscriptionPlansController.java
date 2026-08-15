package com.bgssai.shortai.subscription.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.subscription.service.ListSubscriptionPlansService;

@RestController
public class ListSubscriptionPlansController {

    @Autowired
    private ListSubscriptionPlansService listSubscriptionPlansService;

    @NeedAop
    @GetMapping("/bgssai/user/subscription/plans")
    public ApiResponse handle() {
        return listSubscriptionPlansService.list();
    }
}
