package com.bgssai.shortai.useraimodel.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.useraimodel.dto.UpsertUserAiProviderKeyRequest;
import com.bgssai.shortai.useraimodel.dto.UserAiProviderKeyItem;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService;

/**
 * 用户自有厂商 API Key 管理接口（bgssai 前缀）。
 */
@RestController
public class UserAiKeysController {

    private final UserAiProviderKeyService userAiProviderKeyService;

    @Autowired
    public UserAiKeysController(UserAiProviderKeyService userAiProviderKeyService) {
        this.userAiProviderKeyService = userAiProviderKeyService;
    }

    @NeedAop
    @GetMapping("/bgssai/user/ai-keys")
    public ApiResponse list() {
        List<UserAiProviderKeyItem> items = userAiProviderKeyService.listForCurrentUser();
        return Responses.ok(Map.of("items", items));
    }

    @NeedAop
    @PostMapping("/bgssai/user/ai-keys")
    public ApiResponse upsert(@RequestBody UpsertUserAiProviderKeyRequest req) {
        return Responses.ok(userAiProviderKeyService.upsert(req));
    }

    @NeedAop
    @DeleteMapping("/bgssai/user/ai-keys/{providerCode}")
    public ApiResponse delete(@PathVariable("providerCode") String providerCode) {
        return Responses.ok(userAiProviderKeyService.deleteByProviderCode(providerCode));
    }
}
