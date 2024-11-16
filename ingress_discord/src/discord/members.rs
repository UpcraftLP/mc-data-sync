use rusty_interaction::types::Snowflake;

pub fn update_users(filter_role_id: Option<Snowflake>) -> anyhow::Result<()> {
    log::info!("Updating users...");

    Err(anyhow::anyhow!("Updating users is not implemented yet!"))
}