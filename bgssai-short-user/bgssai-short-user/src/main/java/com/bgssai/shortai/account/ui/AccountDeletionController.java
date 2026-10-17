package com.bgssai.shortai.account.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.account.dto.ReqDeleteAccount;
import com.bgssai.shortai.account.service.AccountDeletionService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;

@RestController
public class AccountDeletionController {

    @Autowired
    private AccountDeletionService accountDeletionService;

    @NeedAop
    @GetMapping("/bgssai/user/account/deletion/preview")
    public ApiResponse preview() {
        return accountDeletionService.preview();
    }

    @NeedAop
    @PostMapping("/bgssai/user/account/deletion")
    public ApiResponse delete(@RequestBody ReqDeleteAccount req) {
        return accountDeletionService.delete(req);
    }
}
