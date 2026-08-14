package com.bgssai.shortai.common;

/**
 * Unified response wrapper.
 */
public class ApiResponse {

    private String code;

    private String message;

    private boolean success;

    private Object result;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ApiResponse [code=" + code + ", message=" + message + ", success=" + success + ", result=" + result + "]";
    }
}
