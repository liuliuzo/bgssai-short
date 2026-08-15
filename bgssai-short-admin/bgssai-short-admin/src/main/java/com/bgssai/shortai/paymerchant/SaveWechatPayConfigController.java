package com.bgssai.shortai.paymerchant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.paymerchant.dto.ReqSaveWechatPayConfig;

@RestController
public class SaveWechatPayConfigController {

    @Autowired
    private WechatPayConfigService wechatPayConfigService;

    @NeedAop
    @PostMapping("/bgssai/admin/pay/wechat-configs/save")
    public ApiResponse handle(@RequestBody ReqSaveWechatPayConfig req) {
        return wechatPayConfigService.save(req);
    }
}
