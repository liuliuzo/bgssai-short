package com.bgssai.shortai.account.dto;

public class ReqDeleteAccount {

    /** password / sms / email */
    private String verifyMethod;

    private String password;

    private String phone;

    private String email;

    private String code;

    /** Must be true after user reads consequences. */
    private Boolean acknowledgeConsequences;

    /** When open orders exist, client must re-confirm after seeing the block reason. */
    private Boolean forceDespiteOpenItems;

    private String reason;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getAcknowledgeConsequences() {
        return acknowledgeConsequences;
    }

    public void setAcknowledgeConsequences(Boolean acknowledgeConsequences) {
        this.acknowledgeConsequences = acknowledgeConsequences;
    }

    public Boolean getForceDespiteOpenItems() {
        return forceDespiteOpenItems;
    }

    public void setForceDespiteOpenItems(Boolean forceDespiteOpenItems) {
        this.forceDespiteOpenItems = forceDespiteOpenItems;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
