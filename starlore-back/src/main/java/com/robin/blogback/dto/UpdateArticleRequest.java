package com.robin.blogback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateArticleRequest {
    private String title;
    private String content;
    private String description;
    private String category;
    private List<String> tags;
    private String coverImage;
    private String status;

    @JsonProperty("is_public")
    private Boolean isPublic;
}
