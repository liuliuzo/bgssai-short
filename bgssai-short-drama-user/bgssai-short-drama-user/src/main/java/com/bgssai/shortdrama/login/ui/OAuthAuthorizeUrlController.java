package com.bgssai.shortdrama.login.ui;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortdrama.common.ApiResponse;
import com.bgssai.shortdrama.common.ratelimit.RateLimit;
import com.bgssai.shortdrama.login.dto.req.ReqOAuthAuthorizeUrl;
import com.bgssai.shortdrama.login.dto.resp.RespOAuthAuthorizeUrl;
import com.bgssai.shortdrama.login.service.OAuthAuthorizeUrlService;

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
