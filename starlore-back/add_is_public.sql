-- 添加是否公开的字段，用于控制游客访问权限
-- 0 表示私密，1 表示公开（默认值为 0 私密）
ALTER TABLE articles ADD COLUMN is_public TINYINT(1) DEFAULT 0;
