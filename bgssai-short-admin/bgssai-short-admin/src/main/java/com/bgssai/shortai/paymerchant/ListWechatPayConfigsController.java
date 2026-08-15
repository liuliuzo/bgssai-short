package com.bgssai.shortai.paymerchant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;

@RestController
public class ListWechatPayConfigsController {

    @Autowired
    private WechatPayConfigService wechatPayConfigService;

    @NeedAop
    @GetMapping("/bgssai/admin/pay/wechat-configs")
    public ApiResponse handle() {
        return wechatPayConfigService.list();
    }
}
