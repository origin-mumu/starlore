-- 简历表
CREATE TABLE IF NOT EXISTS `resumes` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `title` VARCHAR(200) DEFAULT '',
    `template` VARCHAR(50) DEFAULT 'classic',
    `name` VARCHAR(100) DEFAULT '',
    `job_title` VARCHAR(200) DEFAULT '',
    `phone` VARCHAR(50) DEFAULT '',
    `email` VARCHAR(200) DEFAULT '',
    `photo_url` VARCHAR(500) DEFAULT '',
    `content` LONGTEXT COMMENT 'JSON格式的完整简历内容',
    `status` VARCHAR(20) DEFAULT 'active',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
