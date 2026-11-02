CREATE TABLE IF NOT EXISTS recommend_log
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    content_id  BIGINT NOT NULL,
    scene       VARCHAR(32),
    strategy    VARCHAR(64),
    score       DOUBLE,
    reason      VARCHAR(255),
    create_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recommend_log_user ON recommend_log (user_id, create_time);
