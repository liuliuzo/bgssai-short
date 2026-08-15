package com.bgssai.shortai.creditpack.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.creditpack.service.CreditPackAdminService;

@RestController
public class ListAdminCreditPacksController {

    @Autowired
    private CreditPackAdminService creditPackAdminService;

    @NeedAop
    @GetMapping("/bgssai/admin/credit-packs/list")
    public ApiResponse handle() {
        return creditPackAdminService.list();
    }
}
