package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_document_chunks")
public class KnowledgeDocumentChunk {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("document_id")
    private Long documentId;

    @TableField("chunk_index")
    private Integer chunkIndex;

    private String content;

    @TableField("token_count")
    private Integer tokenCount;

    @TableField("is_enabled")
    private Integer isEnabled;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
