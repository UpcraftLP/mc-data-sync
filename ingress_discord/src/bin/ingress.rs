use anyhow::{Context, anyhow};
use diesel::SqliteConnection;
use diesel::r2d2::ConnectionManager;
use ingress_discord::discord;
use ingress_discord::discord::{BotInfo, members};
use ingress_discord::util::{config, db};
use ingress_discord::web;
use r2d2::Pool;
use std::env;
use std::time::Duration;
use tokio_cron_scheduler::{Job, JobScheduler};
use tracing::{error, info};

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    ingress_discord::init("ingress_discord");

    info!("Starting Discord Ingress v{}", ingress_discord::version());

    let cfg = config::load()?;

    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL must be set");
    let manager = ConnectionManager::<SqliteConnection>::new(database_url.clone());
    let pool: Pool<ConnectionManager<SqliteConnection>> = Pool::builder()
        .build(manager)
        .with_context(|| format!("Failed to connect to database at {database_url}"))?;

    db::apply_migrations(&pool).map_err(|e| anyhow!(e))?;

    let mut handler = discord::init().await?;

    handler.add_data(cfg);

    handler.add_data(pool.clone());
    let app_info = handler.data.get::<BotInfo>().expect("AppInfo not found");
    info!("Discord Application ID: {}", app_info.app_id);

    let scheduler = JobScheduler::new().await?;

    let pool_clone = pool.clone();
    let repeating_job = Job::new_async("every 3 hours", move |_uuid, _lock| {
        Box::pin({
            let value = pool_clone.clone();
            async move {
                if let Err(cause) =
                    actix_web::web::block(move || members::update_users(value, None))
                        .await
                        .expect("blocking error")
                        .await
                {
                    error!(%cause, "Failed to run scheduled user update");
                }
            }
        })
    })?;
    scheduler.add(repeating_job).await?;

    let pool_clone = pool.clone();
    let startup_job = Job::new_one_shot_async(Duration::from_secs(15), move |_uuid, _lock| {
        Box::pin({
            let value = pool_clone.clone();
            async move {
                if let Err(cause) =
                    actix_web::web::block(move || members::update_users(value, None))
                        .await
                        .expect("blocking error")
                        .await
                {
                    error!(%cause, "Failed to run scheduled user update");
                }
            }
        })
    })?;
    scheduler.add(startup_job).await?;

    scheduler.start().await?;
    web::server::start(handler, pool.clone(), 3000).await
}
