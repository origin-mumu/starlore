package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("categories")
public class Category {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("user_id")
    private Integer userId;
    private String name;
    private String description;
    private String color;
    @TableField("article_count")
    private Integer articleCount;
    @TableField("createdAt")
    private LocalDateTime createdAt;
    @TableField("updatedAt")
    private LocalDateTime updatedAt;
}
