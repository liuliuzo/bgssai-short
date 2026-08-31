package com.bgssai.shortai.subscription.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.subscription.service.ListUserSubscriptionOrdersService;

@RestController
public class ListUserSubscriptionOrdersController {

    @Autowired
    private ListUserSubscriptionOrdersService listUserSubscriptionOrdersService;

    @NeedAop
    @GetMapping("/bgssai/user/subscription/orders")
    public ApiResponse handle() {
        return listUserSubscriptionOrdersService.list();
    }
}
