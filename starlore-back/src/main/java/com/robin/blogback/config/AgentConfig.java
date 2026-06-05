package com.robin.blogback.config;

import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.service.AiConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.File;

@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    @Autowired
    private AiConfigService aiConfigService;

    @Value("${spring.ai.openai.api-key:}")
    private String localApiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String localBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String localModel;

    @Bean
    public ChatModel chatModel() {
        // 优先从数据库读取配置
        AiConfig dbConfig = aiConfigService.getConfigByKey("deepseek-v4-flash");

        String apiKey;
        String baseUrl;
        String model;

        if (dbConfig != null && dbConfig.getEnabled() && StringUtils.hasText(dbConfig.getApiKey())) {
            apiKey = dbConfig.getApiKey();
            baseUrl = dbConfig.getApiUrl().replaceAll("/chat/completions$", "");
            model = dbConfig.getModelId();
        } else {
            // 回退到本地配置
            apiKey = localApiKey;
            baseUrl = localBaseUrl;
            model = localModel;
        }

        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .build();
        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        AiConfig dbConfig = aiConfigService.getConfigByKey("zhipu-embedding");
        if (dbConfig != null && dbConfig.getEnabled() && StringUtils.hasText(dbConfig.getApiKey())) {
            String baseUrl = dbConfig.getApiUrl().replaceAll("/embeddings$", "");
            log.info("[RAG] 使用智谱 Embedding: {}, model: {}", baseUrl, dbConfig.getModelId());
            return new ZhipuEmbeddingModel(baseUrl, dbConfig.getApiKey(), dbConfig.getModelId());
        }
        log.warn("[RAG] 未找到可用的 Embedding 配置，语义搜索已禁用");
        return null;
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        if (embeddingModel == null) {
            return null;
        }
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File storeFile = new File("./data/vector-store.json");
        if (storeFile.exists() && storeFile.length() > 0) {
            try {
                store.load(storeFile);
                log.info("[RAG] 已加载向量存储: {}", storeFile.getAbsolutePath());
            } catch (Exception e) {
                log.warn("[RAG] 加载向量存储失败，将重新创建: {}", e.getMessage());
            }
        } else {
            storeFile.getParentFile().mkdirs();
            log.info("[RAG] 创建新的向量存储: {}", storeFile.getAbsolutePath());
        }
        return store;
    }
}
