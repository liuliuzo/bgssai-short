-- Durable provider receipts; independent from commercial product balances.
CREATE TABLE IF NOT EXISTS tokenhub_usage_receipt (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
 user_id VARCHAR(64) NOT NULL,
 work_ref VARCHAR(128) DEFAULT NULL,
 task_ref VARCHAR(255) DEFAULT NULL,
 hub VARCHAR(255) NOT NULL,
 endpoint VARCHAR(255) NOT NULL,
 key_fingerprint VARCHAR(64) NOT NULL,
 model VARCHAR(255) NOT NULL,
 request_id VARCHAR(64) NOT NULL,
 receipt_json MEDIUMTEXT DEFAULT NULL,
 reconciled TINYINT(1) NOT NULL DEFAULT 0,
 next_poll_at BIGINT NOT NULL DEFAULT 0,
 del_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除字段',
 creator VARCHAR(64) DEFAULT NULL COMMENT '创建者标识',
 modifier VARCHAR(64) DEFAULT NULL COMMENT '最后修改者标识',
 gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
 PRIMARY KEY (id),
 UNIQUE KEY uk_receipt_request (hub, request_id),
 KEY idx_receipt_owner (user_id, id),
 KEY idx_receipt_pending (reconciled, next_poll_at)
);
