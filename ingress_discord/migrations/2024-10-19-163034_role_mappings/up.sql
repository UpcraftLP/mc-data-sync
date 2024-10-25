CREATE TABLE role_mappings (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,

    guild_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    role_reward TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL,

    FOREIGN KEY (guild_id) REFERENCES guilds(snowflake) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT unique_role_mapping UNIQUE (role_id, guild_id, role_reward)
);

CREATE INDEX idx_role_mappings_by_guild ON role_mappings(guild_id);
CREATE INDEX idx_role_mappings_by_guild_role ON role_mappings(guild_id, role_id);