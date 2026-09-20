package com.bgssai.shortai.mcp.ui;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpTokenPathAliasTest {

    @Test
    void list_create_revoke_register_canonical_and_legacy() {
        assertPaths(ListMcpTokensController.class, "/api/mcp/tokens/list", "/bgssai/mcp-tokens/list");
        assertPaths(CreateMcpTokenController.class, "/api/mcp/tokens/create", "/bgssai/mcp-tokens/create");
        assertPaths(RevokeMcpTokenController.class, "/api/mcp/tokens/revoke", "/bgssai/mcp-tokens/revoke");
    }

    private static void assertPaths(Class<?> type, String canonical, String alias) {
        PostMapping mapping = null;
        for (Method method : type.getDeclaredMethods()) {
            mapping = method.getAnnotation(PostMapping.class);
            if (mapping != null) {
                break;
            }
        }
        assertNotNull(mapping, type.getSimpleName() + " has no @PostMapping");
        List<String> paths = Arrays.asList(mapping.value());
        assertTrue(paths.contains(canonical), type.getSimpleName() + " missing " + canonical);
        assertTrue(paths.contains(alias), type.getSimpleName() + " missing " + alias);
    }
}
