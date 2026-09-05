package com.bgssai.shortai.login.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.login.dto.req.ReqEmailLogin;
import com.bgssai.shortai.login.dto.resp.RespUserLogin;
import com.bgssai.shortai.login.service.OtpLoginSupport;

import jakarta.validation.Valid;

@RestController
public class EmailLoginController {

    @Autowired
    private OtpLoginSupport otpLoginSupport;

    @PostMapping("/bgssai/user/login/email")
    @RateLimit(capacity = 5, refillPerMinute = 5, keyBy = RateLimit.Key.IP)
    public ApiResponse handle(@RequestBody @Valid ReqEmailLogin req) {
        RespUserLogin data = otpLoginSupport.loginEmail(req.getEmail(), req.getEmailCode());
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(data);
        return response;
    }
}
