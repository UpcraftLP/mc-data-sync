CREATE TABLE minecraft_users (
    user_id BIGINT NOT NULL PRIMARY KEY,
    minecraft_uuid VARCHAR(36) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users (snowflake) ON DELETE CASCADE ON UPDATE CASCADE
);
