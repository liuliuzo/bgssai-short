package com.bgssai.shortdrama.mcp.ui;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortdrama.common.ApiResponse;
import com.bgssai.shortdrama.common.aop.NeedAop;
import com.bgssai.shortdrama.mcp.service.McpTokenService;

/**
 * POST /bgssai/mcp-tokens/list — 我的 MCP 令牌掩码列表。
 */
@RestController
@NeedAop
public class ListMcpTokensController {

    private final McpTokenService tokenService;

    public ListMcpTokensController(McpTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/bgssai/mcp-tokens/list")
    public ApiResponse handle(@RequestBody(required = false) Object ignored) {
        ApiResponse r = new ApiResponse();
        r.setCode("0");
        r.setSuccess(true);
        r.setMessage("OK");
        r.setResult(tokenService.listMine());
        return r;
    }
}
