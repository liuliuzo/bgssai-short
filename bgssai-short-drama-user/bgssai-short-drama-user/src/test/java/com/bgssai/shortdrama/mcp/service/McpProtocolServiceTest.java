package com.bgssai.shortdrama.mcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bgssai.shortdrama.mcp.domain.McpCaller;
import com.fasterxml.jackson.databind.JsonNode;

class McpProtocolServiceTest {

    @Test
    void initializeNegotiatesProtocolVersion() throws Exception {
        McpProtocolService svc = new McpProtocolService(null);
        McpCaller caller = new McpCaller("u1");
        JsonNode resp = svc.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"t\",\"version\":\"0\"}}}",
                caller);
        assertEquals("2.0", resp.get("jsonrpc").asText());
        assertEquals("2025-06-18", resp.get("result").get("protocolVersion").asText());
        assertTrue(resp.get("result").get("capabilities").has("tools"));
    }

    @Test
    void batchArrayRejected() {
        McpProtocolService svc = new McpProtocolService(null);
        McpCaller caller = new McpCaller("u1");
        JsonNode resp = svc.handleMessage("[{}]", caller);
        assertEquals(-32600, resp.get("error").get("code").asInt());
    }
}
