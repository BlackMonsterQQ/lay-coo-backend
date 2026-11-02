CREATE TABLE IF NOT EXISTS category
(
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(32) NOT NULL,
    code        VARCHAR(32) NOT NULL,
    type        VARCHAR(16) NOT NULL,
    icon        VARCHAR(16),
    sort        INT DEFAULT 0,
    description VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_category_code ON category (code);

CREATE TABLE IF NOT EXISTS tag
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tag_name ON tag (name);

CREATE TABLE IF NOT EXISTS content
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(128) NOT NULL,
    summary        VARCHAR(512),
    category_id    BIGINT,
    area           VARCHAR(64),
    address        VARCHAR(255),
    body           VARCHAR(8000),
    highlights     VARCHAR(2000),
    tips           VARCHAR(1024),
    opening_hours  VARCHAR(128),
    price_range    VARCHAR(64),
    cover_desc     VARCHAR(255),
    source         VARCHAR(16) DEFAULT 'MANUAL',
    status         VARCHAR(16) DEFAULT 'PENDING',
    audit_remark   VARCHAR(255),
    ai_task_id     BIGINT,
    view_count     BIGINT DEFAULT 0,
    favorite_count BIGINT DEFAULT 0,
    like_count     BIGINT DEFAULT 0,
    comment_count  BIGINT DEFAULT 0,
    rating         DECIMAL(3, 2) DEFAULT 0,
    rating_count   BIGINT DEFAULT 0,
    publish_time   TIMESTAMP,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    deleted        INT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_content_status ON content (status);
CREATE INDEX IF NOT EXISTS idx_content_category ON content (category_id);
CREATE INDEX IF NOT EXISTS idx_content_area ON content (area);

CREATE TABLE IF NOT EXISTS content_tag
(
    content_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (content_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_content_tag_tag ON content_tag (tag_id);
