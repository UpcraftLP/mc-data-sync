use tracing_subscriber::filter::LevelFilter;
use tracing_subscriber::layer::SubscriberExt;
use tracing_subscriber::util::SubscriberInitExt;
use tracing_subscriber::{fmt, EnvFilter};

pub mod discord;
pub mod models;
pub mod schema;
pub mod util;
pub mod web;

pub mod build_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

pub const VERSION: Option<&str> = build_info::GIT_VERSION;

pub fn version() -> &'static str {
    VERSION.unwrap_or("UNKNOWN")
}

pub fn init(_prefix: &'static str) {
    dotenvy::dotenv().ok();
    tracing_subscriber::registry()
        .with(fmt::layer())
        .with(
            EnvFilter::builder()
                .with_default_directive(LevelFilter::INFO.into())
                .from_env_lossy()
                .add_directive("tokio_cron_scheduler=error".parse().unwrap())
                .add_directive("actix_web=error".parse().unwrap())
                .add_directive("twilight_http_ratelimiting=error".parse().unwrap()),
        )
        .init();
}
