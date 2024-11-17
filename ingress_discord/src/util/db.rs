use std::collections::HashMap;
use crate::models::*;
use anyhow::Context;
use diesel::prelude::*;
use diesel::r2d2::ConnectionManager;
use diesel::RunQueryDsl;
use diesel_migrations::{embed_migrations, EmbeddedMigrations, MigrationHarness};
use r2d2::Pool;
use rusty_interaction::types::Snowflake;
use std::error::Error;
use twilight_model::id::Id;
use twilight_model::id::marker::UserMarker;
use crate::util::identifier::Identifier;

pub fn apply_migrations(
    pool: &Pool<ConnectionManager<SqliteConnection>>,
) -> Result<(), Box<dyn Error + Send + Sync>> {
    const MIGRATIONS: EmbeddedMigrations = embed_migrations!();

    let mut connection = pool.get()?;

    if connection.has_pending_migration(MIGRATIONS)? {
        log::info!("Running database migrations...");
        connection.run_pending_migrations(MIGRATIONS)?;
        log::info!("done!");
    }

    Ok(())
}

fn i64(snowflake: Snowflake) -> i64 {
    i64::try_from(snowflake).expect("Failed to convert Snowflake to i64")
}

pub fn add_user(connection: &mut SqliteConnection, snowflake_id: Snowflake) -> anyhow::Result<()> {
    use crate::schema::users::dsl::*;

    diesel::insert_into(users)
        .values(NewUser {
            snowflake: i64(snowflake_id),
        })
        .on_conflict_do_nothing()
        .execute(connection)
        .context("Failed to insert user")?;

    Ok(())
}

pub fn add_guild(connection: &mut SqliteConnection, snowflake_id: Snowflake) -> anyhow::Result<()> {
    use crate::schema::guilds::dsl::*;

    diesel::insert_into(guilds)
        .values(NewGuild {
            snowflake: i64(snowflake_id),
        })
        .on_conflict_do_nothing()
        .execute(connection)
        .context("Failed to insert guild")?;

    Ok(())
}

pub fn add_guild_connection(
    connection: &mut SqliteConnection,
    user_snowflake_id: Snowflake,
    guild_snowflake_id: Snowflake,
    minecraft_uuid_string: &str,
) -> anyhow::Result<()> {
    use crate::schema::guild_users::dsl::*;

    diesel::insert_into(guild_users)
        .values(NewGuildUser {
            user_id: i64(user_snowflake_id),
            guild_id: i64(guild_snowflake_id),
        })
        .on_conflict_do_nothing()
        .execute(connection)
        .context("Failed to insert guild connection")?;

    use crate::schema::minecraft_users::dsl::*;
    use crate::schema::minecraft_users::dsl::user_id;
    diesel::insert_into(minecraft_users)
        .values(NewMinecraftUser {
            user_id: i64(user_snowflake_id),
            minecraft_uuid: minecraft_uuid_string.to_string(),
        })
        .on_conflict(user_id)
        .do_update()
        .set(minecraft_uuid.eq(minecraft_uuid_string))
        .execute(connection)
        .context("Failed to insert minecraft account mapping")?;

    Ok(())
}

pub fn register_role_mapping(
    connection: &mut SqliteConnection,
    guild_snowflake_id: Snowflake,
    role_snowflake_id: Snowflake,
    reward: &Identifier,
) -> anyhow::Result<()> {
    use crate::schema::role_mappings::dsl::*;

    diesel::insert_into(role_mappings)
        .values(NewRoleMapping {
            guild_id: i64(guild_snowflake_id),
            role_id: i64(role_snowflake_id),
            role_reward: reward.to_string(),
        })
        .on_conflict_do_nothing()
        .execute(connection)
        .map_err(|e| e.into())
        .map(|_| ())
}

pub fn remove_role_mapping(
    connection: &mut SqliteConnection,
    guild_id_snowflake: Snowflake,
    role_id_snowflake: Snowflake,
    reward: &str,
) {
    use crate::schema::role_mappings::dsl::*;

    diesel::delete(
        role_mappings
            .filter(guild_id.eq(&i64(guild_id_snowflake)))
            .filter(role_id.eq(&i64(role_id_snowflake)))
            .filter(role_reward.eq(reward)),
    )
    .execute(connection)
    .expect("Failed to delete role mapping");
}

#[derive(Debug, Copy, Clone)]
pub struct  GuildRoleFilter {
    pub guild_id: Snowflake,
    pub role_id: Snowflake,
}

pub fn get_guild_users(connection: &mut SqliteConnection, filter: Option<GuildRoleFilter>) -> anyhow::Result<Vec<GuildUser>> {
    use crate::schema::guild_users::dsl::*;

    let users: Vec<GuildUser>;
    let query = guild_users.select(GuildUser::as_select());

    if let Some(filter) = filter {
        users = query
            .filter(guild_id.eq(i64(filter.guild_id)))
            .load(connection)
            .context("Failed to load users")?;
    } else {
        users = query.load(connection).context("Failed to load users")?;
    }

    Ok(users)
}

pub fn get_role_mappings(connection: &mut SqliteConnection, filter: Option<GuildRoleFilter>) -> anyhow::Result<Vec<RoleMapping>> {
    use crate::schema::role_mappings::dsl::*;

    let mappings: Vec<RoleMapping>;
    let query = role_mappings.select(RoleMapping::as_select());

    if let Some(filter) = filter {
        mappings = query
            .filter(role_id.eq(i64(filter.role_id)))
            .load(connection)
            .context("Failed to load role mappings")?;
    } else {
        mappings = query.load(connection).context("Failed to load role mappings")?;
    }

    Ok(mappings)
}

pub fn map_minecraft_users(connection: &mut SqliteConnection, values: HashMap<Id<UserMarker>, Vec<Identifier>>) -> anyhow::Result<Vec<(String, Vec<Identifier>)>> {
    use crate::schema::minecraft_users::dsl::*;

    let users = values.keys().map(|id| i64(id.to_string().parse::<Snowflake>().unwrap())).collect::<Vec<_>>();

    let result = minecraft_users.select((user_id, minecraft_uuid))
        .filter(user_id.eq_any(users))
        .load::<(i64, String)>(connection).context("Failed to load minecraft users")?;

    Ok(result.iter().map(|x| {
        let id = x.0.to_string().parse::<Id<UserMarker>>().unwrap();
        let uuid = x.1.clone();

        (uuid, values.get(&id).unwrap().clone())
    }).collect())
}