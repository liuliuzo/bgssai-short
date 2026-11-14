-- Additive login-log context for databases that already exist.
-- Safe to re-run. New databases take the same columns from sql/DDL.sql.
-- Does not rewrite historical rows. Unknown facts stay NULL.

-- login_result
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'login_result');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN login_result VARCHAR(16) NULL COMMENT ''SUCCESS / FAILURE''', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- result_reason
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'result_reason');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN result_reason VARCHAR(128) NULL COMMENT ''人类可读原因，不含口令、验证码或令牌''', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- login_method
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'login_method');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN login_method VARCHAR(32) NULL COMMENT ''password/email_otp/phone_otp/wechat/alipay/douyin/baidu/bgssai/oauth''', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- identifier_type
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'identifier_type');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN identifier_type VARCHAR(16) NULL COMMENT ''username/email/phone/oauth''', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- identifier_masked
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'identifier_masked');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN identifier_masked VARCHAR(128) NULL COMMENT ''脱敏后的登录标识''', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- client_channel
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'client_channel');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN client_channel VARCHAR(16) NULL COMMENT ''web/admin/app''', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- browser_name
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'browser_name');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN browser_name VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- os_name
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'os_name');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN os_name VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- device_type
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'device_type');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN device_type VARCHAR(16) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- device_label
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'device_label');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN device_label VARCHAR(160) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- ip_country
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'ip_country');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN ip_country VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- ip_province
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'ip_province');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN ip_province VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- ip_city
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'ip_city');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN ip_city VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- ip_isp
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'ip_isp');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN ip_isp VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- request_scheme
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'request_scheme');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN request_scheme VARCHAR(8) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- tls_protocol
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'tls_protocol');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN tls_protocol VARCHAR(32) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- referer_host
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'referer_host');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN referer_host VARCHAR(255) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- entry_path
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'entry_path');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN entry_path VARCHAR(255) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- request_id
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'request_id');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN request_id VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- session_id
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'session_id');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN session_id VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- session_issued_at
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'session_issued_at');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN session_issued_at DATETIME NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- session_expires_at
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'session_expires_at');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN session_expires_at DATETIME NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- session_last_seen_at
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'session_last_seen_at');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN session_last_seen_at DATETIME NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- logout_time
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'logout_time');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN logout_time DATETIME NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- accept_language
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'accept_language');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN accept_language VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- client_timezone
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'client_timezone');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN client_timezone VARCHAR(64) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- client_hints
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'client_hints');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN client_hints VARCHAR(160) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- risk_new_device
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'risk_new_device');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN risk_new_device TINYINT(1) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
-- risk_new_city
SET @login_log_col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'risk_new_city');
SET @login_log_ddl = IF(@login_log_col_exists = 0, 'ALTER TABLE login_log ADD COLUMN risk_new_city TINYINT(1) NULL', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;

SET @login_log_idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND INDEX_NAME = 'idx_login_log_session');
SET @login_log_ddl = IF(@login_log_idx_exists = 0, 'ALTER TABLE login_log ADD INDEX idx_login_log_session (session_id)', 'SELECT 1');
PREPARE login_log_stmt FROM @login_log_ddl; EXECUTE login_log_stmt; DEALLOCATE PREPARE login_log_stmt;
