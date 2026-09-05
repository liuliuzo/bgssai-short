package com.bgssai.shortai.login.dto.req;

import jakarta.validation.constraints.NotBlank;

public class ReqSmsSend {
    @NotBlank(message = "phone must not be blank")
    private String phone;
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
