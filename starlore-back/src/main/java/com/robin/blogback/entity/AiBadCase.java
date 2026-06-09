package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Bad Case 实体，用于存储 Agent 执行失败的案例。
 * 支持 Few-Shot 动态反馈机制。
 */
@TableName("ai_bad_cases")
public class AiBadCase {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String question;
    private String expectedAnswer;
    private String actualAnswer;
    private String agentPath;
    private String errorMessage;
    private Integer tokens;
    private Long latencyMs;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }

    public String getActualAnswer() { return actualAnswer; }
    public void setActualAnswer(String actualAnswer) { this.actualAnswer = actualAnswer; }

    public String getAgentPath() { return agentPath; }
    public void setAgentPath(String agentPath) { this.agentPath = agentPath; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getTokens() { return tokens; }
    public void setTokens(Integer tokens) { this.tokens = tokens; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
