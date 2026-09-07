package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.FrameLinkDtos.FrameLinkBatchReq;
import com.bgssai.shortai.production.service.FrameLinkService;

@RestController
public class BatchFrameLinkController {

    @Autowired
    private FrameLinkService service;

    @NeedAop
    @PostMapping("/bgssai/user/shots/frame-link-batch")
    public ApiResponse handle(@RequestBody FrameLinkBatchReq req) {
        return service.batch(req);
    }
}
