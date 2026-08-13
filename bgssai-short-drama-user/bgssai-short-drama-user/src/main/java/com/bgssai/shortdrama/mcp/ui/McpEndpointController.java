package com.bgssai.shortdrama.mcp.ui;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortdrama.common.aop.UserContext;
import com.bgssai.shortdrama.mcp.domain.McpCaller;
import com.bgssai.shortdrama.mcp.service.McpProtocolService;
import com.bgssai.shortdrama.mcp.service.McpTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * POST /api/mcp — MCP Streamable HTTP（无 JWT，PAT 自校验；路径在 /bgssai/ 之外，不经 RBAC）。
 */
@RestController
@RequestMapping("/api/mcp")
public class McpEndpointController {

    private final McpTokenService tokenService;
    private final McpProtocolService protocolService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpEndpointController(McpTokenService tokenService, McpProtocolService protocolService) {
        this.tokenService = tokenService;
        this.protocolService = protocolService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> handle(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String body) {
        McpCaller caller = resolveCaller(authorization);
        if (caller == null) {
            ObjectNode err = objectMapper.createObjectNode();
            err.put("jsonrpc", "2.0");
            err.putNull("id");
            ObjectNode e = err.putObject("error");
            e.put("code", -32001);
            e.put("message", "MCP 令牌无效或已吊销，请在 MCP 接入页重新创建");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
        caller.applyToUserContext();
        try {
            JsonNode resp = protocolService.handleMessage(body, caller);
            if (resp == null) {
                return ResponseEntity.accepted().build();
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp);
        } finally {
            UserContext.clear();
        }
    }

    @GetMapping
    public ResponseEntity<Void> handleGet() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> handleDelete() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    private McpCaller resolveCaller(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokenService.verify(authorization.substring(7).trim());
    }
}
