package com.robin.blogback.config;

import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.service.AiConfigService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class AgentConfig {

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
}
