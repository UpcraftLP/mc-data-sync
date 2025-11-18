use crate::discord::members;
use crate::util::{
    db, http, snowflake_to_guild_marker, snowflake_to_role_marker, snowflake_to_user_marker,
};
use actix_web::web;
use diesel::SqliteConnection;
use diesel::r2d2::ConnectionManager;
use r2d2::Pool;
use rusty_interaction::handler::InteractionHandler;
use rusty_interaction::types::interaction::{Context, InteractionResponse};
use rusty_interaction::{defer, slash_command};
use serde::{Deserialize, Serialize};
use tracing::{error, info};
use twilight_model::id::Id;
use twilight_model::id::marker::RoleMarker;

pub(crate) const COMMAND_NAME: &str = "link";

#[derive(Serialize, Deserialize, Debug, Clone)]
struct PlayerDbResponse {
    success: bool,
    error: Option<bool>,
    code: String,
    message: String,
    data: PlayerDbData,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
struct PlayerDbData {
    player: Option<PlayerDbPlayer>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
struct PlayerDbPlayer {
    username: String,
    id: String,
    raw_id: String,
    avatar: String,
}

#[defer]
#[slash_command]
pub(crate) async fn link_command(
    handler: &mut InteractionHandler,
    ctx: Context,
) -> InteractionResponse {
    let pool = handler
        .data
        .get::<Pool<ConnectionManager<SqliteConnection>>>()
        .expect("Failed to get connection pool from interaction handler")
        .clone();

    let data = &ctx.interaction.data.clone().unwrap();
    let opts = data.options.clone().unwrap();

    let discord_snowflake = ctx
        .interaction
        .member
        .clone()
        .expect("must run commands within a guild")
        .user
        .id;
    let guild_snowflake = ctx
        .interaction
        .guild_id
        .expect("must run commands within a guild");

    let roles: Vec<Id<RoleMarker>> = ctx
        .interaction
        .member
        .clone()
        .expect("must run commands within a guild")
        .roles
        .clone()
        .iter()
        .map(|&sf| snowflake_to_role_marker(sf))
        .collect();

    let name_option = opts
        .iter()
        .find(|&o| o.name == "username")
        .expect("username option is required");

    let username_or_id = name_option.value.clone();
    let url = format!("https://playerdb.co/api/player/minecraft/{username_or_id}");

    let error: Option<String>;

    let client = reqwest::Client::builder()
        .user_agent(http::user_agent())
        .build()
        .expect("failed to construct http client"); //TODO better error handling
    match client.get(&url).send().await {
        Ok(response) => match response.json::<PlayerDbResponse>().await {
            Ok(db_response) => {
                if !db_response.success {
                    if db_response.code == "minecraft.invalid_username" {
                        error = Some(format!("No player found for '{username_or_id}'"));
                    } else {
                        error!(
                            "Received error from PlayerDb: {message}",
                            message = db_response.message
                        );
                        error = Some(db_response.message);
                    }
                } else {
                    let player_data = db_response.data.player.expect("Player data is missing");

                    let clone_pool = pool.clone();
                    match web::block(move || {
                        let mut conn = clone_pool
                            .get()
                            .expect("Failed to get connection from pool");
                        db::add_guild_connection(
                            &mut conn,
                            discord_snowflake,
                            guild_snowflake,
                            player_data.id.as_str(),
                        )
                    })
                    .await
                    .expect("blocking error")
                    {
                        Ok(()) => {
                            match members::update_single_user(
                                pool.clone(),
                                snowflake_to_user_marker(discord_snowflake),
                                snowflake_to_guild_marker(guild_snowflake),
                                &roles,
                            )
                            .await
                            {
                                Err(cause) => {
                                    error!("Failed to update user: {cause:#}");
                                    error = Some("Internal Server Error".to_string());
                                }
                                _ => {
                                    info!(
                                        "Link success: Discord: {discord_snowflake}, Minecraft: {mc_username}",
                                        mc_username = &player_data.username
                                    );
                                    return ctx
                                        .respond()
                                        .is_ephemeral(true)
                                        .content(format!(
                                            "Successfully linked with user: `{mc_username}`",
                                            mc_username = &player_data.username
                                        ))
                                        .finish();
                                }
                            }
                        }
                        Err(cause) => {
                            error!("Failed add guild connection: {cause:#}");
                            error = Some("Internal Server Error".to_string());
                        }
                    }
                }
            }
            Err(cause) => {
                error!("Failed to parse api response: {cause:#}");
                error = Some("Internal Server Error".to_string());
            }
        },
        Err(cause) => {
            error!("Failed to link! DiscordUser: {discord_snowflake}, Input: '{username_or_id}', Error: {cause:#}");
            error = Some("Internal Server Error".to_string());
        }
    }

    ctx.respond()
        .is_ephemeral(true)
        .content(format!(
            "Failed to link: `{err}`",
            err = error.unwrap_or("unknown error".to_string())
        ))
        .finish()
}
