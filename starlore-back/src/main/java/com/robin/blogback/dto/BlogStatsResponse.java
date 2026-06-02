package com.robin.blogback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogStatsResponse {
    private BlogStatsData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BlogStatsData {
        @JsonProperty("totalArticles")
        private long totalArticles;
        @JsonProperty("totalCategories")
        private long totalCategories;
        @JsonProperty("totalViews")
        private long totalViews;
        @JsonProperty("popularArticles")
        private List<ArticleSummary> popularArticles;
        @JsonProperty("popularCategories")
        private List<CategoryInfo> popularCategories;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryInfo {
        private Integer id;
        private String name;
        @JsonProperty("article_count")
        private Integer articleCount;
    }
}
