package com.bgssai.shortai.quota.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.quota.dto.ReqAdjustQuota;
import com.bgssai.shortai.quota.service.AdjustQuotaService;

@RestController
public class AdjustQuotaController {

    @Autowired
    private AdjustQuotaService adjustQuotaService;

    @NeedAop
    @PostMapping("/bgssai/admin/creators/adjust-quota")
    public ApiResponse handle(@RequestBody ReqAdjustQuota req) {
        return adjustQuotaService.adjust(req);
    }
}
