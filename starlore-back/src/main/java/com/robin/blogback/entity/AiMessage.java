package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_messages")
public class AiMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
