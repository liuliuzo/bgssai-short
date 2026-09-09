package com.bgssai.shortai.secret.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.secret.SysSecretAdminService;
import com.bgssai.shortai.secret.dto.ReqSysSecret;

@RestController
public class UpdateSysSecretController {

    @Autowired
    private SysSecretAdminService service;

    @NeedAop
    @PostMapping("/bgssai/admin/secrets/update")
    public ApiResponse handle(@RequestBody ReqSysSecret req) {
        return service.update(
                req == null ? null : req.getSecretKey(),
                req == null ? null : req.getSecretValue(),
                req == null ? null : req.getEnabled(),
                req == null ? null : req.getDescription());
    }
}
