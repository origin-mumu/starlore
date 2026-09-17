-- Starlore AI 调用明细表结构
-- 执行前请先备份数据库。本文件仅提供 SQL，不会由应用自动执行。

CREATE TABLE IF NOT EXISTS `ai_usage_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `username` VARCHAR(50) NULL,
  `role` VARCHAR(20) NULL,
  `scene` VARCHAR(30) NOT NULL,
  `model` VARCHAR(100) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'success',
  `duration_ms` INT NOT NULL DEFAULT 0,
  `tokens_prompt` INT NOT NULL DEFAULT 0,
  `tokens_completion` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `ix_ai_usage_logs_user_id` (`user_id`),
  INDEX `idx_ai_usage_user_time` (`user_id`, `created_at`),
  INDEX `ix_ai_usage_logs_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
