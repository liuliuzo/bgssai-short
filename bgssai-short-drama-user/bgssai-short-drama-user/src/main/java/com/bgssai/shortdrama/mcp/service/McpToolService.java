package com.bgssai.shortdrama.mcp.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bgssai.shortdrama.common.PageResponse;
import com.bgssai.shortdrama.common.exception.BizException;
import com.bgssai.shortdrama.health.dto.LivenessReport;
import com.bgssai.shortdrama.health.dto.ReadinessComponent;
import com.bgssai.shortdrama.health.dto.ReadinessReport;
import com.bgssai.shortdrama.health.service.QueryLivenessService;
import com.bgssai.shortdrama.health.service.QueryReadinessService;
import com.bgssai.shortdrama.loginlog.dto.LoginLogItem;
import com.bgssai.shortdrama.loginlog.service.ListLoginLogsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class McpToolService {

    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);

    public static final class ToolResult {
        private final String text;
        private final boolean error;

        private ToolResult(String text, boolean error) {
            this.text = text;
            this.error = error;
        }

        static ToolResult ok(String text) {
            return new ToolResult(text, false);
        }

        static ToolResult error(String text) {
            return new ToolResult(text, true);
        }

        public String text() {
            return text;
        }

        public boolean isError() {
            return error;
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final ListLoginLogsService listLoginLogsService;
    private final QueryLivenessService queryLivenessService;
    private final QueryReadinessService queryReadinessService;

    public McpToolService(ListLoginLogsService listLoginLogsService,
            QueryLivenessService queryLivenessService,
            QueryReadinessService queryReadinessService) {
        this.listLoginLogsService = listLoginLogsService;
        this.queryLivenessService = queryLivenessService;
        this.queryReadinessService = queryReadinessService;
    }

    public boolean hasTool(String name) {
        return switch (name) {
            case "list_login_logs", "get_liveness", "get_readiness" -> true;
            default -> false;
        };
    }

    public ArrayNode listToolDefinitions() {
        ArrayNode tools = mapper.createArrayNode();
        tools.add(tool("list_login_logs", "列出当前用户本人登录日志（分页）。身份取自令牌，勿传 user_id。",
                schema(List.of(),
                        prop("page_num", "integer", "页码，从 1 开始，默认 1"),
                        prop("page_size", "integer", "每页条数，默认 20，最大 50"))));
        tools.add(tool("get_liveness", "进程存活探针（不触碰数据库）。", schema(List.of())));
        tools.add(tool("get_readiness", "就绪探针（critical 组件）。", schema(List.of())));
        return tools;
    }

    public ToolResult callTool(String name, JsonNode args) {
        try {
            return switch (name) {
                case "list_login_logs" -> ToolResult.ok(loginLogsJson(
                        listLoginLogsService.list(intArg(args, "page_num", 1), intArg(args, "page_size", 20))));
                case "get_liveness" -> ToolResult.ok(livenessJson(queryLivenessService.liveness()));
                case "get_readiness" -> ToolResult.ok(readinessJson(queryReadinessService.readiness()));
                default -> ToolResult.error("未知工具：" + name);
            };
        } catch (IllegalArgumentException | BizException e) {
            log.info("mcp.tool business fail name={} msg={}", name, e.getMessage());
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            log.warn("mcp.tool error name={} err={}", name, e.toString());
            return ToolResult.error("工具执行失败");
        }
    }

    private String loginLogsJson(PageResponse page) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("page_num", page.getPageNum());
        out.put("page_size", page.getPageSize());
        out.put("total_size", page.getTotalSize());
        List<Map<String, Object>> logs = new ArrayList<>();
        if (page.getResult() instanceof List<?> rows) {
            for (Object row : rows) {
                if (row instanceof LoginLogItem item) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", item.getId());
                    m.put("user_id", item.getUserId());
                    m.put("action", item.getAction());
                    m.put("ip_address", item.getIpAddress());
                    m.put("user_agent", item.getUserAgent());
                    m.put("create_time", item.getCreateTime());
                    logs.add(m);
                }
            }
        }
        out.put("logs", logs);
        return toJson(out);
    }

    private String livenessJson(LivenessReport report) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", report.getStatus() == null ? null : report.getStatus().name());
        out.put("app", report.getApp());
        out.put("profile", report.getProfile());
        out.put("uptime_seconds", report.getUptimeSeconds());
        out.put("checked_at", report.getCheckedAt());
        return toJson(out);
    }

    private String readinessJson(ReadinessReport report) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", report.getStatus() == null ? null : report.getStatus().name());
        out.put("app", report.getApp());
        out.put("checked_at", report.getCheckedAt());
        List<Map<String, Object>> components = new ArrayList<>();
        if (report.getComponents() != null) {
            for (ReadinessComponent c : report.getComponents()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", c.getName());
                m.put("status", c.getStatus() == null ? null : c.getStatus().name());
                components.add(m);
            }
        }
        out.put("components", components);
        return toJson(out);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private ObjectNode tool(String name, String description, ObjectNode inputSchema) {
        ObjectNode t = mapper.createObjectNode();
        t.put("name", name);
        t.put("description", description);
        t.set("inputSchema", inputSchema);
        return t;
    }

    private ObjectNode schema(List<String> required, ObjectNode... props) {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "object");
        ObjectNode properties = s.putObject("properties");
        for (ObjectNode p : props) {
            String n = p.remove("__name").asText();
            properties.set(n, p);
        }
        if (!required.isEmpty()) {
            ArrayNode req = s.putArray("required");
            required.forEach(req::add);
        }
        return s;
    }

    private ObjectNode prop(String name, String type, String description) {
        ObjectNode p = mapper.createObjectNode();
        p.put("__name", name);
        p.put("type", type);
        p.put("description", description);
        return p;
    }

    private static int intArg(JsonNode args, String name, int def) {
        if (args == null || !args.hasNonNull(name)) {
            return def;
        }
        return args.get(name).asInt(def);
    }
}
