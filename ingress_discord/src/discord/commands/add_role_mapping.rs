use crate::discord::members;
use crate::util::db;
use crate::util::db::GuildRoleFilter;
use crate::util::identifier::Identifier;
use actix_web::web;
use anyhow::bail;
use diesel::r2d2::ConnectionManager;
use diesel::SqliteConnection;
use r2d2::Pool;
use rusty_interaction::handler::InteractionHandler;
use rusty_interaction::types::interaction::{Context, InteractionResponse};
use rusty_interaction::types::Snowflake;
use rusty_interaction::{defer, slash_command};
use std::future::IntoFuture;
use tracing::{error, info};

pub(crate) const COMMAND_NAME: &str = "add_role_mapping";

// FIXME rusty_interaction can't do modals properly, so we have to do one role at a time :'(
#[defer]
#[slash_command]
pub(crate) async fn add_role_mapping_command(
    handler: &mut InteractionHandler,
    ctx: Context,
) -> InteractionResponse {
    let pool = handler
        .data
        .get::<Pool<ConnectionManager<SqliteConnection>>>()
        .expect("Failed to get connection pool from interaction handler")
        .clone();

    let guild_snowflake = ctx
        .interaction
        .guild_id
        .expect("must run command within a guild");

    let data = &ctx.interaction.data.clone().unwrap();
    let opts = data.options.clone().unwrap();

    let reward_id_raw = opts
        .iter()
        .find(|&o| o.name == "reward_id")
        .expect("reward_id is required")
        .value
        .clone();
    let reward_id = reward_id_raw.parse::<Identifier>();

    if let Err(e) = reward_id {
        return ctx
            .respond()
            .is_ephemeral(true)
            .content(format!("Invalid Identifier `{reward_id_raw}` - {e:#}"))
            .finish();
    }
    let reward_id: Identifier = reward_id.unwrap();

    let role_snowflake: Snowflake = opts
        .iter()
        .find(|&o| o.name == "role")
        .expect("role is required")
        .value
        .clone()
        .parse()
        .expect("role is not a valid Snowflake");

    info!("Adding role mapping for reward_id: {reward_id} and role: {role_snowflake}");

    let reward_id_clone = reward_id.clone();
    let clone_pool = pool.clone();
    let db_err = web::block(move || {
        let mut conn = clone_pool
            .get()
            .expect("Failed to get connection from pool");

        if let Err(e) = db::add_guild(&mut conn, guild_snowflake) {
            bail!("failed to add guild: {e:#}");
        }

        if let Err(e) =
            db::register_role_mapping(&mut conn, guild_snowflake, role_snowflake, &reward_id_clone)
        {
            bail!("failed to add role mapping: {e:#}");
        }
        Ok(())
    })
    .await
    .expect("blocking error");

    if let Err(cause) = db_err {
        error!(%cause, "Failed to add role mapping `{role_snowflake}->{reward_id}`");
        return ctx
            .respond()
            .is_ephemeral(true)
            .content(format!(
                "DB ERROR: Failed to add role mapping `{role_snowflake}->{reward_id}`"
            ))
            .finish();
    }

    if let Err(cause) = members::create_entitlement(&reward_id).await {
        error!(%cause, "Failed to send create entitlement request for `{reward_id}`");
        return ctx
            .respond()
            .is_ephemeral(true)
            .content("HTTP ERROR: Failed to send web request!".to_string())
            .finish();
    }

    let filter = GuildRoleFilter {
        guild_id: guild_snowflake,
        role_id: role_snowflake,
    };

    if let Err(cause) = web::block(move || members::update_users(pool, Some(filter)).into_future())
        .await
        .expect("blocking error")
        .await
    {
        error!(%cause, "Failed to apply update to new users");
        return ctx.respond().is_ephemeral(true).content(format!("Successfully added reward `{reward_id}` for <@&{role_snowflake}>\n\n`ERROR:` Unable to update users, will retry later!```\n{cause}\n```")).finish();
    }

    ctx.respond()
        .is_ephemeral(true)
        .content(format!(
            "Successfully added reward `{reward_id}` for <@&{role_snowflake}>"
        ))
        .finish()
}
