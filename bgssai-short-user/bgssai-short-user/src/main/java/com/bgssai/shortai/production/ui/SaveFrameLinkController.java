package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.FrameLinkDtos.FrameLinkSaveReq;
import com.bgssai.shortai.production.service.FrameLinkService;

@RestController
public class SaveFrameLinkController {

    @Autowired
    private FrameLinkService service;

    @NeedAop
    @PostMapping("/bgssai/user/shots/frame-link-save")
    public ApiResponse handle(@RequestBody FrameLinkSaveReq req) {
        return service.save(req);
    }
}
