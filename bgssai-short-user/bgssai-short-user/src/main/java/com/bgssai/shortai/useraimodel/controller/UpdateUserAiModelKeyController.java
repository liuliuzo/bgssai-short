package com.bgssai.shortai.useraimodel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.useraimodel.dto.UpdateUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bgssai/user/ai-model-keys")
public class UpdateUserAiModelKeyController {

    @Autowired
    private UserAiModelKeyService userAiModelKeyService;

    @NeedAop
    @PostMapping("/update")
    public ApiResponse handle(@Valid @RequestBody UpdateUserAiModelKeyRequest req) {
        return Responses.ok(userAiModelKeyService.update(req));
    }
}
