package com.bgssai.shortai.recharge.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.recharge.service.RechargeAdminService;

@RestController
public class ListRechargeController {

    @Autowired
    private RechargeAdminService service;

    @NeedAop
    @GetMapping("/bgssai/admin/recharge/list")
    public PageResponse handle(
            @RequestParam(value = "page_num", defaultValue = "1") int pageNum,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(value = "apply_status", required = false) String applyStatus) {
        return service.list(pageNum, pageSize, applyStatus);
    }
}
