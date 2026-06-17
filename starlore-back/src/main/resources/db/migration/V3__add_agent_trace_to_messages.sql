-- 给 ai_messages 表添加 agent_trace 字段，用于存储多 Agent 协作的追踪信息
ALTER TABLE `ai_messages` ADD COLUMN `agent_trace` TEXT COMMENT 'Agent 追踪信息 JSON（planSummary, subtasks, reviewDecision 等）';
