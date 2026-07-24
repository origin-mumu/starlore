package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_configs")
public class AgentConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String modelName;
    private Double similarityThreshold;
    private Integer topK;
    private Double temperature;
    private Integer enableRerank;
    private LocalDateTime updatedAt;
}
