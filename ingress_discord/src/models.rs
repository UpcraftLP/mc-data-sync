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
