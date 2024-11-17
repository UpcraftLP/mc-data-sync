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
    minecraft_users (user_id) {
        user_id -> BigInt,
        minecraft_uuid -> Text,
        created_at -> Timestamp,
    }
}

diesel::table! {
    role_mappings (id) {
        id -> Integer,
        guild_id -> BigInt,
        role_id -> BigInt,
        role_reward -> Text,
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
diesel::joinable!(minecraft_users -> users (user_id));
diesel::joinable!(role_mappings -> guilds (guild_id));

diesel::allow_tables_to_appear_in_same_query!(
    guild_users,
    guilds,
    minecraft_users,
    role_mappings,
    users,
);
