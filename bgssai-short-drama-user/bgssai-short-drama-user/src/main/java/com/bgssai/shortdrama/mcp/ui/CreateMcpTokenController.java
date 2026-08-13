package com.bgssai.shortdrama.mcp.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortdrama.common.ApiResponse;
import com.bgssai.shortdrama.common.aop.NeedAop;
import com.bgssai.shortdrama.common.ratelimit.RateLimit;
import com.bgssai.shortdrama.mcp.service.McpTokenService;

/**
 * POST /bgssai/mcp-tokens/create — 创建 MCP 令牌，明文仅本次返回。
 */
@RestController
@NeedAop
public class CreateMcpTokenController {

    private final McpTokenService tokenService;

    public CreateMcpTokenController(McpTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/bgssai/mcp-tokens/create")
    @RateLimit(capacity = 10, refillPerMinute = 10, keyBy = RateLimit.Key.USER)
    public ApiResponse handle(@RequestBody(required = false) Map<String, Object> body) {
        String name = body == null || body.get("token_name") == null ? null : String.valueOf(body.get("token_name"));
        ApiResponse r = new ApiResponse();
        r.setCode("0");
        r.setSuccess(true);
        r.setMessage("令牌已创建，完整值仅此一次展示，请妥善保存");
        r.setResult(tokenService.create(name));
        return r;
    }
}
