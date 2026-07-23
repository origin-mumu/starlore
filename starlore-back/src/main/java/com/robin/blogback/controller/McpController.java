package com.robin.blogback.controller;

import com.robin.blogback.mcp.McpClientManager;
import com.robin.blogback.mcp.McpToolDescriptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/mcp")
public class McpController {

    private final McpClientManager mcpClientManager;

    public McpController(McpClientManager mcpClientManager) {
        this.mcpClientManager = mcpClientManager;
    }

    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        List<McpToolDescriptor> tools = mcpClientManager.listTools();
        return Map.of(
                "success", true,
                "servers", mcpClientManager.configuredServers(),
                "tools", tools,
                "count", tools.size()
        );
    }
}
