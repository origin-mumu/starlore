-- 为 articles、categories、ai_sessions 表添加 user_id 列
-- 执行前请先备份数据库

-- 1. articles 表添加 user_id
ALTER TABLE articles ADD COLUMN user_id INT NULL COMMENT '作者用户ID';
ALTER TABLE articles ADD INDEX idx_user_id (user_id);

-- 2. categories 表添加 user_id
ALTER TABLE categories ADD COLUMN user_id INT NULL COMMENT '创建者用户ID';
ALTER TABLE categories ADD INDEX idx_user_id (user_id);

-- 3. ai_sessions 表添加 user_id（替代原来的 client_id）
ALTER TABLE ai_sessions ADD COLUMN user_id INT NULL COMMENT '所属用户ID';
ALTER TABLE ai_sessions ADD INDEX idx_user_id (user_id);

-- 可选：如果要删除 ai_sessions 的 client_id 列（确认不再需要后执行）
-- ALTER TABLE ai_sessions DROP COLUMN client_id;

-- 可选：如果要将已有数据关联到某个用户（比如第一个注册的用户）
-- 假设 user_id = 1 是管理员
-- UPDATE articles SET user_id = 1 WHERE user_id IS NULL;
-- UPDATE categories SET user_id = 1 WHERE user_id IS NULL;
-- UPDATE ai_sessions SET user_id = 1 WHERE user_id IS NULL;
