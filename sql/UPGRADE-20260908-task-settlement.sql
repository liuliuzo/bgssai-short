
CREATE TABLE IF NOT EXISTS short_debit_outbox (
    outbox_id VARCHAR(128) NOT NULL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    website_user_id VARCHAR(64) NOT NULL,
    amount INT NOT NULL,
    usage_kind VARCHAR(32) NOT NULL,
    delivered TINYINT NOT NULL DEFAULT 0,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_until DATETIME NULL,
    KEY idx_short_debit_pending(delivered, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
