package com.bgssai.shortai.login.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.login.dto.req.ReqUserLogin;
import com.bgssai.shortai.login.dto.resp.RespUserLogin;
import com.bgssai.shortai.login.service.UserLoginService;

/**
 * User password login.
 * POST /bgssai/user/login
 * Body: { login_type, identifier, password }
 * Returns result { jwt_token, role_code, role_name, user_id, user_name, real_name }
 * with role_code=USER on success.
 * Public endpoint (no prior token), so it does NOT carry @NeedAop.
 * Reference for the single-endpoint-per-Controller rule (Standards §1 / §3):
 * only binds params, applies the rate limit, calls the service, wraps the response.
 */
@RestController
public class UserLoginController {

    @Autowired
    private UserLoginService userLoginService;

    @PostMapping("/bgssai/user/login")
    @RateLimit(capacity = 5, refillPerMinute = 5, keyBy = RateLimit.Key.IP)
    public ApiResponse login(@RequestBody @Valid ReqUserLogin req, HttpServletRequest httpReq) {
        RespUserLogin data = userLoginService.login(req, httpReq);
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(data);
        return response;
    }
}
