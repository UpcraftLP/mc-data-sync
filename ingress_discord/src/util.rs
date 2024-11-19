use rusty_interaction::types::Snowflake;
use twilight_model::id::marker::UserMarker;
use twilight_model::id::Id;

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
