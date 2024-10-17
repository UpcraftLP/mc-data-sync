use crate::build_info;
use once_cell::sync::Lazy;
use std::env;
use std::string::ToString;

pub const USER_AGENT: Lazy<String> = Lazy::new(|| {
    let version = crate::version();
    let mut repo_url = build_info::PKG_REPOSITORY.to_string();
    if let Some(commit) = build_info::GIT_COMMIT_HASH_SHORT {
        repo_url = format!("{repo_url}/{commit}");
    }
    let os_family = build_info::CFG_FAMILY;
    let mut user_agent = format!("datasync_ingress_discord/{version} ({os_family};) {repo_url}");
    if let Ok(contact) = env::var("UA_CONTACT_INFO") {
        user_agent = format!("{user_agent} ({contact})");
    }

    user_agent
});
