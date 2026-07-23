package com.robin.blogback.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpStdioClientTest {

    @Test
    void keepsSessionAliveForDiscoveryAndToolCall() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        McpServerConfig config = new McpServerConfig(
                "fake",
                java,
                List.of("-cp", System.getProperty("java.class.path"), FakeServer.class.getName()),
                Map.of()
        );

        try (McpStdioClient client = new McpStdioClient(config, new ObjectMapper(), Duration.ofSeconds(5))) {
            List<McpToolDescriptor> tools = client.listTools();
            assertEquals(1, tools.size());
            assertEquals("echo", tools.get(0).name());

            JsonNode args = new ObjectMapper().readTree("{\"text\":\"hello\"}");
            JsonNode result = client.callTool("echo", args);
            assertTrue(result.path("content").get(0).path("text").asText().contains("hello"));
            assertTrue(client.isConnected());
        }
    }

    @Test
    void officialFilesystemServerSmokeTest() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("RUN_MCP_FILESYSTEM_SMOKE")));
        Path allowedDirectory = Files.createTempDirectory("starlore-mcp-smoke-");
        Files.writeString(allowedDirectory.resolve("hello.txt"), "starlore");
        McpServerConfig config = new McpServerConfig(
                "filesystem",
                "cmd",
                List.of("/c", "npx", "-y", "@modelcontextprotocol/server-filesystem",
                        allowedDirectory.toString()),
                Map.of()
        );

        try (McpStdioClient client = new McpStdioClient(config, new ObjectMapper(), Duration.ofSeconds(60))) {
            List<McpToolDescriptor> tools = client.listTools();
            assertTrue(tools.stream().anyMatch(tool -> "read_text_file".equals(tool.name())));

            JsonNode args = new ObjectMapper().createObjectNode()
                    .put("path", allowedDirectory.resolve("hello.txt").toString());
            JsonNode result = client.callTool("read_text_file", args);
            assertTrue(result.toString().contains("starlore"));
        }
    }

    public static final class FakeServer {
        public static void main(String[] ignored) throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    JsonNode request = mapper.readTree(line);
                    if (!request.has("id")) continue;
                    long id = request.path("id").asLong();
                    String method = request.path("method").asText();
                    String result;
                    if ("initialize".equals(method)) {
                        result = "{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{\"tools\":{}},"
                                + "\"serverInfo\":{\"name\":\"fake\",\"version\":\"1\"}}";
                    } else if ("tools/list".equals(method)) {
                        result = "{\"tools\":[{\"name\":\"echo\",\"description\":\"Echo text\","
                                + "\"inputSchema\":{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\"}}}}]}";
                    } else if ("tools/call".equals(method)) {
                        String text = request.path("params").path("arguments").path("text").asText();
                        result = "{\"content\":[{\"type\":\"text\",\"text\":"
                                + mapper.writeValueAsString(text) + "}]}";
                    } else {
                        result = "{}";
                    }
                    System.out.println("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":" + result + "}");
                    System.out.flush();
                }
            }
        }
    }
}
