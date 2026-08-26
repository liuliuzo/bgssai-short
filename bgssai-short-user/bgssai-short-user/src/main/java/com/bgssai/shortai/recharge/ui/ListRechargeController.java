package com.bgssai.shortai.recharge.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.recharge.service.RechargeUserService;

@RestController
public class ListRechargeController {

    @Autowired
    private RechargeUserService service;

    @NeedAop
    @GetMapping("/bgssai/user/recharge/list")
    public ApiResponse handle() {
        return service.list();
    }
}
