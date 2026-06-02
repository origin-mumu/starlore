package com.robin.blogback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleListResponse {
    private List<ArticleSummary> data;
    private PaginationInfo pagination;
}
