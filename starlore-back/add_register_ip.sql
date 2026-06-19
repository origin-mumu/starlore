-- 增加用户注册 IP 及其归属地字段
ALTER TABLE users 
    ADD COLUMN register_ip VARCHAR(50) DEFAULT NULL COMMENT '注册IP',
    ADD COLUMN register_country VARCHAR(50) DEFAULT NULL COMMENT '注册国家',
    ADD COLUMN register_province VARCHAR(50) DEFAULT NULL COMMENT '注册省份',
    ADD COLUMN register_city VARCHAR(50) DEFAULT NULL COMMENT '注册城市';
