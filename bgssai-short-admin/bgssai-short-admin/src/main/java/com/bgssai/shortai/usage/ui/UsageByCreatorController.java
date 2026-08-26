package com.bgssai.shortai.usage.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.usage.service.UsageAdminService;

@RestController
public class UsageByCreatorController {

    @Autowired
    private UsageAdminService service;

    @NeedAop
    @GetMapping("/bgssai/admin/usage/by-creator")
    public PageResponse handle(
            @RequestParam(value = "page_num", defaultValue = "1") int pageNum,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate) {
        return service.byCreator(pageNum, pageSize, startDate, endDate);
    }
}
