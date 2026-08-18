package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ToggleCreatorReq;
import com.bgssai.shortai.production.service.CreatorAdminService;

@RestController
public class ToggleCreatorEnabledController {

    @Autowired
    private CreatorAdminService creatorAdminService;

    @NeedAop
    @PostMapping("/bgssai/admin/creators/toggle-enabled")
    public ApiResponse handle(@RequestBody ToggleCreatorReq req) {
        return creatorAdminService.toggleEnabled(req);
    }
}
