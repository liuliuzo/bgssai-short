package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.SourceDtos.SourceAdaptReq;
import com.bgssai.shortai.production.service.SourceService;

@RestController
public class EstimateSourceAdaptController {

    @Autowired
    private SourceService service;

    @NeedAop
    @PostMapping("/bgssai/user/sources/adapt-estimate")
    public ApiResponse handle(@RequestBody SourceAdaptReq req) {
        return service.adaptEstimate(req);
    }
}
