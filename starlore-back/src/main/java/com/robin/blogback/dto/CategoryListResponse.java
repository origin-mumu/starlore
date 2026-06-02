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
public class CategoryListResponse {
    private List<CategoryItem> data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryItem {
        private Integer id;
        private Integer userId;
        private String name;
        private String description;
        private String color;
        @JsonProperty("article_count")
        private Integer articleCount;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private LocalDateTime createdAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private LocalDateTime updatedAt;
    }
}
