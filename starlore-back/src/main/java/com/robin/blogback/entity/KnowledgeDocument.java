package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_documents")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_url")
    private String fileUrl;

    @TableField("extracted_text")
    private String extractedText;

    private String status;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
