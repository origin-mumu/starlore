package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_message_feedbacks")
public class AiMessageFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long sessionId;
    private Long userId;
    private String rating;
    private String feedbackType;
    private String comment;
    private LocalDateTime createdAt;
}
