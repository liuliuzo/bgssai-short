package com.bgssai.shortai.login.dto.req;

import jakarta.validation.constraints.NotBlank;

public class ReqEmailSend {
    @NotBlank(message = "email must not be blank")
    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
