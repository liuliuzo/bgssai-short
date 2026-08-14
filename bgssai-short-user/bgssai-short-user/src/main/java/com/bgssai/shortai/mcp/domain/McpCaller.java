package com.bgssai.shortai.mcp.domain;

import com.bgssai.shortai.common.aop.UserContext;

/**
 * MCP 调用身份：从 PAT 行恢复，注入 UserContext。工具入参不得自报身份。
 */
public class McpCaller {

    private final String userId;

    public McpCaller(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void applyToUserContext() {
        UserContext.setUserId(userId);
    }
}
