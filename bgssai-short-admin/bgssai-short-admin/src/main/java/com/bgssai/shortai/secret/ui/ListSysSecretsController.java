package com.bgssai.shortai.secret.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.secret.SysSecretAdminService;

@RestController
public class ListSysSecretsController {

    @Autowired
    private SysSecretAdminService service;

    @NeedAop
    @GetMapping("/bgssai/admin/secrets/list")
    public ApiResponse handle(@RequestParam(value = "key_prefix", required = false) String keyPrefix) {
        return service.list(keyPrefix);
    }
}
