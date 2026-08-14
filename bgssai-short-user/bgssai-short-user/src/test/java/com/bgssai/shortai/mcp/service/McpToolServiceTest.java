package com.bgssai.shortai.mcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bgssai.shortai.health.service.QueryLivenessService;
import com.bgssai.shortai.health.service.QueryReadinessService;
import com.bgssai.shortai.loginlog.service.ListLoginLogsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

@ExtendWith(MockitoExtension.class)
class McpToolServiceTest {

    @Mock
    private ListLoginLogsService listLoginLogsService;
    @Mock
    private QueryLivenessService queryLivenessService;
    @Mock
    private QueryReadinessService queryReadinessService;

    private McpToolService service;

    @BeforeEach
    void setUp() {
        service = new McpToolService(listLoginLogsService, queryLivenessService, queryReadinessService);
    }

    @Test
    void listToolDefinitions_containsThreeTools() {
        ArrayNode tools = service.listToolDefinitions();
        assertEquals(3, tools.size());
        List<String> names = new ArrayList<>();
        for (JsonNode t : tools) {
            names.add(t.get("name").asText());
            assertEquals("object", t.get("inputSchema").get("type").asText());
        }
        assertTrue(names.containsAll(List.of("list_login_logs", "get_liveness", "get_readiness")));
        assertFalse(service.hasTool("create_episode"));
        assertTrue(service.hasTool("get_liveness"));
    }
}
