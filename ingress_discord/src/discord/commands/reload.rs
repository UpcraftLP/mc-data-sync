use crate::discord::BotInfo;
use crate::discord::register::update_global_commands;
use lazy_static::lazy_static;
use rusty_interaction::handler::InteractionHandler;
use rusty_interaction::types::interaction::{Context, InteractionResponse};
use rusty_interaction::{defer, slash_command};
use tracing::{error, info};

pub(crate) const COMMAND_NAME: &str = "reload";

lazy_static! {
    static ref ENABLED: bool = std::env::var("ENABLE_RELOAD_COMMAND")
        .map(|s| s.trim_ascii().eq_ignore_ascii_case("true"))
        .unwrap_or(false);
}

pub fn enabled() -> bool {
    *ENABLED
}

#[defer]
#[slash_command]
pub(crate) async fn reload_command(
    handler: &mut InteractionHandler,
    ctx: Context,
) -> InteractionResponse {
    if !enabled() {
        return ctx
            .respond()
            .content("Command is disabled")
            .is_ephemeral(true)
            .finish();
    }

    let bot_info = handler.data.get::<BotInfo>().unwrap();

    match ctx.author_id {
        Some(id) => {
            if id != bot_info.owner_id {
                return ctx
                    .respond()
                    .content("Only the application owner can use this command")
                    .is_ephemeral(true)
                    .finish();
            }
        }
        None => {
            return ctx
                .respond()
                .content("Cannot use this command without being a user")
                .is_ephemeral(true)
                .finish();
        }
    }

    info!("Reloading commands");

    match update_global_commands(handler, ctx.interaction.application_id.unwrap()).await {
        Ok(count) => ctx
            .respond()
            .content(format!("Reloaded {count} commands"))
            .is_ephemeral(true)
            .finish(),
        Err(cause) => {
            error!(%cause, "Failed to reload commands");
            ctx.respond()
                .content("Failed to reload commands")
                .is_ephemeral(true)
                .finish()
        }
    }
}
