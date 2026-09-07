package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.SourceDtos.SourceImportReq;
import com.bgssai.shortai.production.service.SourceService;

@RestController
public class ImportSourceController {

    @Autowired
    private SourceService service;

    @NeedAop
    @PostMapping("/bgssai/user/sources/import")
    public ApiResponse handle(@RequestBody SourceImportReq req) {
        return service.importSource(req);
    }
}
