package com.bgssai.shortai.subscriptionplan.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.subscriptionplan.dto.ReqSaveSubscriptionPlan;
import com.bgssai.shortai.subscriptionplan.service.SubscriptionPlanAdminService;

@RestController
public class SaveSubscriptionPlanController {

    @Autowired
    private SubscriptionPlanAdminService subscriptionPlanAdminService;

    @NeedAop
    @PostMapping("/bgssai/admin/subscription-plans/save")
    public ApiResponse handle(@RequestBody ReqSaveSubscriptionPlan req) {
        return subscriptionPlanAdminService.save(req);
    }
}
