use crate::models::*;
use anyhow::Context;
use diesel::prelude::*;
use diesel::r2d2::ConnectionManager;
use diesel::RunQueryDsl;
use diesel_migrations::{embed_migrations, EmbeddedMigrations, MigrationHarness};
use r2d2::Pool;
use rusty_interaction::types::Snowflake;
use std::error::Error;

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
    minecraft_uuid: &str,
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

    Ok(())
}

pub fn register_role_mapping(
    connection: &mut SqliteConnection,
    guild_snowflake_id: Snowflake,
    role_snowflake_id: Snowflake,
    reward: &str,
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
