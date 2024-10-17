// @generated automatically by Diesel CLI.

diesel::table! {
    guild_users (user_id, guild_id) {
        user_id -> BigInt,
        guild_id -> BigInt,
        created_at -> Timestamp,
    }
}

diesel::table! {
    guilds (snowflake) {
        snowflake -> BigInt,
        created_at -> Timestamp,
    }
}

diesel::table! {
    users (snowflake) {
        snowflake -> BigInt,
        created_at -> Timestamp,
    }
}

diesel::joinable!(guild_users -> guilds (guild_id));
diesel::joinable!(guild_users -> users (user_id));

diesel::allow_tables_to_appear_in_same_query!(
    guild_users,
    guilds,
    users,
);
