use crate::models::*;
use anyhow::Context;
use diesel::prelude::*;
use diesel::RunQueryDsl;
use diesel_migrations::{embed_migrations, EmbeddedMigrations, MigrationHarness};
use rusty_interaction::types::Snowflake;
use std::error::Error;

pub fn apply_migrations(
    connection: &mut SqliteConnection,
) -> Result<(), Box<dyn Error + Send + Sync>> {
    const MIGRATIONS: EmbeddedMigrations = embed_migrations!();

    if connection.has_pending_migration(MIGRATIONS)? {
        log::info!("Running database migrations...");
        connection.run_pending_migrations(MIGRATIONS)?;
        log::info!("done!");
    }

    Ok(())
}

pub fn add_user(snowflake_id: Snowflake, connection: &mut SqliteConnection) -> anyhow::Result<()> {
    use crate::schema::users::dsl::*;

    let converted_id = i64::try_from(snowflake_id).expect("Failed to convert Snowflake to i64");

    diesel::insert_into(users)
        .values(NewUser {
            snowflake: converted_id,
        })
        .on_conflict_do_nothing()
        .execute(connection)
        .context("Failed to insert user")?;

    Ok(())
}

pub fn add_guild(snowflake_id: Snowflake, connection: &mut SqliteConnection) -> anyhow::Result<()> {
    use crate::schema::guilds::dsl::*;

    let converted_id = i64::try_from(snowflake_id).expect("Failed to convert Snowflake to i64");

    diesel::insert_into(guilds)
        .values(NewGuild {
            snowflake: converted_id,
        })
        .on_conflict_do_nothing()
        .execute(connection)
        .context("Failed to insert guild")?;

    Ok(())
}

pub fn add_guild_connection(
    user_snowflake_id: Snowflake,
    guild_snowflake_id: Snowflake,
    minecraft_uuid: &str,
    connection: &mut SqliteConnection,
) -> anyhow::Result<()> {
    use crate::schema::guild_users::dsl::*;

    let converted_user_id =
        i64::try_from(user_snowflake_id).expect("Failed to convert Snowflake to i64");
    let converted_guild_id =
        i64::try_from(guild_snowflake_id).expect("Failed to convert Snowflake to i64");

    diesel::insert_into(guild_users)
        .values(NewGuildUser {
            user_id: converted_user_id,
            guild_id: converted_guild_id,
        })
        .on_conflict_do_nothing()
        .execute(connection)
        .context("Failed to insert guild connection")?;

    Ok(())
}
