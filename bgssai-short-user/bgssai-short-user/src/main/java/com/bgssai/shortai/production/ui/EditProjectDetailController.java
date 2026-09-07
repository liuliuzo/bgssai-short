package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.EditProjectService;

@RestController
public class EditProjectDetailController {

    @Autowired
    private EditProjectService service;

    @NeedAop
    @GetMapping("/bgssai/user/edit-projects/detail")
    public ApiResponse handle(@RequestParam(value = "export_id", required = false) String export_id) {
        return service.detail(export_id);
    }
}
