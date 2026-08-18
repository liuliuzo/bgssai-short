package com.bgssai.shortai.common;

public final class Responses {

    private Responses() {
    }

    public static ApiResponse ok(Object result) {
        ApiResponse resp = new ApiResponse();
        resp.setCode("0");
        resp.setMessage("OK");
        resp.setSuccess(true);
        resp.setResult(result);
        return resp;
    }
}
