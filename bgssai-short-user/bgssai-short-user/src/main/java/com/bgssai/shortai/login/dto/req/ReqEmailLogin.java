package com.bgssai.shortai.login.dto.req;

import jakarta.validation.constraints.NotBlank;

public class ReqEmailLogin {
    @NotBlank(message = "email must not be blank")
    private String email;
    @NotBlank(message = "email_code must not be blank")
    private String emailCode;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailCode() { return emailCode; }
    public void setEmailCode(String emailCode) { this.emailCode = emailCode; }
}
