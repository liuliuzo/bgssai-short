package com.bgssai.shortai.paymerchant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.paymerchant.dto.ReqSaveAlipayConfig;

@RestController
public class SaveAlipayConfigController {

    @Autowired
    private AlipayConfigService alipayConfigService;

    @NeedAop
    @PostMapping("/bgssai/admin/pay/alipay-configs/save")
    public ApiResponse handle(@RequestBody ReqSaveAlipayConfig req) {
        return alipayConfigService.save(req);
    }
}
