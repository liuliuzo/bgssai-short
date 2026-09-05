package com.bgssai.shortai.login.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.login.dto.req.ReqSmsLogin;
import com.bgssai.shortai.login.dto.resp.RespUserLogin;
import com.bgssai.shortai.login.service.OtpLoginSupport;

import jakarta.validation.Valid;

@RestController
public class SmsLoginController {

    @Autowired
    private OtpLoginSupport otpLoginSupport;

    @PostMapping("/bgssai/user/login/sms")
    @RateLimit(capacity = 5, refillPerMinute = 5, keyBy = RateLimit.Key.IP)
    public ApiResponse handle(@RequestBody @Valid ReqSmsLogin req) {
        RespUserLogin data = otpLoginSupport.loginSms(req.getPhone(), req.getCode());
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(data);
        return response;
    }
}
