CREATE TABLE IF NOT EXISTS ai_task
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(32),
    area          VARCHAR(64),
    target_count  INT DEFAULT 2,
    keywords      VARCHAR(200),
    status        VARCHAR(16) DEFAULT 'PENDING',
    model         VARCHAR(64),
    prompt        CLOB,
    result        CLOB,
    error         VARCHAR(1024),
    cost_tokens   BIGINT DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_task_status ON ai_task (status);
