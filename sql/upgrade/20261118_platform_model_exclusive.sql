-- 同一能力的自建模型可被用户单独关闭。已有库补列，可重复执行。
SET @setting_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_ai_setting' AND COLUMN_NAME = 'suppressed_platform_abilities');
SET @setting_ddl = IF(@setting_col_exists = 0, 'ALTER TABLE user_ai_setting ADD COLUMN suppressed_platform_abilities VARCHAR(64) DEFAULT NULL COMMENT ''用户关闭的自建模型能力，逗号分隔：TEXT/IMAGE/VIDEO/VOICE/ASR/MUSIC''', 'SELECT 1');
PREPARE setting_stmt FROM @setting_ddl; EXECUTE setting_stmt; DEALLOCATE PREPARE setting_stmt;
