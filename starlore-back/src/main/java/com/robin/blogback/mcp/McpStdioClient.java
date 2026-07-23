package com.robin.blogback.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal persistent MCP client for the STDIO transport.
 *
 * MCP STDIO messages are JSON-RPC objects, one UTF-8 JSON object per line.
 * The client keeps the child process and session alive for subsequent tool calls.
 */
public final class McpStdioClient implements McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpStdioClient.class);
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final McpServerConfig config;
    private final ObjectMapper objectMapper;
    private final Duration requestTimeout;
    private final AtomicLong requestIds = new AtomicLong(1);
    private final Map<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor;
    private final Object writeLock = new Object();

    private Process process;
    private BufferedWriter writer;
    private volatile boolean initialized;

    public McpStdioClient(McpServerConfig config, ObjectMapper objectMapper, Duration requestTimeout) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.requestTimeout = requestTimeout;
        this.ioExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "mcp-" + config.name());
            thread.setDaemon(true);
            return thread;
        });
    }

    public String name() {
        return config.name();
    }

    public synchronized void connect() throws Exception {
        if (isConnected()) return;
        if (config.command() == null || config.command().isBlank()) {
            throw new IllegalArgumentException("MCP server command is empty: " + config.name());
        }

        List<String> command = new ArrayList<>();
        command.add(config.command());
        command.addAll(config.args());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(config.env());
        process = builder.start();
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        ioExecutor.submit(this::readStdout);
        ioExecutor.submit(this::readStderr);

        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.set("capabilities", objectMapper.createObjectNode());
        ObjectNode clientInfo = objectMapper.createObjectNode();
        clientInfo.put("name", "starlore-back");
        clientInfo.put("version", "1.0.0");
        params.set("clientInfo", clientInfo);

        JsonNode result = request("initialize", params);
        String negotiatedVersion = result.path("protocolVersion").asText(PROTOCOL_VERSION);
        ObjectNode initializedParams = objectMapper.createObjectNode();
        notification("notifications/initialized", initializedParams);
        initialized = true;
        log.info("[MCP] Connected to server '{}', protocol={}", config.name(), negotiatedVersion);
    }

    public synchronized boolean isConnected() {
        return initialized && process != null && process.isAlive();
    }

    public List<McpToolDescriptor> listTools() throws Exception {
        ensureConnected();
        JsonNode result = request("tools/list", objectMapper.createObjectNode());
        List<McpToolDescriptor> tools = new ArrayList<>();
        for (JsonNode tool : result.path("tools")) {
            tools.add(new McpToolDescriptor(
                    config.name(),
                    tool.path("name").asText(),
                    tool.path("description").asText(""),
                    tool.path("inputSchema")
            ));
        }
        return tools;
    }

    public JsonNode callTool(String toolName, JsonNode arguments) throws Exception {
        ensureConnected();
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", arguments == null || arguments.isNull()
                ? objectMapper.createObjectNode() : arguments);
        return request("tools/call", params);
    }

    private void ensureConnected() throws Exception {
        if (!isConnected()) connect();
    }

    private JsonNode request(String method, JsonNode params) throws Exception {
        long id = requestIds.getAndIncrement();
        ObjectNode message = baseMessage();
        message.put("id", id);
        message.put("method", method);
        message.set("params", params);

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);
        try {
            send(message);
            return future.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("MCP request timed out: server=" + config.name() + ", method=" + method);
        } finally {
            pending.remove(id);
        }
    }

    private void notification(String method, JsonNode params) throws Exception {
        ObjectNode message = baseMessage();
        message.put("method", method);
        message.set("params", params);
        send(message);
    }

    private ObjectNode baseMessage() {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("jsonrpc", "2.0");
        return message;
    }

    private void send(JsonNode message) throws Exception {
        String json = objectMapper.writeValueAsString(message);
        synchronized (writeLock) {
            if (writer == null) throw new IllegalStateException("MCP process is not connected");
            writer.write(json);
            writer.newLine();
            writer.flush();
        }
    }

    private void readStdout() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    JsonNode message = objectMapper.readTree(line);
                    if (!message.has("id")) continue;
                    long id = message.path("id").asLong();
                    CompletableFuture<JsonNode> future = pending.get(id);
                    if (future == null) continue;
                    if (message.has("error")) {
                        future.completeExceptionally(new IllegalStateException(
                                "MCP error from " + config.name() + ": " + message.get("error")));
                    } else {
                        future.complete(message.path("result"));
                    }
                } catch (Exception parseError) {
                    log.warn("[MCP] Ignoring invalid stdout from '{}': {}", config.name(), parseError.getMessage());
                }
            }
        } catch (Exception e) {
            if (process != null && process.isAlive()) {
                log.warn("[MCP] stdout reader stopped for '{}': {}", config.name(), e.getMessage());
            }
        } finally {
            initialized = false;
            IllegalStateException closed = new IllegalStateException("MCP server stopped: " + config.name());
            pending.values().forEach(future -> future.completeExceptionally(closed));
            pending.clear();
        }
    }

    private void readStderr() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) log.debug("[MCP:{}] {}", config.name(), line);
            }
        } catch (Exception ignored) {
            // Process shutdown closes the stream.
        }
    }

    @Override
    public synchronized void close() {
        initialized = false;
        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        ioExecutor.shutdownNow();
    }
}
