package com.bgssai.shortai.paymerchant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.paymerchant.dto.ReqPayConfigId;

@RestController
public class SetAlipayConfigCollectionActiveController {

    @Autowired
    private AlipayConfigService alipayConfigService;

    @NeedAop
    @PostMapping("/bgssai/admin/pay/alipay-configs/set-collection-active")
    public ApiResponse handle(@RequestBody ReqPayConfigId req) {
        return alipayConfigService.setCollectionActive(req);
    }
}
