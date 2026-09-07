package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ShotReorderDtos.ShotReorderReq;
import com.bgssai.shortai.production.service.ShotReorderService;

@RestController
public class ReorderShotsController {

    @Autowired
    private ShotReorderService service;

    @NeedAop
    @PostMapping("/bgssai/user/shots/reorder")
    public ApiResponse handle(@RequestBody ShotReorderReq req) {
        return service.reorder(req);
    }
}
