package com.bgssai.shortdrama.mcp.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortdrama.common.ApiResponse;
import com.bgssai.shortdrama.common.aop.NeedAop;
import com.bgssai.shortdrama.mcp.service.McpTokenService;

/**
 * POST /bgssai/mcp-tokens/revoke — 吊销 MCP 令牌（软删）。
 */
@RestController
@NeedAop
public class RevokeMcpTokenController {

    private final McpTokenService tokenService;

    public RevokeMcpTokenController(McpTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/bgssai/mcp-tokens/revoke")
    public ApiResponse handle(@RequestBody Map<String, Object> body) {
        Long id = null;
        if (body != null && body.get("id") != null) {
            id = Long.valueOf(String.valueOf(body.get("id")));
        }
        tokenService.revoke(id);
        ApiResponse r = new ApiResponse();
        r.setCode("0");
        r.setSuccess(true);
        r.setMessage("令牌已吊销");
        return r;
    }
}
