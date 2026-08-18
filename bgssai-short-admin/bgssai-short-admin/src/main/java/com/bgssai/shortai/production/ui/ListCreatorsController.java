package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.CreatorAdminService;

@RestController
public class ListCreatorsController {

    @Autowired
    private CreatorAdminService creatorAdminService;

    @NeedAop
    @GetMapping("/bgssai/admin/creators/list")
    public PageResponse handle(@RequestParam(name = "page_num", defaultValue = "1") int pageNum,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "enabled_flag", required = false) Integer enabledFlag) {
        return creatorAdminService.list(pageNum, pageSize, keyword, enabledFlag);
    }
}
