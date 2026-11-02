CREATE TABLE IF NOT EXISTS user_favorite
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    create_time TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_favorite_user_content ON user_favorite (user_id, content_id);
CREATE INDEX IF NOT EXISTS idx_favorite_content ON user_favorite (content_id);

CREATE TABLE IF NOT EXISTS user_like
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    create_time TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_like_user_content ON user_like (user_id, content_id);
CREATE INDEX IF NOT EXISTS idx_like_content ON user_like (content_id);

CREATE TABLE IF NOT EXISTS user_comment
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    parent_id   BIGINT DEFAULT 0,
    content     VARCHAR(500) NOT NULL,
    status      INT DEFAULT 1,
    create_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comment_content ON user_comment (content_id, status);
CREATE INDEX IF NOT EXISTS idx_comment_user ON user_comment (user_id);

CREATE TABLE IF NOT EXISTS user_rate
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    score       INT NOT NULL,
    create_time TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_rate_user_content ON user_rate (user_id, content_id);
CREATE INDEX IF NOT EXISTS idx_rate_content ON user_rate (content_id);
