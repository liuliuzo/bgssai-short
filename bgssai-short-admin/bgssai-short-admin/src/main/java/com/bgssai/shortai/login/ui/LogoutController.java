package com.bgssai.shortai.login.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.login.service.LogoutService;

/** POST /bgssai/admin/logout — no body. Clears the current admin session. */
@RestController
public class LogoutController {

    @Autowired
    private LogoutService logoutService;

    @NeedAop
    @PostMapping("/bgssai/admin/logout")
    public ApiResponse logout() {
        logoutService.logout();
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        return response;
    }
}
