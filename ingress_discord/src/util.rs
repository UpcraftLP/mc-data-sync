use rusty_interaction::types::Snowflake;
use twilight_model::id::Id;
use twilight_model::id::marker::{GuildMarker, RoleMarker, UserMarker};

pub mod config;
pub mod db;
pub mod http;
pub mod identifier;

pub fn snowflake_to_user_marker(snowflake: Snowflake) -> Id<UserMarker> {
    snowflake
        .to_string()
        .parse()
        .expect("Failed to convert snowflake to user marker")
}

pub fn snowflake_to_role_marker(snowflake: Snowflake) -> Id<RoleMarker> {
    snowflake
        .to_string()
        .parse()
        .expect("Failed to convert snowflake to role marker")
}

pub fn snowflake_to_guild_marker(snowflake: Snowflake) -> Id<GuildMarker> {
    snowflake
        .to_string()
        .parse()
        .expect("Failed to convert snowflake to guild marker")
}
