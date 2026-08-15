package com.bgssai.shortai.creditpack.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.creditpack.dto.ReqSaveCreditPack;
import com.bgssai.shortai.creditpack.service.CreditPackAdminService;

@RestController
public class SaveCreditPackController {

    @Autowired
    private CreditPackAdminService creditPackAdminService;

    @NeedAop
    @PostMapping("/bgssai/admin/credit-packs/save")
    public ApiResponse handle(@RequestBody ReqSaveCreditPack req) {
        return creditPackAdminService.save(req);
    }
}
