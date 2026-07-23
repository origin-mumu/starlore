package com.robin.blogback.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP Streamable HTTP client. It also accepts an SSE-formatted response body,
 * which keeps it compatible with hosted MCP gateways using the older naming.
 */
public final class McpRemoteHttpClient implements McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpRemoteHttpClient.class);
    private static final String PROTOCOL_VERSION = "2025-03-26";

    private final String name;
    private final URI endpoint;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final AtomicLong requestIds = new AtomicLong();
    private volatile String sessionId;
    private volatile boolean initialized;

    public McpRemoteHttpClient(String name, URI endpoint, ObjectMapper objectMapper, Duration timeout) {
        this.name = name;
        this.endpoint = endpoint;
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String name() {
        return name;
    }

    private synchronized void ensureInitialized() throws Exception {
        if (initialized) return;
        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.set("capabilities", objectMapper.createObjectNode());
        ObjectNode clientInfo = params.putObject("clientInfo");
        clientInfo.put("name", "starlore");
        clientInfo.put("version", "1.0.0");
        JsonNode result = request("initialize", params);
        String negotiated = result.path("protocolVersion").asText(PROTOCOL_VERSION);
        notification("notifications/initialized");
        initialized = true;
        log.info("[MCP] Connected to remote server '{}', protocol={}", name, negotiated);
    }

    @Override
    public List<McpToolDescriptor> listTools() throws Exception {
        ensureInitialized();
        JsonNode result = request("tools/list", objectMapper.createObjectNode());
        List<McpToolDescriptor> tools = new ArrayList<>();
        for (JsonNode tool : result.path("tools")) {
            tools.add(new McpToolDescriptor(
                    name,
                    tool.path("name").asText(),
                    tool.path("description").asText(""),
                    tool.path("inputSchema")
            ));
        }
        return tools;
    }

    @Override
    public JsonNode callTool(String toolName, JsonNode arguments) throws Exception {
        ensureInitialized();
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", arguments == null ? objectMapper.createObjectNode() : arguments);
        return request("tools/call", params);
    }

    private JsonNode request(String method, JsonNode params) throws Exception {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("jsonrpc", "2.0");
        message.put("id", requestIds.incrementAndGet());
        message.put("method", method);
        message.set("params", params);
        JsonNode response = send(message);
        if (response.has("error")) {
            throw new IllegalStateException("MCP error from " + name + ": " + response.get("error"));
        }
        return response.path("result");
    }

    private void notification(String method) throws Exception {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("jsonrpc", "2.0");
        message.put("method", method);
        send(message);
    }

    private JsonNode send(JsonNode message) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", PROTOCOL_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(message)));
        if (sessionId != null) builder.header("Mcp-Session-Id", sessionId);

        HttpResponse<String> response = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofString());
        response.headers().firstValue("Mcp-Session-Id").ifPresent(value -> sessionId = value);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("MCP HTTP " + response.statusCode() + " from " + name);
        }
        if (response.body() == null || response.body().isBlank()) {
            return objectMapper.createObjectNode();
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (contentType.contains("text/event-stream") || response.body().startsWith("event:")) {
            return parseSse(response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        // Some hosted gateways return their own JSON error envelope with HTTP 200.
        if (!json.has("jsonrpc") && json.has("success") && !json.path("success").asBoolean()) {
            throw new IllegalStateException("MCP gateway rejected request: " + json.path("msg").asText());
        }
        return json;
    }

    private JsonNode parseSse(String body) throws Exception {
        JsonNode latest = null;
        for (String line : body.split("\\R")) {
            if (!line.startsWith("data:")) continue;
            String data = line.substring(5).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) continue;
            JsonNode candidate = objectMapper.readTree(data);
            if (candidate.has("result") || candidate.has("error")) latest = candidate;
        }
        if (latest == null) throw new IllegalStateException("MCP server returned no JSON-RPC SSE data");
        return latest;
    }

    @Override
    public void close() {
        // java.net.http.HttpClient owns no closeable resources on Java 17.
    }
}
