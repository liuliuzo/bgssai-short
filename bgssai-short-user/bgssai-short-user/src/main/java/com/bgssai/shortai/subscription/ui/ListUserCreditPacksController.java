package com.bgssai.shortai.subscription.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.subscription.service.ListUserCreditPacksService;

@RestController
public class ListUserCreditPacksController {

    @Autowired
    private ListUserCreditPacksService listUserCreditPacksService;

    @NeedAop
    @GetMapping("/bgssai/user/credit-packs")
    public ApiResponse handle() {
        return listUserCreditPacksService.list();
    }
}
