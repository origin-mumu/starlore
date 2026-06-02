-- AI 模型配置表
CREATE TABLE IF NOT EXISTS `ai_configs` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `modelKey` VARCHAR(32) NOT NULL COMMENT '模型标识，如 qwen-plus、mimo',
  `modelName` VARCHAR(64) NOT NULL COMMENT '模型显示名称',
  `apiUrl` VARCHAR(255) NOT NULL COMMENT 'API 请求地址',
  `modelId` VARCHAR(64) NOT NULL COMMENT '实际调用的模型 ID',
  `apiKey` VARCHAR(255) DEFAULT NULL COMMENT 'API 密钥',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_modelKey` (`modelKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型配置表';

-- 插入默认配置（API 密钥留空，请在后台管理中填写）
INSERT INTO `ai_configs` (`modelKey`, `modelName`, `apiUrl`, `modelId`, `apiKey`, `enabled`) VALUES
('qwen-plus', '通义千问 Plus', 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', 'qwen-plus', NULL, 1),
('mimo', '小米 MiMo', 'https://api.xiaomimimo.com/v1/chat/completions', 'mimo-v2-flash', NULL, 1),
('zhipu-embedding', '智谱 Embedding', 'https://open.bigmodel.cn/api/paas/v4', 'embedding-2', '48e26f9891024142ad41b432ab183fd0.7VEK6aZ2zfP5QQDt', 1);
