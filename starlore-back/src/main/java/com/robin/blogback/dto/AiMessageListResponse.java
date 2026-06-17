package com.robin.blogback.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiMessageListResponse {
    private boolean success;
    private SessionInfo session;
    private List<MessageItem> messages;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionInfo {
        private Integer id;
        private String title;
        private String characterKey;
        private String modelId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageItem {
        private Integer id;
        private String role;
        private String content;
        private String agentTrace;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private LocalDateTime createdAt;
    }
}
