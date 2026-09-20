package com.bgssai.shortai.login.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.login.dto.resp.RespOAuthChannels;
import com.bgssai.shortai.login.service.OAuthChannelsService;

/** 已开通的三方登录渠道。GET /bgssai/user/oauth/channels，公开接口，登录页据此渲染入口。 */
@RestController
public class OAuthChannelsController {

    @Autowired private OAuthChannelsService oauthChannelsService;

    @GetMapping("/bgssai/user/oauth/channels")
    @RateLimit(capacity = 60, refillPerMinute = 60, keyBy = RateLimit.Key.IP)
    public ApiResponse handle() {
        RespOAuthChannels data = oauthChannelsService.list();
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(data);
        return response;
    }
}
