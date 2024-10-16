use crate::discord::commands;
use rusty_interaction::handler::InteractionHandler;
use rusty_interaction::types::application::{
    ApplicationCommand, ApplicationCommandOption, ApplicationCommandOptionType,
    SlashCommandDefinitionBuilder,
};
use rusty_interaction::types::Snowflake;
use rusty_interaction::Builder;

const BASE_URL: &str = rusty_interaction::BASE_URL;

pub async fn update_global_commands(
    handler: &InteractionHandler,
    app_id: Snowflake,
) -> anyhow::Result<usize> {
    let commands: Vec<ApplicationCommand> = vec![
        SlashCommandDefinitionBuilder::default()
            .name(commands::reload::COMMAND_NAME)
            .description("Reload the commands")
            .default_permission(false)
            .build()?,
        SlashCommandDefinitionBuilder::default()
            .name(commands::link::COMMAND_NAME)
            .description("Link your Minecraft account")
            .add_option(
                ApplicationCommandOption::default()
                    .name("username")
                    .option_type(&ApplicationCommandOptionType::String)
                    .required(&true)
                    .description("Your Minecraft username or UUID"),
            )
            .build()?,
    ];

    let url = format!("{BASE_URL}/applications/{app_id}/commands");
    let response = handler
        .client()
        .clone()
        .put(url)
        .json(&commands)
        .send()
        .await?;

    if !response.status().is_success() {
        anyhow::bail!(
            "Failed to update global commands: {:?}",
            response.text().await?
        );
    }

    Ok(commands.len())
}
