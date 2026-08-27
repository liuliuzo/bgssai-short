package com.bgssai.shortai.subscription.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.subscription.dto.ReqPayOrderNo;
import com.bgssai.shortai.subscription.service.GetPayOrderStatusService;

@RestController
public class GetPayOrderStatusController {

    @Autowired
    private GetPayOrderStatusService getPayOrderStatusService;

    @NeedAop
    @PostMapping("/bgssai/user/subscription/order/status")
    public ApiResponse handle(@RequestBody ReqPayOrderNo req) {
        return getPayOrderStatusService.query(req == null ? null : req.getOrderNo());
    }
}
