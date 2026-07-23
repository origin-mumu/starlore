package com.robin.blogback.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Closeable;
import java.util.List;

interface McpClient extends Closeable {
    String name();

    List<McpToolDescriptor> listTools() throws Exception;

    JsonNode callTool(String toolName, JsonNode arguments) throws Exception;
}
