package com.bgssai.shortai.subscription.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.subscription.dto.ReqCreateSubscriptionOrder;
import com.bgssai.shortai.subscription.service.CreatePayOrderService;

@RestController
public class CreateSubscriptionOrderController {

    @Autowired
    private CreatePayOrderService createPayOrderService;

    @NeedAop
    @PostMapping("/bgssai/user/subscription/order/create")
    public ApiResponse handle(@RequestBody ReqCreateSubscriptionOrder req) {
        return createPayOrderService.createSubscription(
                req == null ? null : req.getPlanCode(),
                req == null ? null : req.getPaymentChannel());
    }
}
