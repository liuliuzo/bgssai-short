package com.bgssai.shortai.support.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.support.service.SupportUserService;

import jakarta.servlet.http.HttpServletRequest;

/** POST /bgssai/user/support/sessions/detail — 会话详情与消息。 */
@RestController
public class SupportSessionDetailController {

    private final SupportUserService supportUserService;

    public SupportSessionDetailController(SupportUserService supportUserService) {
        this.supportUserService = supportUserService;
    }

    @PostMapping("/bgssai/user/support/sessions/detail")
    @RateLimit(capacity = 30, refillPerMinute = 30, keyBy = RateLimit.Key.IP)
    public ApiResponse handle(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        ApiResponse r = new ApiResponse();
        r.setCode("0");
        r.setSuccess(true);
        r.setMessage("OK");
        r.setResult(supportUserService.detail(body, request));
        return r;
    }
}
