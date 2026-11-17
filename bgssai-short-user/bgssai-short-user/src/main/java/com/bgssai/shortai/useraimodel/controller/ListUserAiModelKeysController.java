package com.bgssai.shortai.useraimodel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;

@RestController
@RequestMapping("/bgssai/user/ai-model-keys")
public class ListUserAiModelKeysController {

    @Autowired
    private UserAiModelKeyService userAiModelKeyService;

    @NeedAop
    @GetMapping("/list")
    public ApiResponse handle(
            @RequestParam(value = "modality_type", required = false) String modalityType,
            @RequestParam(value = "ability_type", required = false) String abilityType) {
        return Responses.ok(userAiModelKeyService.listForCurrentUser(modalityType, abilityType));
    }
}
