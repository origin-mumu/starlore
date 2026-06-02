package com.robin.blogback.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDetailResponse {
    private CategoryDetailData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryDetailData {
        private Integer id;
        private String name;
        private String description;
        private String color;
        @JsonProperty("article_count")
        private Integer articleCount;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private LocalDateTime createdAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private LocalDateTime updatedAt;
        private List<ArticleSummary> articles;
    }
}
