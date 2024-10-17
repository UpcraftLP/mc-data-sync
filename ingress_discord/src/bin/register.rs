use ingress_discord::discord;
use ingress_discord::discord::BotInfo;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    dotenvy::dotenv().ok();
    env_logger::try_init_from_env(env_logger::Env::new().default_filter_or("info"))?;

    let handler = discord::init().await?;
    let app_info = handler.data.get::<BotInfo>().expect("AppInfo not found");
    log::info!("Discord Application ID: {}", app_info.app_id);
    let count = discord::register::update_global_commands(&handler, app_info.app_id).await?;

    log::info!("Registered {count} commands");

    Ok(())
}
