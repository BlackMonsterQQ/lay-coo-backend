CREATE TABLE IF NOT EXISTS user_favorite
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    create_time TIMESTAMP,
    UNIQUE KEY uk_favorite_user_content (user_id, content_id),
    KEY idx_favorite_content (content_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_like
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    create_time TIMESTAMP,
    UNIQUE KEY uk_like_user_content (user_id, content_id),
    KEY idx_like_content (content_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_comment
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    parent_id   BIGINT DEFAULT 0,
    content     VARCHAR(500) NOT NULL,
    status      INT DEFAULT 1,
    create_time TIMESTAMP,
    KEY idx_comment_content (content_id, status),
    KEY idx_comment_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_rate
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    score       INT NOT NULL,
    create_time TIMESTAMP,
    UNIQUE KEY uk_rate_user_content (user_id, content_id),
    KEY idx_rate_content (content_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
