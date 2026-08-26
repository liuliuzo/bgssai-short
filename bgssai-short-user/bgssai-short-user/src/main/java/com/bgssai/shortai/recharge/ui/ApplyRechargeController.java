package com.bgssai.shortai.recharge.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.recharge.dto.ReqRechargeApply;
import com.bgssai.shortai.recharge.service.RechargeUserService;

@RestController
public class ApplyRechargeController {

    @Autowired
    private RechargeUserService service;

    @NeedAop
    @PostMapping("/bgssai/user/recharge/apply")
    public ApiResponse handle(@RequestBody ReqRechargeApply req) {
        return service.apply(req);
    }
}
