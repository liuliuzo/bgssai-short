package com.bgssai.shortai.login.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.login.dto.req.ReqAdminLogin;
import com.bgssai.shortai.login.dto.resp.RespAdminLogin;
import com.bgssai.shortai.login.service.AdminLoginService;

/**
 * Admin password login.
 * POST /bgssai/admin/login
 * Body: { login_type, identifier, password }
 * Returns result { jwt_token, role_code, role_name, user_id, user_name, real_name }
 * with role_code=PLATFORM_ADMIN on success.
 * Public endpoint (no prior token), so it does NOT carry @NeedAop.
 * Reference for the single-endpoint-per-Controller rule (Standards §1 / §3):
 * only binds params, applies the rate limit, calls the service, wraps the response.
 */
@RestController
public class AdminLoginController {

    @Autowired
    private AdminLoginService adminLoginService;

    @PostMapping("/bgssai/admin/login")
    @RateLimit(capacity = 5, refillPerMinute = 5, keyBy = RateLimit.Key.IP)
    public ApiResponse login(@RequestBody @Valid ReqAdminLogin req, HttpServletRequest httpReq) {
        RespAdminLogin data = adminLoginService.login(req, httpReq);
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(data);
        return response;
    }
}
