package com.bgssai.shortai.login.dto.resp;

/**
 * Admin login response. Carries the JWT token and identity on success.
 * JSON fields (snake_case): jwt_token, role_code, role_name, user_id, user_name, real_name.
 * The shape aligns with the tenant user-app login contract so the shared front-end
 * foundation (axios Jwttoken header injection + role_code gate) works unchanged.
 */
public class RespAdminLogin {

    private String jwtToken;

    private String roleCode;

    private String roleName;

    private String userId;

    private String userName;

    private String realName;

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
