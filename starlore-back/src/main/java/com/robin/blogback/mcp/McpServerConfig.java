package com.robin.blogback.mcp;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record McpServerConfig(
        String name,
        String command,
        List<String> args,
        Map<String, String> env
) {
    public McpServerConfig {
        args = args == null ? Collections.emptyList() : List.copyOf(args);
        env = env == null ? Collections.emptyMap() : Map.copyOf(env);
    }
}
