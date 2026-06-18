package com.robin.blogback.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleDetail {
    private Integer id;
    private String title;
    private String content;
    private String description;
    private String category;
    private List<String> tags;
    @JsonProperty("cover_image")
    private String coverImage;
    @JsonProperty("view_count")
    private Integer viewCount;
    private String status;
    @JsonProperty("is_public")
    private Boolean isPublic;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;
}
