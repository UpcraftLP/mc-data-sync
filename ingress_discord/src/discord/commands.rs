pub(crate) mod link;
pub(crate) mod reload;

use rusty_interaction::handler::InteractionHandler;

pub(crate) fn register_commands(handler: &mut InteractionHandler) {
    handler.add_global_command(reload::COMMAND_NAME, reload::reload_command);
    handler.add_global_command(link::COMMAND_NAME, link::link_command);
}
