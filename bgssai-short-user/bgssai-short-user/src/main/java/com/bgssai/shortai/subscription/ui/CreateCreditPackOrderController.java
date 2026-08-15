package com.bgssai.shortai.subscription.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.subscription.dto.ReqCreateCreditPackOrder;
import com.bgssai.shortai.subscription.service.CreatePayOrderService;

@RestController
public class CreateCreditPackOrderController {

    @Autowired
    private CreatePayOrderService createPayOrderService;

    @NeedAop
    @PostMapping("/bgssai/user/credit-packs/orders")
    public ApiResponse handle(@RequestBody ReqCreateCreditPackOrder req) {
        return createPayOrderService.createCreditPack(
                req == null ? null : req.getPackCode(),
                req == null ? null : req.getPaymentChannel());
    }
}
