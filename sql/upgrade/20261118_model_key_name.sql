-- 模型密钥名称，既有配置保持未命名。
SET @key_name_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_ai_model_key' AND COLUMN_NAME = 'key_name');
SET @key_name_ddl = IF(@key_name_exists = 0,
  'ALTER TABLE user_ai_model_key ADD COLUMN key_name VARCHAR(64) DEFAULT NULL COMMENT ''用户自定义密钥名称''',
  'SELECT 1');
PREPARE key_name_stmt FROM @key_name_ddl;
EXECUTE key_name_stmt;
DEALLOCATE PREPARE key_name_stmt;
