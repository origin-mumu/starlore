package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "articles", autoResultMap = true)
public class Article {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("user_id")
    private Integer userId;
    private String title;
    private String content;
    private String description;
    private String category;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
    @TableField("cover_image")
    private String coverImage;
    @TableField("view_count")
    private Integer viewCount;
    private String status;
    @TableField("is_public")
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
