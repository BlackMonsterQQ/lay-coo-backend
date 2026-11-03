CREATE TABLE IF NOT EXISTS category
(
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(32) NOT NULL,
    code        VARCHAR(32) NOT NULL,
    type        VARCHAR(16) NOT NULL,
    icon        VARCHAR(16),
    sort        INT DEFAULT 0,
    description VARCHAR(255),
    UNIQUE KEY uk_category_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tag
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    UNIQUE KEY uk_tag_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS content
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(128) NOT NULL,
    summary        VARCHAR(512),
    category_id    BIGINT,
    city_code      VARCHAR(32) DEFAULT 'hangzhou',
    area           VARCHAR(64),
    address        VARCHAR(255),
    body           TEXT,
    highlights     TEXT,
    tips           TEXT,
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
    deleted        INT DEFAULT 0,
    KEY idx_content_status (status),
    KEY idx_content_category (category_id),
    KEY idx_content_area (area),
    KEY idx_content_city (city_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS city
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(32) NOT NULL,
    name        VARCHAR(32) NOT NULL,
    province    VARCHAR(64),
    country     VARCHAR(32),
    longitude   DECIMAL(10, 6) NOT NULL,
    latitude    DECIMAL(10, 6) NOT NULL,
    has_content INT DEFAULT 0,
    enabled     INT DEFAULT 1,
    sort        INT DEFAULT 0,
    UNIQUE KEY uk_city_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS content_tag
(
    content_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (content_id, tag_id),
    KEY idx_content_tag_tag (tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
