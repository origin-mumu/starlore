package com.robin.blogback.dto;

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
}
