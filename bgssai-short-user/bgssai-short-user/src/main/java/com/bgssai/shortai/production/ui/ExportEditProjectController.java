package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.EditProjectDtos.EditProjectExportReq;
import com.bgssai.shortai.production.service.EditProjectService;

@RestController
public class ExportEditProjectController {

    @Autowired
    private EditProjectService service;

    @NeedAop
    @PostMapping("/bgssai/user/edit-projects/export")
    public ApiResponse handle(@RequestBody EditProjectExportReq req) {
        return service.export(req);
    }
}
