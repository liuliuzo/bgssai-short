package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.QualityService;

/** Kept for compatibility; delegates to QualityService. */
@RestController
public class ListQualityTemplatesController {

    @Autowired
    private QualityService service;

    @NeedAop
    @GetMapping("/bgssai/user/quality/templates")
    public ApiResponse handle(@RequestParam(value = "genre", required = false) String genre) {
        return service.templates(genre);
    }
}
