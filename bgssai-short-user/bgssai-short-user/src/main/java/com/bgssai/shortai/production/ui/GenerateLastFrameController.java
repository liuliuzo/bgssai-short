package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.FrameLinkDtos.LastFrameGenerateReq;
import com.bgssai.shortai.production.service.FrameLinkService;

@RestController
public class GenerateLastFrameController {

    @Autowired
    private FrameLinkService service;

    @NeedAop
    @PostMapping("/bgssai/user/shots/last-frame-generate")
    public ApiResponse handle(@RequestBody LastFrameGenerateReq req) {
        return service.generateLastFrame(req);
    }
}
