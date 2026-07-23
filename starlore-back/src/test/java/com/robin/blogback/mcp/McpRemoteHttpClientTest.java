package com.robin.blogback.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpRemoteHttpClientTest {

    @Test
    void initializesListsAndCallsToolsOverStreamableHttp() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/mcp", exchange -> {
            JsonNode request = mapper.readTree(exchange.getRequestBody());
            String method = request.path("method").asText();
            String response;
            if ("initialize".equals(method)) {
                response = rpc(request, """
                        {"protocolVersion":"2025-03-26","capabilities":{"tools":{}},"serverInfo":{"name":"fake","version":"1"}}
                        """);
                exchange.getResponseHeaders().add("Mcp-Session-Id", "test-session");
            } else if ("notifications/initialized".equals(method)) {
                exchange.sendResponseHeaders(202, -1);
                exchange.close();
                return;
            } else if ("tools/list".equals(method)) {
                response = rpc(request, """
                        {"tools":[{"name":"webSearch","description":"search","inputSchema":{"type":"object"}}]}
                        """);
            } else {
                response = rpc(request, """
                        {"content":[{"type":"text","text":"ok"}],"isError":false}
                        """);
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try (McpRemoteHttpClient client = new McpRemoteHttpClient(
                "remote", java.net.URI.create("http://localhost:" + server.getAddress().getPort() + "/mcp"),
                mapper, Duration.ofSeconds(5))) {
            List<McpToolDescriptor> tools = client.listTools();
            assertEquals("webSearch", tools.get(0).name());
            assertEquals("ok", client.callTool("webSearch", mapper.createObjectNode())
                    .path("content").get(0).path("text").asText());
        } finally {
            server.stop(0);
        }
    }

    private static String rpc(JsonNode request, String result) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + request.path("id") + ",\"result\":" + result + "}";
    }
}
