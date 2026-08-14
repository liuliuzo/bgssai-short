package com.bgssai.shortai.login.ui;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.login.dto.req.ReqOAuthAuthorizeUrl;
import com.bgssai.shortai.login.dto.resp.RespOAuthAuthorizeUrl;
import com.bgssai.shortai.login.service.OAuthAuthorizeUrlService;

/** 三方授权跳转地址。POST /bgssai/user/oauth/authorize-url，公开接口，首次授权即自动建号。 */
@RestController
public class OAuthAuthorizeUrlController {

    @Autowired private OAuthAuthorizeUrlService oauthAuthorizeUrlService;

    @PostMapping("/bgssai/user/oauth/authorize-url")
    @RateLimit(capacity = 20, refillPerMinute = 20, keyBy = RateLimit.Key.IP)
    public ApiResponse handle(@RequestBody @Valid ReqOAuthAuthorizeUrl req) {
        RespOAuthAuthorizeUrl data = oauthAuthorizeUrlService.build(req);
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(data);
        return response;
    }
}
