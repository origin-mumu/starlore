-- Multi-Agent 可观测性表
-- 用于 LangSmith Tracing、Bad Case 收集和 Few-Shot 动态反馈

-- Bad Case 表：存储 Agent 执行失败的案例
CREATE TABLE IF NOT EXISTS `ai_bad_cases` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT DEFAULT NULL COMMENT '用户ID',
    `question` TEXT NOT NULL COMMENT '用户原始问题',
    `expected_answer` TEXT COMMENT '期望的正确答案（可人工标注）',
    `actual_answer` TEXT COMMENT 'Agent 实际输出的答案',
    `agent_path` VARCHAR(255) COMMENT 'Agent 执行路径，如 Planner->Executor->Reviewer',
    `error_message` TEXT COMMENT '错误信息或 Reviewer 反馈',
    `tokens` INT DEFAULT 0 COMMENT '消耗的总 Token 数',
    `latency_ms` BIGINT DEFAULT 0 COMMENT '总耗时（毫秒）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent Bad Case 案例库，用于 Few-Shot 动态反馈';

-- Agent 追踪日志表：记录每次 LLM 调用的详细信息
CREATE TABLE IF NOT EXISTS `agent_trace_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT DEFAULT NULL COMMENT '用户ID',
    `trace_id` VARCHAR(64) NOT NULL COMMENT '追踪ID（同一次 Graph 执行共享）',
    `node_id` VARCHAR(64) NOT NULL COMMENT '节点标识，如 planner/executor/reviewer',
    `node_name` VARCHAR(128) COMMENT '节点显示名称',
    `input_text` TEXT COMMENT '节点输入文本',
    `output_text` TEXT COMMENT '节点输出文本',
    `tokens_in` INT DEFAULT 0 COMMENT '输入 Token 数',
    `tokens_out` INT DEFAULT 0 COMMENT '输出 Token 数',
    `latency_ms` BIGINT DEFAULT 0 COMMENT '节点执行耗时（毫秒）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_trace_id` (`trace_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 执行追踪日志，用于 LangSmith 可观测性';
