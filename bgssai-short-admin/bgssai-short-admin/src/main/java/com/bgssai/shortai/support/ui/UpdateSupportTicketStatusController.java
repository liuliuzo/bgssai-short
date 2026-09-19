package com.bgssai.shortai.support.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.support.service.SupportAdminService;

/** POST /bgssai/admin/support/tickets/update_status — 更新工单状态。 */
@RestController
@NeedAop
public class UpdateSupportTicketStatusController {

    private final SupportAdminService supportAdminService;

    public UpdateSupportTicketStatusController(SupportAdminService supportAdminService) {
        this.supportAdminService = supportAdminService;
    }

    @PostMapping("/bgssai/admin/support/tickets/update_status")
    public ApiResponse handle(@RequestBody(required = false) Map<String, Object> body) {
        ApiResponse r = new ApiResponse();
        r.setCode("0");
        r.setSuccess(true);
        r.setMessage("OK");
        r.setResult(supportAdminService.updateTicketStatus(body));
        return r;
    }
}
