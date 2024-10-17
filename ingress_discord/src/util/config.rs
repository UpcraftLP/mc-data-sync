use anyhow::Context;
use rusty_interaction::types::Snowflake;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use std::{env, fs, path};

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct Config {
    pub roles: Option<Vec<RoleMapping>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RoleMapping {
    pub role: Snowflake,
    pub entitlement: String,
}

pub fn load() -> anyhow::Result<Config> {
    let config_path = env::var("CONFIG_PATH").unwrap_or("/data/config/config.json".to_string());

    let final_path = path::absolute(PathBuf::from(config_path))?;

    if !final_path.try_exists()? {
        eprintln!(
            "Config file not found at {}, creating default config...",
            &final_path.to_string_lossy().replace("\\", "/")
        );
        return create_default_config(&final_path);
    }

    let cfg_string = fs::read_to_string(&final_path).context("Unable to read config file!")?;
    serde_json::from_str::<Config>(&cfg_string).map_err(Into::into)
}

pub fn create_default_config(path: &PathBuf) -> anyhow::Result<Config> {
    let config = Config::default();

    let cfg_string = serde_json::to_string(&config)?;
    if let Some(dir) = path.parent() {
        fs::create_dir_all(dir)?;
    }
    fs::write(path, cfg_string)?;

    Ok(config)
}
