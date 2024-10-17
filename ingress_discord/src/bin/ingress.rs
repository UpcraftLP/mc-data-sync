use anyhow::anyhow;
use diesel::{Connection, SqliteConnection};
use futures::lock::Mutex;
use ingress_discord::discord;
use ingress_discord::discord::BotInfo;
use ingress_discord::util::{config, db};
use std::env;
use std::sync::Arc;
use tokio_cron_scheduler::{Job, JobScheduler};

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    dotenvy::dotenv().ok();

    env_logger::try_init_from_env(env_logger::Env::new().default_filter_or("info"))?;

    log::info!("Starting Discord Ingress v{}", ingress_discord::version());

    let cfg = config::load()?;

    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL must be set");
    let mut connection = SqliteConnection::establish(&database_url)
        .unwrap_or_else(|_| panic!("Error connecting to {}", database_url));

    db::apply_migrations(&mut connection).map_err(|e| anyhow!(e))?;

    let mut handler = discord::init().await?;

    handler.add_data(cfg);

    let mutex = Mutex::new(connection);
    let arc = Arc::new(mutex);
    handler.add_data(arc);
    let app_info = handler.data.get::<BotInfo>().expect("AppInfo not found");
    log::info!("Discord Application ID: {}", app_info.app_id);

    let scheduler = JobScheduler::new().await?;

    let job = Job::new("every 3 hours", |uuid, lock| {
        log::info!("Updating users...")
    })?;
    scheduler.add(job).await?;

    scheduler.start().await?;
    handler.run(3000).await.map_err(Into::into)
}
