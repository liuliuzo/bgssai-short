package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.SourceService;

@RestController
public class SourceDetailController {

    @Autowired
    private SourceService service;

    @NeedAop
    @GetMapping("/bgssai/user/sources/detail")
    public PageResponse handle(@RequestParam(value = "source_id", required = false) String source_id,
            @RequestParam(value = "page_num", required = false) Integer page_num,
            @RequestParam(value = "page_size", required = false) Integer page_size) {
        return service.detail(source_id, page_num, page_size);
    }
}
