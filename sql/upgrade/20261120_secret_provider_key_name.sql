-- Add key_name and platform_key_name to sys_secret and user_ai_provider_key (idempotent)
-- Also backfills key_name / platform_key_name safely without overwriting existing non-empty values.

SET @db = DATABASE();

SET @sql_sys_key_name = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_secret' AND COLUMN_NAME = 'key_name'
    ),
    'SELECT 1',
    'ALTER TABLE `sys_secret` ADD COLUMN `key_name` VARCHAR(64) DEFAULT NULL COMMENT ''密钥名称(自定义标签)'' AFTER `secret_value`'
  )
);
PREPARE stmt_sys_key_name FROM @sql_sys_key_name;
EXECUTE stmt_sys_key_name;
DEALLOCATE PREPARE stmt_sys_key_name;

SET @sql_sys_platform_key_name = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_secret' AND COLUMN_NAME = 'platform_key_name'
    ),
    'SELECT 1',
    'ALTER TABLE `sys_secret` ADD COLUMN `platform_key_name` VARCHAR(128) DEFAULT NULL COMMENT ''平台侧密钥名称(自动探测)'' AFTER `key_name`'
  )
);
PREPARE stmt_sys_platform_key_name FROM @sql_sys_platform_key_name;
EXECUTE stmt_sys_platform_key_name;
DEALLOCATE PREPARE stmt_sys_platform_key_name;

SET @sql_user_key_name = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user_ai_provider_key' AND COLUMN_NAME = 'key_name'
    ),
    'SELECT 1',
    'ALTER TABLE `user_ai_provider_key` ADD COLUMN `key_name` VARCHAR(64) DEFAULT NULL COMMENT ''密钥名称(用户自定义标签)'' AFTER `base_url`'
  )
);
PREPARE stmt_user_key_name FROM @sql_user_key_name;
EXECUTE stmt_user_key_name;
DEALLOCATE PREPARE stmt_user_key_name;

SET @sql_user_platform_key_name = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user_ai_provider_key' AND COLUMN_NAME = 'platform_key_name'
    ),
    'SELECT 1',
    'ALTER TABLE `user_ai_provider_key` ADD COLUMN `platform_key_name` VARCHAR(128) DEFAULT NULL COMMENT ''平台侧密钥名称(自动探测)'' AFTER `key_name`'
  )
);
PREPARE stmt_user_platform_key_name FROM @sql_user_platform_key_name;
EXECUTE stmt_user_platform_key_name;
DEALLOCATE PREPARE stmt_user_platform_key_name;
