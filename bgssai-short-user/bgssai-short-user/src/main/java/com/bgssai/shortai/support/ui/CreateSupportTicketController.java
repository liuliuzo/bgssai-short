package com.bgssai.shortai.support.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.support.service.SupportUserService;

import jakarta.servlet.http.HttpServletRequest;

/** POST /bgssai/user/support/tickets/create — 聊内提工单（公开，IP 限流）。 */
@RestController
public class CreateSupportTicketController {

    private final SupportUserService supportUserService;

    public CreateSupportTicketController(SupportUserService supportUserService) {
        this.supportUserService = supportUserService;
    }

    @PostMapping("/bgssai/user/support/tickets/create")
    @RateLimit(capacity = 10, refillPerMinute = 10, keyBy = RateLimit.Key.IP)
    public ApiResponse handle(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        ApiResponse r = new ApiResponse();
        r.setCode("0");
        r.setSuccess(true);
        r.setMessage("OK");
        r.setResult(supportUserService.createTicket(body, request));
        return r;
    }
}
