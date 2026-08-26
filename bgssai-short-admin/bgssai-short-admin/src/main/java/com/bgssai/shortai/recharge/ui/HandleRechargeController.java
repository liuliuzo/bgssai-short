package com.bgssai.shortai.recharge.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.recharge.dto.ReqHandleRecharge;
import com.bgssai.shortai.recharge.service.RechargeAdminService;

@RestController
public class HandleRechargeController {

    @Autowired
    private RechargeAdminService service;

    @NeedAop
    @PostMapping("/bgssai/admin/recharge/handle")
    public ApiResponse handle(@RequestBody ReqHandleRecharge req) {
        return service.handle(req);
    }
}
