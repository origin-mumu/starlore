package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article_chunks")
public class ArticleChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private Integer isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
