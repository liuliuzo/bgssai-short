package com.bgssai.shortai.login.dto.req;

import jakarta.validation.constraints.NotBlank;

/**
 * User login request. Password-only credential.
 * identifier (the login name) + password in JSON body; identity is resolved from the DB
 * and carried thereafter via JWT. login_type is accepted for contract symmetry (SMS / code
 * flows can be added later) but ignored here.
 */
public class ReqUserLogin {

    private String loginType;

    @NotBlank(message = "identifier must not be blank")
    private String identifier;

    @NotBlank(message = "password must not be blank")
    private String password;

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "ReqUserLogin [identifier=" + identifier + ", loginType=" + loginType + "]";
    }
}
