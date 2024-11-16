use anyhow::Context;
use rusty_interaction::handler::InteractionHandler;
use rusty_interaction::types::Snowflake;

pub mod commands;
pub mod register;
pub mod members;

#[derive(Debug, Clone)]
pub struct BotInfo {
    pub app_id: Snowflake,
    pub owner_id: Snowflake,
}

pub async fn init() -> anyhow::Result<InteractionHandler> {
    log::info!("Initializing Discord Module");

    let app_id: Snowflake = std::env::var("DISCORD_APP_ID")
        .context("DISCORD_APP_ID not set")?
        .parse()
        .context("DISCORD_APP_ID is not a valid Snowflake")?;

    let public_key = std::env::var("DISCORD_PUBLIC_KEY").context("DISCORD_PUBLIC_KEY not set")?;

    let token = std::env::var("DISCORD_TOKEN").context("DISCORD_TOKEN not set")?;

    let owner_id: Snowflake = std::env::var("DISCORD_BOT_OWNER_ID")
        .context("DISCORD_BOT_OWNER_ID not set")?
        .parse()
        .context("DISCORD_BOT_OWNER_ID is not a valid Snowflake")?;

    let mut handler = InteractionHandler::new(app_id, public_key, Some(&token));

    handler.add_data(BotInfo { app_id, owner_id });

    commands::register_commands(&mut handler);

    Ok(handler)
}
