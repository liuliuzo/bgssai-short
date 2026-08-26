package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.QualityAdminDtos.TemplateReq;
import com.bgssai.shortai.production.dto.QualityAdminDtos.ThresholdSaveReq;
import com.bgssai.shortai.production.service.QualityAdminService;

@RestController
public class QualityAdminControllers {

    @Autowired
    private QualityAdminService service;

    @NeedAop
    @GetMapping("/bgssai/admin/quality/templates/list")
    public ApiResponse list() {
        return service.listTemplates();
    }

    @NeedAop
    @PostMapping("/bgssai/admin/quality/templates/create")
    public ApiResponse create(@RequestBody TemplateReq req) {
        return service.create(req);
    }

    @NeedAop
    @PostMapping("/bgssai/admin/quality/templates/update")
    public ApiResponse update(@RequestBody TemplateReq req) {
        return service.update(req);
    }

    @NeedAop
    @PostMapping("/bgssai/admin/quality/templates/toggle-enabled")
    public ApiResponse toggle(@RequestBody TemplateReq req) {
        return service.toggleEnabled(req);
    }

    @NeedAop
    @PostMapping("/bgssai/admin/quality/threshold-save")
    public ApiResponse thresholdSave(@RequestBody ThresholdSaveReq req) {
        return service.thresholdSave(req);
    }
}
