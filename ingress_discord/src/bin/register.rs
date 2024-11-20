use ingress_discord::discord;
use ingress_discord::discord::BotInfo;
use tracing::info;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    ingress_discord::init("register");

    let handler = discord::init().await?;
    let app_info = handler.data.get::<BotInfo>().expect("AppInfo not found");
    info!("Discord Application ID: {}", app_info.app_id);
    let count = discord::register::update_global_commands(&handler, app_info.app_id).await?;

    info!("Registered {count} commands");

    Ok(())
}
