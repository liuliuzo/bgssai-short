package com.bgssai.shortai.support.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.support.service.SupportAdminService;

@RestController
@NeedAop
public class AdminSupportSessionDetailController {

    private final SupportAdminService supportAdminService;

    public AdminSupportSessionDetailController(SupportAdminService supportAdminService) {
        this.supportAdminService = supportAdminService;
    }

    @PostMapping("/bgssai/admin/support/sessions/detail")
    public ApiResponse handle(@RequestBody(required = false) Map<String, Object> body) {
        ApiResponse r = new ApiResponse();
        r.setCode("0");
        r.setSuccess(true);
        r.setMessage("OK");
        r.setResult(supportAdminService.detail(body));
        return r;
    }
}
