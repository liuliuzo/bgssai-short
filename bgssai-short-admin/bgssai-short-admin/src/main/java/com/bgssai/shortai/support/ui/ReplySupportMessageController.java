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
public class ReplySupportMessageController {

    private final SupportAdminService supportAdminService;

    public ReplySupportMessageController(SupportAdminService supportAdminService) {
        this.supportAdminService = supportAdminService;
    }

    @PostMapping("/bgssai/admin/support/messages/reply")
    public ApiResponse handle(@RequestBody(required = false) Map<String, Object> body) {
        ApiResponse r = new ApiResponse();
        r.setCode("0");
        r.setSuccess(true);
        r.setMessage("OK");
        r.setResult(supportAdminService.reply(body));
        return r;
    }
}
