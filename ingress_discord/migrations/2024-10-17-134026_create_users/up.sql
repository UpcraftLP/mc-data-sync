CREATE TABLE users (
    snowflake BIGINT PRIMARY KEY NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guilds (
    snowflake BIGINT PRIMARY KEY NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guild_users (
    user_id BIGINT NOT NULL,
    guild_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, guild_id),
    FOREIGN KEY (user_id) REFERENCES users(snowflake) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (guild_id) REFERENCES guilds(snowflake) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_guild_users_by_guild ON guild_users(guild_id);
