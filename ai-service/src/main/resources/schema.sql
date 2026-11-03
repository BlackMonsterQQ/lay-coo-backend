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

CREATE TABLE IF NOT EXISTS travel_plan
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_no       VARCHAR(16) NOT NULL,
    user_id       BIGINT,
    city_code     VARCHAR(32) NOT NULL,
    city_name     VARCHAR(32) NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE NOT NULL,
    day_count     INT NOT NULL,
    preferences   VARCHAR(512),
    extra_note    VARCHAR(256),
    request_hash  VARCHAR(64),
    status        VARCHAR(16) DEFAULT 'PENDING',
    title         VARCHAR(128),
    summary       VARCHAR(1024),
    content_json  CLOB,
    model         VARCHAR(64),
    prompt        CLOB,
    error         VARCHAR(1024),
    progress      INT DEFAULT 0,
    cost_tokens   BIGINT DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_travel_plan_no ON travel_plan (plan_no);
CREATE INDEX IF NOT EXISTS idx_travel_plan_user ON travel_plan (user_id);
CREATE INDEX IF NOT EXISTS idx_travel_plan_hash ON travel_plan (request_hash);
