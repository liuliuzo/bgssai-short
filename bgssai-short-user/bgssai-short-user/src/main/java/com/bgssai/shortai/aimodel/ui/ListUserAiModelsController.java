package com.bgssai.shortai.aimodel.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;

@RestController
public class ListUserAiModelsController {

    @Autowired
    private AiModelResolveService aiModelResolveService;

    @NeedAop
    @GetMapping("/bgssai/user/ai-models/list")
    public ApiResponse handle(@RequestParam(value = "ability_type", required = false) String ability_type) {
        return aiModelResolveService.listEnabled(ability_type);
    }
}
