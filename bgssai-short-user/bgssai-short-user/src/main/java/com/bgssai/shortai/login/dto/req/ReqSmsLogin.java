package com.bgssai.shortai.login.dto.req;

import jakarta.validation.constraints.NotBlank;

public class ReqSmsLogin {
    @NotBlank(message = "phone must not be blank")
    private String phone;
    @NotBlank(message = "code must not be blank")
    private String code;
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
