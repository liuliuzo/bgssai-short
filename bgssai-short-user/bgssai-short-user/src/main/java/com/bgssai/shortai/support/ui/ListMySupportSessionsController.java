package com.bgssai.shortai.support.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.support.service.SupportUserService;

import jakarta.servlet.http.HttpServletRequest;

/** POST /bgssai/user/support/sessions/mine — 我的客服会话列表。 */
@RestController
public class ListMySupportSessionsController {

    private final SupportUserService supportUserService;

    public ListMySupportSessionsController(SupportUserService supportUserService) {
        this.supportUserService = supportUserService;
    }

    @PostMapping("/bgssai/user/support/sessions/mine")
    @RateLimit(capacity = 30, refillPerMinute = 30, keyBy = RateLimit.Key.IP)
    public PageResponse handle(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return supportUserService.listMine(body, request);
    }
}
