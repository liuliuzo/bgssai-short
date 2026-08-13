package com.bgssai.shortdrama.login.dto.req;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin login request. Password-only, no SMS/code flows.
 * identifier (the admin login name) + password in JSON body; identity
 * resolved from DB and carried thereafter via JWT.
 * login_type is accepted for contract symmetry with the tenant user-app login
 * but ignored here (the admin console supports password login only).
 */
public class ReqAdminLogin {

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
        return "ReqAdminLogin [identifier=" + identifier + ", loginType=" + loginType + "]";
    }
}
