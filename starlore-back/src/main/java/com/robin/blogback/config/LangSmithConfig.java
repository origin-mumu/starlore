package com.robin.blogback.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * LangSmith 可观测性配置。
 * <p>
 * 通过环境变量启用 LangChain/LangSmith 自动追踪。
 * 当配置了 API Key 时，LangSmith SDK 会自动拦截 LLM 调用并上报 trace。
 */
@Configuration
public class LangSmithConfig {

    private static final Logger log = LoggerFactory.getLogger(LangSmithConfig.class);

    @Value("${langsmith.tracing.enabled:true}")
    private boolean tracingEnabled;

    @Value("${langsmith.api-key:}")
    private String apiKey;

    @Value("${langsmith.project:starlore-java}")
    private String project;

    @Value("${langsmith.endpoint:https://api.smith.langchain.com}")
    private String endpoint;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank() && tracingEnabled) {
            // 设置环境变量，LangSmith SDK 会自动读取
            System.setProperty("LANGCHAIN_TRACING_V2", "true");
            System.setProperty("LANGCHAIN_API_KEY", apiKey);
            System.setProperty("LANGCHAIN_PROJECT", project);
            System.setProperty("LANGCHAIN_ENDPOINT", endpoint);

            log.info("[LangSmith] Tracing enabled via env vars - project: {}, endpoint: {}", project, endpoint);
        } else {
            log.info("[LangSmith] Tracing disabled (no API key or tracing.enabled=false)");
        }
    }
}
