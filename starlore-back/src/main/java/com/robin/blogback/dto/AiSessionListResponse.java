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
public class AiSessionListResponse {
    private boolean success;
    private List<SessionItem> sessions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionItem {
        private Integer id;
        private String title;
        private String characterKey;
        private String modelId;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private LocalDateTime createdAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private LocalDateTime updatedAt;
    }
}
