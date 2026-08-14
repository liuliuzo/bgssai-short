package com.bgssai.shortai.login.ui;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.login.dto.req.ReqOAuthCallback;
import com.bgssai.shortai.login.dto.resp.RespUserLogin;
import com.bgssai.shortai.login.service.OAuthLoginService;

/** 三方授权回调。POST /bgssai/user/oauth/callback，公开接口，首次授权即自动建号。 */
@RestController
public class OAuthCallbackController {

    @Autowired private OAuthLoginService oauthLoginService;

    @PostMapping("/bgssai/user/oauth/callback")
    @RateLimit(capacity = 10, refillPerMinute = 10, keyBy = RateLimit.Key.IP)
    public ApiResponse handle(@RequestBody @Valid ReqOAuthCallback req) {
        RespUserLogin data = oauthLoginService.loginOrRegister(req);
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(data);
        return response;
    }
}
