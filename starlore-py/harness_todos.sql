-- Starlore Harness 任务清单表结构（会话级整表快照，一个会话一行）
-- 执行前请先备份数据库。本文件仅提供 SQL，不会由应用自动执行。

CREATE TABLE IF NOT EXISTS `harness_todos` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `user_id` INT NOT NULL DEFAULT 1,
  `todos` JSON NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_harness_todos_session_id` (`session_id`),
  INDEX `ix_harness_todos_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
