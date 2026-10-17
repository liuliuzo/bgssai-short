-- Additive upgrade for account deletion (REQ-SHORT-COMMON-016). Safe to re-run.
SET @deleted_at_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'deleted_at');
SET @deleted_at_ddl = IF(@deleted_at_exists = 0,
  'ALTER TABLE sys_user ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL COMMENT ''账号注销时间；非空表示已自助注销'' AFTER current_session_id',
  'SELECT 1');
PREPARE stmt FROM @deleted_at_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `account_deletion_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` varchar(64) NOT NULL COMMENT '被注销用户',
  `verify_method` varchar(32) NOT NULL COMMENT 'password / sms / email',
  `reason` varchar(256) DEFAULT NULL COMMENT '用户填写原因',
  `anonymized` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否已匿名化',
  `del_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modifier` varchar(64) DEFAULT NULL COMMENT '修改者',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_adl_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号注销审计';
