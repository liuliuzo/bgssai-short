package com.bgssai.shortai.aimodel.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.aimodel.dto.ReqAiModel;
import com.bgssai.shortai.aimodel.service.AiModelAdminService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;

@RestController
public class UpdateAiModelController {

    @Autowired
    private AiModelAdminService service;

    @NeedAop
    @PostMapping("/bgssai/admin/ai-models/update")
    public ApiResponse handle(@RequestBody ReqAiModel req) {
        return service.update(req);
    }
}
