package com.bgssai.shortai.account.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.account.dto.ReqBindContact;
import com.bgssai.shortai.account.dto.ReqBindOAuth;
import com.bgssai.shortai.account.dto.ReqUnbindLoginMethod;
import com.bgssai.shortai.account.service.AccountLoginMethodService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;

@RestController
public class AccountLoginMethodController {

    @Autowired
    private AccountLoginMethodService accountLoginMethodService;

    @NeedAop
    @GetMapping("/bgssai/user/account/login-methods")
    public ApiResponse list() {
        return accountLoginMethodService.listMethods();
    }

    @NeedAop
    @PostMapping("/bgssai/user/account/login-methods/bind-oauth")
    public ApiResponse bindOAuth(@RequestBody ReqBindOAuth req) {
        return accountLoginMethodService.bindOAuth(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/account/login-methods/bind-contact")
    public ApiResponse bindContact(@RequestBody ReqBindContact req) {
        return accountLoginMethodService.bindContact(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/account/login-methods/unbind")
    public ApiResponse unbind(@RequestBody ReqUnbindLoginMethod req) {
        return accountLoginMethodService.unbind(req);
    }
}
