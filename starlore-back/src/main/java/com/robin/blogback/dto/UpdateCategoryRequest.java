package com.robin.blogback.dto;

import lombok.Data;

@Data
public class UpdateCategoryRequest {
    private String name;
    private String description;
    private String color;
}
