package com.bgssai.shortai.aimodel.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.aimodel.service.AiModelAdminService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;

@RestController
public class ListAiModelsController {

    @Autowired
    private AiModelAdminService service;

    @NeedAop
    @GetMapping("/bgssai/admin/ai-models/list")
    public ApiResponse handle(@RequestParam(value = "ability_type", required = false) String abilityType) {
        return service.list(abilityType);
    }
}
