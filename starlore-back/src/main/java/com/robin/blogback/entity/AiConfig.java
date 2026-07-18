package com.robin.blogback.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_configs")
public class AiConfig {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String modelKey;
    private String modelName;
    private String apiUrl;
    private String modelId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
