package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.LookKnowledgeService;

@RestController
public class ListLookLibrariesController {

    @Autowired
    private LookKnowledgeService service;

    @NeedAop
    @GetMapping("/bgssai/user/look-libraries/list")
    public ApiResponse handle() {
        return service.list();
    }
}
