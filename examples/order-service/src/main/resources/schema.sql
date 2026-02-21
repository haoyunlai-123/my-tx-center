CREATE TABLE IF NOT EXISTS tx_idempotency (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              idem_key VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_idem_key (idem_key)
    );