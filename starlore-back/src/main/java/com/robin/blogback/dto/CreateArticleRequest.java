package com.robin.blogback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateArticleRequest {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;

    private String content;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    private String category;
    private List<String> tags;
    private String coverImage;
    private String status;
}
