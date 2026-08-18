package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ProductionDtos;
import com.bgssai.shortai.production.service.ScriptService;

@RestController
public class ReadyScriptController {

    @Autowired
    private ScriptService service;

    @NeedAop
    @PostMapping("/bgssai/user/scripts/ready")
    public ApiResponse handle(@RequestBody ProductionDtos.ScriptGenerateReq req) {
        return service.ready(req);
    }
}
