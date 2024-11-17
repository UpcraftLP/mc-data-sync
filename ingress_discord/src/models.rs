use chrono::NaiveDateTime;
use diesel::prelude::*;

#[derive(Queryable, Selectable, Identifiable, PartialEq)]
#[diesel(table_name = crate::schema::users)]
#[diesel(primary_key(snowflake))]
#[diesel(check_for_backend(diesel::sqlite::Sqlite))]
pub struct User {
    pub snowflake: i64,

    pub created_at: NaiveDateTime,
}

#[derive(Insertable, AsChangeset)]
#[diesel(table_name = crate::schema::users)]
pub struct NewUser {
    pub snowflake: i64,
}

#[derive(Queryable, Selectable, Identifiable, PartialEq)]
#[diesel(table_name = crate::schema::guilds)]
#[diesel(primary_key(snowflake))]
#[diesel(check_for_backend(diesel::sqlite::Sqlite))]
pub struct Guild {
    pub snowflake: i64,

    pub created_at: NaiveDateTime,
}

#[derive(Insertable, AsChangeset)]
#[diesel(table_name = crate::schema::guilds)]
pub struct NewGuild {
    pub snowflake: i64,
}

#[derive(Queryable, Selectable, Identifiable, Associations, PartialEq)]
#[diesel(belongs_to(User))]
#[diesel(belongs_to(Guild))]
#[diesel(table_name = crate::schema::guild_users)]
#[diesel(primary_key(user_id, guild_id))]
#[diesel(check_for_backend(diesel::sqlite::Sqlite))]
pub struct GuildUser {
    pub user_id: i64,
    pub guild_id: i64,

    pub created_at: NaiveDateTime,
}

#[derive(Insertable, AsChangeset)]
#[diesel(table_name = crate::schema::guild_users)]
pub struct NewGuildUser {
    pub user_id: i64,
    pub guild_id: i64,
}

#[derive(Queryable, Selectable, Identifiable, Associations, PartialEq, Clone)]
#[diesel(belongs_to(Guild))]
#[diesel(table_name = crate::schema::role_mappings)]
#[diesel(primary_key(id))]
#[diesel(check_for_backend(diesel::sqlite::Sqlite))]
pub struct RoleMapping {
    pub id: i32,
    pub guild_id: i64,
    pub role_id: i64,
    pub role_reward: String,

    pub created_at: NaiveDateTime,
}

#[derive(Insertable, AsChangeset)]
#[diesel(table_name = crate::schema::role_mappings)]
pub struct NewRoleMapping {
    pub guild_id: i64,
    pub role_id: i64,
    pub role_reward: String,
}

#[derive(Queryable, Selectable, Identifiable, Associations, PartialEq, Clone)]
#[diesel(belongs_to(User))]
#[diesel(table_name = crate::schema::minecraft_users)]
#[diesel(primary_key(user_id))]
#[diesel(check_for_backend(diesel::sqlite::Sqlite))]
pub struct MinecraftUser {
    pub user_id: i64,
    pub minecraft_uuid: String,

    pub created_at: NaiveDateTime,
}

#[derive(Insertable, AsChangeset)]
#[diesel(table_name = crate::schema::minecraft_users)]
pub struct NewMinecraftUser {
    pub user_id: i64,
    pub minecraft_uuid: String,
}
