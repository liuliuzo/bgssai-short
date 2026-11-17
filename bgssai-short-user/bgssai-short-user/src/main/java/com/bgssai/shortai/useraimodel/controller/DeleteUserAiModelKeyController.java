package com.bgssai.shortai.useraimodel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyIdRequest;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bgssai/user/ai-model-keys")
public class DeleteUserAiModelKeyController {

    @Autowired
    private UserAiModelKeyService userAiModelKeyService;

    @NeedAop
    @PostMapping("/delete")
    public ApiResponse handle(@Valid @RequestBody UserAiModelKeyIdRequest req) {
        return Responses.ok(userAiModelKeyService.delete(req));
    }
}
