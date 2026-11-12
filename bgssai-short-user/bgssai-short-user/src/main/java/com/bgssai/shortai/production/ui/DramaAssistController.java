package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ProductionDtos;
import com.bgssai.shortai.production.service.DramaAssistService;

@RestController
public class DramaAssistController {

    @Autowired
    private DramaAssistService service;

    @NeedAop
    @PostMapping("/bgssai/user/drama-assist/advise")
    public ApiResponse advise(@RequestBody ProductionDtos.DramaAssistReq req) {
        return service.advise(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/drama-assist/apply")
    public ApiResponse apply(@RequestBody ProductionDtos.DramaAssistApplyReq req) {
        return service.apply(req);
    }
}
