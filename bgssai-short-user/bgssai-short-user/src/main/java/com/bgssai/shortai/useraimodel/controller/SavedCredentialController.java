package com.bgssai.shortai.useraimodel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.useraimodel.dto.SavedCredentialRequest;
import com.bgssai.shortai.useraimodel.service.UserAiModelKeyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bgssai/user/ai-model-keys/credentials")
public class SavedCredentialController {

    @Autowired
    private UserAiModelKeyService userAiModelKeyService;


    @NeedAop
    @PostMapping("/create")
    public ApiResponse create(@Valid @RequestBody SavedCredentialRequest req) {
        return Responses.ok(userAiModelKeyService.createCredential(req));
    }

    @NeedAop
    @PostMapping("/update")
    public ApiResponse update(@Valid @RequestBody SavedCredentialRequest req) {
        return Responses.ok(userAiModelKeyService.updateCredential(req));
    }

    @NeedAop
    @PostMapping("/delete")
    public ApiResponse delete(@Valid @RequestBody SavedCredentialRequest req) {
        return Responses.ok(userAiModelKeyService.deleteCredential(req));
    }

    @NeedAop
    @PostMapping("/test")
    public ApiResponse test(@Valid @RequestBody SavedCredentialRequest req) {
        return Responses.ok(userAiModelKeyService.testCredential(req));
    }

    @NeedAop
    @PostMapping("/activate")
    public ApiResponse activate(@Valid @RequestBody SavedCredentialRequest req) {
        return Responses.ok(userAiModelKeyService.activateCredential(req));
    }
}
