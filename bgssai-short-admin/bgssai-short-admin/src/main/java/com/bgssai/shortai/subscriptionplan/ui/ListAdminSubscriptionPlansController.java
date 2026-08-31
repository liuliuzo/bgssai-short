package com.bgssai.shortai.subscriptionplan.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.subscriptionplan.service.SubscriptionPlanAdminService;

@RestController
public class ListAdminSubscriptionPlansController {

    @Autowired
    private SubscriptionPlanAdminService subscriptionPlanAdminService;

    @NeedAop
    @GetMapping("/bgssai/admin/subscription-plans/list")
    public ApiResponse handle() {
        return subscriptionPlanAdminService.list();
    }
}
