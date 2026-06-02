package com.robin.blogback.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectInfo {
    private Integer id;
    private String name;
    private String description;
    private String url;
    private String image;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
