package com.bgssai.shortai.useraimodel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.useraimodel.dto.TestUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;

@RestController
@RequestMapping("/bgssai/user/ai-model-keys")
public class TestUserAiModelKeyController {

    @Autowired
    private UserAiModelKeyService userAiModelKeyService;

    @NeedAop
    @PostMapping("/test")
    public ApiResponse handle(@RequestBody TestUserAiModelKeyRequest req) {
        return Responses.ok(userAiModelKeyService.test(req));
    }
}
