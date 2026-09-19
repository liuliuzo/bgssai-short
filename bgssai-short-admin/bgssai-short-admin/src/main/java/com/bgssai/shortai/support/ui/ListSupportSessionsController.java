package com.bgssai.shortai.support.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.support.service.SupportAdminService;

@RestController
@NeedAop
public class ListSupportSessionsController {

    private final SupportAdminService supportAdminService;

    public ListSupportSessionsController(SupportAdminService supportAdminService) {
        this.supportAdminService = supportAdminService;
    }

    @PostMapping("/bgssai/admin/support/sessions/list")
    public PageResponse handle(@RequestBody(required = false) Map<String, Object> body) {
        return supportAdminService.list(body);
    }
}
