use crate::discord::register::update_global_commands;
use crate::discord::BotInfo;
use rusty_interaction::handler::InteractionHandler;
use rusty_interaction::types::interaction::{Context, InteractionResponse};
use rusty_interaction::{defer, slash_command};

pub(crate) const COMMAND_NAME: &str = "reload";

#[defer]
#[slash_command]
pub(crate) async fn reload_command(
    handler: &mut InteractionHandler,
    ctx: Context,
) -> InteractionResponse {
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

    log::info!("Reloading commands");

    match update_global_commands(handler, ctx.interaction.application_id.unwrap()).await {
        Ok(count) => ctx
            .respond()
            .content(format!("Reloaded {count} commands"))
            .is_ephemeral(true)
            .finish(),
        Err(e) => {
            log::error!("Failed to reload commands: {e}");
            ctx.respond()
                .content("Failed to reload commands")
                .is_ephemeral(true)
                .finish()
        }
    }
}
