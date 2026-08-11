-- Starlore 知识记忆表结构
-- 执行前请先备份数据库。本文件仅提供 SQL，不会由应用自动执行。

CREATE TABLE IF NOT EXISTS `knowledge_cards` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `article_id` INT NOT NULL,
  `question` VARCHAR(500) NOT NULL,
  `answer_markdown` LONGTEXT NOT NULL,
  `source_line_start` INT NOT NULL,
  `source_line_end` INT NOT NULL,
  `source_hash` CHAR(64) NOT NULL,
  `difficulty` VARCHAR(20) NOT NULL DEFAULT 'core',
  `sort_order` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `review_stage` VARCHAR(20) NOT NULL DEFAULT 'new',
  `interval_days` INT NOT NULL DEFAULT 0,
  `review_count` INT NOT NULL DEFAULT 0,
  `lapse_count` INT NOT NULL DEFAULT 0,
  `next_review_at` DATETIME NULL,
  `last_reviewed_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_cards_user_article` (`user_id`, `article_id`),
  KEY `idx_knowledge_cards_due` (`user_id`, `status`, `next_review_at`),
  CONSTRAINT `fk_knowledge_cards_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_knowledge_cards_article`
    FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `knowledge_reviews` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `card_id` INT NOT NULL,
  `rating` VARCHAR(20) NOT NULL,
  `previous_interval_days` INT NOT NULL DEFAULT 0,
  `next_interval_days` INT NOT NULL DEFAULT 0,
  `duration_ms` INT NULL,
  `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_reviews_user_time` (`user_id`, `reviewed_at`),
  KEY `idx_knowledge_reviews_card` (`card_id`),
  CONSTRAINT `fk_knowledge_reviews_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_knowledge_reviews_card`
    FOREIGN KEY (`card_id`) REFERENCES `knowledge_cards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
