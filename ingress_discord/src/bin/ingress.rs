use anyhow::anyhow;
use diesel::r2d2::ConnectionManager;
use diesel::SqliteConnection;
use ingress_discord::discord;
use ingress_discord::discord::BotInfo;
use ingress_discord::util::{config, db};
use ingress_discord::web;
use r2d2::Pool;
use std::env;
use tokio_cron_scheduler::{Job, JobScheduler};

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    ingress_discord::init();

    env_logger::try_init_from_env(env_logger::Env::new().default_filter_or("info"))?;

    log::info!("Starting Discord Ingress v{}", ingress_discord::version());

    let cfg = config::load()?;

    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL must be set");
    let manager = ConnectionManager::<SqliteConnection>::new(database_url.clone());
    let pool: Pool<ConnectionManager<SqliteConnection>> = Pool::builder()
        .build(manager)
        .expect(format!("Failed to connect to database at {}", database_url).as_str());

    db::apply_migrations(&pool).map_err(|e| anyhow!(e))?;

    let mut handler = discord::init().await?;

    handler.add_data(cfg);

    handler.add_data(pool.clone());
    let app_info = handler.data.get::<BotInfo>().expect("AppInfo not found");
    log::info!("Discord Application ID: {}", app_info.app_id);

    let scheduler = JobScheduler::new().await?;

    let job = Job::new("every 3 hours", |_uuid, _lock| {
        log::info!("Updating users...")
    })?;
    scheduler.add(job).await?;

    scheduler.start().await?;
    web::server::start(handler, pool.clone(), 3000).await
}