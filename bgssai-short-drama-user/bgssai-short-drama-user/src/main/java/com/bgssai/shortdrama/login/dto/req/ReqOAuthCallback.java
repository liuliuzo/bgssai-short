package com.bgssai.shortdrama.login.dto.req;

import jakarta.validation.constraints.NotBlank;

/**
 * 三方授权回调请求（FR-LOGIN-EXT）。不接受客户端传入 redirect_uri——
 * 服务端只认自己在 OAuthProperties 里配置的固定值，防开放重定向。
 */
public class ReqOAuthCallback {

    @NotBlank(message = "platform must not be blank")
    private String platform;

    @NotBlank(message = "code must not be blank")
    private String code;

    @NotBlank(message = "state must not be blank")
    private String state;

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
