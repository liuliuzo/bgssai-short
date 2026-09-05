package com.bgssai.shortai.login.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.login.dto.req.ReqSmsSend;
import com.bgssai.shortai.login.dto.resp.RespCodeSend;
import com.bgssai.shortai.login.service.OtpLoginSupport;

import jakarta.validation.Valid;

@RestController
public class SendSmsCodeController {

    @Autowired
    private OtpLoginSupport otpLoginSupport;

    @PostMapping("/bgssai/user/sms/send")
    @RateLimit(capacity = 10, refillPerMinute = 10, keyBy = RateLimit.Key.IP)
    public ApiResponse handle(@RequestBody @Valid ReqSmsSend req) {
        RespCodeSend data = otpLoginSupport.sendSms(req.getPhone());
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(data);
        return response;
    }
}
