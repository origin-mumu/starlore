package com.robin.blogback.mcp;

import com.fasterxml.jackson.databind.JsonNode;

public record McpToolDescriptor(
        String server,
        String name,
        String description,
        JsonNode inputSchema
) {
}
