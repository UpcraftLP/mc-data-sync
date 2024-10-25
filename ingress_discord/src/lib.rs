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

pub fn init() {
    dotenvy::dotenv().ok();
}
