package com.bgssai.shortdrama.mcp.service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bgssai.shortdrama.mcp.domain.McpCaller;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * MCP JSON-RPC 2.0 协议分发（Streamable HTTP、无状态、单条消息）。
 * 协议帧 camelCase 手工构造 ObjectNode，不受全局 SNAKE_CASE 影响。
 */
@Service
public class McpProtocolService {

    private static final Logger log = LoggerFactory.getLogger(McpProtocolService.class);

    static final String SERVER_NAME = "bgssai-short-drama-mcp";
    static final String SERVER_VERSION = "1.0.0";
    static final String LATEST_PROTOCOL_VERSION = "2025-06-18";
    private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS =
            Set.of("2025-06-18", "2025-03-26", "2024-11-05");

    private static final String SERVER_INSTRUCTIONS =
            "BGSSAI Short Drama MCP 服务：查询登录日志与存活/就绪探针。不发明短剧业务工具。";

    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int SERVER_ERROR = -32000;
    private static final long RATE_CAPACITY_PER_MIN = 60L;

    private final ObjectMapper mapper = new ObjectMapper();
    private final McpToolService mcpToolService;
    private final ConcurrentHashMap<String, Bucket> keyBuckets = new ConcurrentHashMap<>();

    public McpProtocolService(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    public JsonNode handleMessage(String body, McpCaller caller) {
        JsonNode msg;
        try {
            if (body == null || body.isBlank()) {
                return errorResponse(null, PARSE_ERROR, "请求体为空，需要 JSON-RPC 2.0 消息");
            }
            msg = mapper.readTree(body);
        } catch (Exception e) {
            return errorResponse(null, PARSE_ERROR, "JSON 解析失败");
        }

        if (msg.isArray()) {
            return errorResponse(null, INVALID_REQUEST, "不支持 JSON-RPC 批量数组（MCP 2025-06-18 已移除批量）");
        }
        if (!msg.isObject() || !msg.has("method") || !msg.get("method").isTextual()) {
            return errorResponse(msg.get("id"), INVALID_REQUEST, "非法 JSON-RPC 消息：缺少 method");
        }

        String method = msg.get("method").asText();
        JsonNode id = msg.get("id");
        JsonNode params = msg.get("params");

        if (method.startsWith("notifications/")) {
            log.info("MCP 通知 method={} userId={}", method, caller.getUserId());
            return null;
        }

        if (!"initialize".equals(method) && !"ping".equals(method)) {
            Bucket bucket = keyBuckets.computeIfAbsent(caller.getUserId(), k -> Bucket.builder()
                    .addLimit(Bandwidth.classic(RATE_CAPACITY_PER_MIN,
                            Refill.greedy(RATE_CAPACITY_PER_MIN, Duration.ofMinutes(1))))
                    .build());
            if (!bucket.tryConsume(1)) {
                log.warn("MCP 请求限流命中 method={} userId={}", method, caller.getUserId());
                return errorResponse(id, SERVER_ERROR, "请求过于频繁（每分钟上限 " + RATE_CAPACITY_PER_MIN + " 次），请稍后再试");
            }
        }

        log.info("MCP 请求 method={} userId={}", method, caller.getUserId());
        try {
            switch (method) {
                case "initialize":
                    return successResponse(id, buildInitializeResult(params));
                case "ping":
                    return successResponse(id, mapper.createObjectNode());
                case "tools/list":
                    return successResponse(id, buildToolsListResult());
                case "tools/call":
                    return handleToolsCall(id, params);
                default:
                    return errorResponse(id, METHOD_NOT_FOUND, "不支持的方法: " + method);
            }
        } catch (Exception e) {
            log.warn("MCP 处理异常 method={} userId={} error={}", method, caller.getUserId(), e.toString());
            return errorResponse(id, SERVER_ERROR, "服务端处理异常");
        }
    }

    private ObjectNode buildInitializeResult(JsonNode params) {
        String requested = params != null && params.hasNonNull("protocolVersion")
                ? params.get("protocolVersion").asText() : null;
        String negotiated = requested != null && SUPPORTED_PROTOCOL_VERSIONS.contains(requested)
                ? requested : LATEST_PROTOCOL_VERSION;
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", negotiated);
        result.putObject("capabilities").putObject("tools");
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("title", "BGSSAI Short Drama");
        serverInfo.put("version", SERVER_VERSION);
        result.put("instructions", SERVER_INSTRUCTIONS);
        return result;
    }

    private ObjectNode buildToolsListResult() {
        ObjectNode result = mapper.createObjectNode();
        result.set("tools", mcpToolService.listToolDefinitions());
        return result;
    }

    private JsonNode handleToolsCall(JsonNode id, JsonNode params) {
        if (params == null || !params.hasNonNull("name") || !params.get("name").isTextual()) {
            return errorResponse(id, INVALID_PARAMS, "tools/call 缺少工具名 name");
        }
        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");
        if (!mcpToolService.hasTool(toolName)) {
            return errorResponse(id, INVALID_PARAMS, "未知工具: " + toolName);
        }
        McpToolService.ToolResult toolResult = mcpToolService.callTool(toolName, arguments);
        ObjectNode result = mapper.createObjectNode();
        ObjectNode content = mapper.createObjectNode();
        content.put("type", "text");
        content.put("text", toolResult.text());
        result.putArray("content").add(content);
        result.put("isError", toolResult.isError());
        return successResponse(id, result);
    }

    private ObjectNode successResponse(JsonNode id, JsonNode result) {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        resp.set("id", id == null ? mapper.nullNode() : id);
        resp.set("result", result);
        return resp;
    }

    private ObjectNode errorResponse(JsonNode id, int code, String message) {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        resp.set("id", id == null ? mapper.nullNode() : id);
        ObjectNode error = resp.putObject("error");
        error.put("code", code);
        error.put("message", message);
        return resp;
    }
}
