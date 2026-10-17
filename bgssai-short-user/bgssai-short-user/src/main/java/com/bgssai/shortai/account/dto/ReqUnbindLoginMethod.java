package com.bgssai.shortai.account.dto;

public class ReqUnbindLoginMethod {

    /** password / phone / email / oauth */
    private String methodType;

    /** Required when method_type=oauth: wechat/douyin/baidu/alipay/bgssai */
    private String platform;

    /** password or otp code for sensitive change re-auth */
    private String verifyMethod;

    private String password;

    private String code;

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getVerifyMethod() {
        return verifyMethod;
    }

    public void setVerifyMethod(String verifyMethod) {
        this.verifyMethod = verifyMethod;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
