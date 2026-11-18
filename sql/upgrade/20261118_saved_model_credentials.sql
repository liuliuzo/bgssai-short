CREATE TABLE IF NOT EXISTS user_ai_saved_credential (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  credential_id VARCHAR(64) NOT NULL UNIQUE,
  user_id VARCHAR(64) NOT NULL,
  provider_code VARCHAR(32) NOT NULL,
  key_name VARCHAR(64) DEFAULT NULL,
  base_url VARCHAR(255) NOT NULL,
  api_key TEXT NOT NULL,
  secret_key TEXT DEFAULT NULL,
  is_active TINYINT DEFAULT NULL,
  last_check_status VARCHAR(16) DEFAULT NULL,
  last_check_time DATETIME DEFAULT NULL,
  del_flag TINYINT NOT NULL DEFAULT 0,
  creator VARCHAR(64) DEFAULT NULL,
  modifier VARCHAR(64) DEFAULT NULL,
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_saved_credential_active (user_id, provider_code, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @credential_id_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_ai_model_key' AND COLUMN_NAME = 'credential_id');
SET @credential_id_ddl = IF(@credential_id_exists = 0,
  'ALTER TABLE user_ai_model_key ADD COLUMN credential_id VARCHAR(64) DEFAULT NULL COMMENT ''关联的供应商凭据''',
  'SELECT 1');
PREPARE credential_id_stmt FROM @credential_id_ddl;
EXECUTE credential_id_stmt;
DEALLOCATE PREPARE credential_id_stmt;
