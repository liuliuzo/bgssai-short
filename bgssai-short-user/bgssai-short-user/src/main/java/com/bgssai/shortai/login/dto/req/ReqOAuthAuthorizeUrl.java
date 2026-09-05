package com.bgssai.shortai.login.dto.req;

import jakarta.validation.constraints.NotBlank;

/** 请求三方授权跳转地址（FR-LOGIN-EXT）。platform ∈ wechat/alipay/douyin/baidu/bgssai。 */
public class ReqOAuthAuthorizeUrl {

    @NotBlank(message = "platform must not be blank")
    private String platform;

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}

