CREATE TABLE IF NOT EXISTS tx_global (
                                         id              BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         tx_id           VARCHAR(64)  NOT NULL UNIQUE,
    biz_type        VARCHAR(64)  NOT NULL,
    biz_key         VARCHAR(128) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    current_step    INT          NOT NULL DEFAULT 0,
    error_code      VARCHAR(64)  NULL,
    error_msg       VARCHAR(1024) NULL,

    next_run_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    version         BIGINT       NOT NULL DEFAULT 0
    );

CREATE INDEX idx_tx_global_status_next_run ON tx_global(status, next_run_at);
CREATE INDEX idx_tx_global_biz ON tx_global(biz_type, biz_key);




CREATE TABLE IF NOT EXISTS tx_step (
                                       id                BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       tx_id             VARCHAR(64) NOT NULL,
    step_index        INT         NOT NULL,
    step_name         VARCHAR(64) NOT NULL,

    action_method     VARCHAR(16) NOT NULL DEFAULT 'POST',
    action_url        VARCHAR(256) NOT NULL,
    action_body       TEXT        NULL,

    compensate_method VARCHAR(16) NOT NULL DEFAULT 'POST',
    compensate_url    VARCHAR(256) NOT NULL,
    compensate_body   TEXT        NULL,

    status            VARCHAR(32) NOT NULL,
    retry_count       INT         NOT NULL DEFAULT 0,
    retry_max         INT         NOT NULL DEFAULT 3,
    timeout_ms        INT         NOT NULL DEFAULT 3000,

    last_error        VARCHAR(1024) NULL,
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_tx_step (tx_id, step_index),
    INDEX idx_tx_step_tx (tx_id)
    );






CREATE TABLE IF NOT EXISTS tx_log (
                                      id          BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      tx_id       VARCHAR(64) NOT NULL,
    step_index  INT         NOT NULL,
    phase       VARCHAR(16) NOT NULL,   -- ACTION / COMPENSATE
    success     TINYINT     NOT NULL,
    cost_ms     INT         NOT NULL,
    message     VARCHAR(1024) NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_tx_log_tx (tx_id, step_index)
    );