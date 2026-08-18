package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.WorkService;

@RestController
public class ListShortWorksController {

    @Autowired
    private WorkService service;

    @NeedAop
    @GetMapping("/bgssai/user/short-works/list")
    public PageResponse handle(@RequestParam(value = "page_num", required = false, defaultValue = "1") int page_num, @RequestParam(value = "page_size", required = false, defaultValue = "20") int page_size, @RequestParam(value = "work_status", required = false) String work_status, @RequestParam(value = "genre", required = false) String genre, @RequestParam(value = "keyword", required = false) String keyword) {
        return service.list(page_num, page_size, work_status, genre, keyword);
    }
}
