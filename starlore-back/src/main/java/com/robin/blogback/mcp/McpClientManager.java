package com.robin.blogback.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.service.AiConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class McpClientManager {

    private static final Logger log = LoggerFactory.getLogger(McpClientManager.class);

    private final ObjectMapper objectMapper;
    private final AiConfigService aiConfigService;
    private final String serversJson;
    private final String zhipuApiKey;
    private final Duration timeout;
    private final Map<String, McpClient> clients = new LinkedHashMap<>();

    public McpClientManager(
            ObjectMapper objectMapper,
            AiConfigService aiConfigService,
            @Value("${app.mcp.servers-json:[]}") String serversJson,
            @Value("${app.mcp.zhipu.api-key:}") String zhipuApiKey,
            @Value("${app.mcp.request-timeout:30s}") Duration timeout) {
        this.objectMapper = objectMapper;
        this.aiConfigService = aiConfigService;
        this.serversJson = serversJson;
        this.zhipuApiKey = zhipuApiKey;
        this.timeout = timeout;
    }

    @PostConstruct
    public void initialize() {
        try {
            List<McpServerConfig> configs = new ArrayList<>();
            if (serversJson != null && !serversJson.isBlank() && !"[]".equals(serversJson.trim())) {
                configs.addAll(objectMapper.readValue(
                        serversJson, new TypeReference<List<McpServerConfig>>() {}));
            }
            if (configs.isEmpty()) {
                log.info("[MCP] No stdio servers configured");
            }
            for (McpServerConfig rawConfig : configs) {
                McpServerConfig config = resolveEnvironment(rawConfig);
                if (config.name() == null || config.name().isBlank()) {
                    log.warn("[MCP] Ignoring server without a name");
                    continue;
                }
                clients.put(config.name(), new McpStdioClient(config, objectMapper, timeout));
            }
            String effectiveZhipuApiKey = resolveZhipuApiKey();
            if (StringUtils.hasText(effectiveZhipuApiKey) && !clients.containsKey("zhipu-web-search")) {
                String endpoint = "https://open.bigmodel.cn/api/mcp-broker/proxy/web-search/mcp?Authorization="
                        + URLEncoder.encode(effectiveZhipuApiKey, StandardCharsets.UTF_8);
                clients.put("zhipu-web-search",
                        new McpRemoteHttpClient("zhipu-web-search", URI.create(endpoint), objectMapper, timeout));
            } else if (!StringUtils.hasText(effectiveZhipuApiKey)) {
                log.info("[MCP] Zhipu Web Search is waiting for ZHIPU_API_KEY or enabled zhipu-embedding config");
            }
            log.info("[MCP] Configured {} external server(s): {}", clients.size(), clients.keySet());
        } catch (Exception e) {
            log.error("[MCP] Invalid APP_MCP_SERVERS_JSON configuration: {}", e.getMessage());
        }
    }

    private String resolveZhipuApiKey() {
        if (StringUtils.hasText(zhipuApiKey)) return zhipuApiKey;
        try {
            AiConfig config = aiConfigService.getConfigByKey("zhipu-embedding");
            if (config != null && Boolean.TRUE.equals(config.getEnabled())
                    && StringUtils.hasText(config.getApiKey())) {
                log.info("[MCP] Reusing enabled Zhipu account configuration for Web Search");
                return config.getApiKey();
            }
        } catch (Exception e) {
            log.warn("[MCP] Could not load Zhipu account configuration: {}", e.getMessage());
        }
        return "";
    }

    private McpServerConfig resolveEnvironment(McpServerConfig config) {
        Map<String, String> resolved = new LinkedHashMap<>();
        config.env().forEach((key, value) -> {
            String resolvedValue = value;
            if (value != null && value.startsWith("${") && value.endsWith("}")) {
                String variableName = value.substring(2, value.length() - 1);
                resolvedValue = System.getenv(variableName);
                if (resolvedValue == null) {
                    log.warn("[MCP] Environment variable '{}' required by server '{}' is not set",
                            variableName, config.name());
                    resolvedValue = "";
                }
            }
            resolved.put(key, resolvedValue);
        });
        return new McpServerConfig(config.name(), config.command(), config.args(), resolved);
    }

    public synchronized List<McpToolDescriptor> listTools() {
        if (clients.isEmpty()) return Collections.emptyList();
        List<McpToolDescriptor> tools = new ArrayList<>();
        for (McpClient client : clients.values()) {
            try {
                tools.addAll(client.listTools());
            } catch (Exception e) {
                log.warn("[MCP] Failed to list tools from '{}': {}", client.name(), e.getMessage());
            }
        }
        return tools;
    }

    public JsonNode callTool(String serverName, String toolName, JsonNode arguments) throws Exception {
        McpClient client = clients.get(serverName);
        if (client == null) {
            throw new IllegalArgumentException("Unknown MCP server: " + serverName);
        }
        return client.callTool(toolName, arguments);
    }

    public List<String> configuredServers() {
        return List.copyOf(clients.keySet());
    }

    @PreDestroy
    public void close() {
        clients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                log.debug("[MCP] Failed to close '{}': {}", client.name(), e.getMessage());
            }
        });
        clients.clear();
    }
}
