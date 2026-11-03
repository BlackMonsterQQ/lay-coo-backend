CREATE TABLE IF NOT EXISTS ai_task
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(32),
    area          VARCHAR(64),
    target_count  INT DEFAULT 2,
    keywords      VARCHAR(200),
    status        VARCHAR(16) DEFAULT 'PENDING',
    model         VARCHAR(64),
    prompt        LONGTEXT,
    result        LONGTEXT,
    error         VARCHAR(1024),
    cost_tokens   BIGINT DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    KEY idx_ai_task_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS travel_plan
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_no              VARCHAR(16) NOT NULL,
    user_id              BIGINT,
    city_code            VARCHAR(32) NOT NULL,
    city_name            VARCHAR(32) NOT NULL,
    origin_city_code     VARCHAR(32),
    origin_city_name     VARCHAR(32),
    transport_preference VARCHAR(16) DEFAULT 'AUTO',
    start_date           DATE NOT NULL,
    end_date             DATE NOT NULL,
    day_count            INT NOT NULL,
    preferences          VARCHAR(512),
    extra_note           VARCHAR(256),
    request_hash         VARCHAR(64),
    status               VARCHAR(16) DEFAULT 'PENDING',
    title                VARCHAR(128),
    summary              VARCHAR(1024),
    content_json         LONGTEXT,
    model                VARCHAR(64),
    prompt               LONGTEXT,
    error                VARCHAR(1024),
    progress             INT DEFAULT 0,
    cost_tokens          BIGINT DEFAULT 0,
    create_time          TIMESTAMP,
    update_time          TIMESTAMP,
    UNIQUE KEY uk_travel_plan_no (plan_no),
    KEY idx_travel_plan_user (user_id),
    KEY idx_travel_plan_hash (request_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
