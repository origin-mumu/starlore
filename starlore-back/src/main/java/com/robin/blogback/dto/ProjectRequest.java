package com.robin.blogback.dto;

import lombok.Data;

@Data
public class ProjectRequest {
    private String name;
    private String description;
    private String url;
    private String image;
    private Integer sortOrder;
}
