use crate::build_info;
use lazy_static::lazy_static;
use reqwest::header;
use reqwest::header::HeaderName;
use serde::{Deserialize, Serialize};
use std::env;
use std::string::ToString;

lazy_static! {
    static ref USER_AGENT: String = {
        let version = crate::version();
        let mut repo_url = build_info::PKG_REPOSITORY.to_string();
        if let Some(commit) = build_info::GIT_COMMIT_HASH_SHORT {
            repo_url = format!("{repo_url}/{commit}");
        }
        let os_family = build_info::CFG_FAMILY;
        let mut user_agent =
            format!("datasync_ingress_discord/{version} ({os_family};) {repo_url}");
        if let Ok(contact) = env::var("UA_CONTACT_INFO") {
            user_agent = format!("{user_agent} ({contact})");
        }

        user_agent
    };
    static ref HTTP_CLIENT: reqwest::Client = reqwest::Client::builder()
        .user_agent(USER_AGENT.clone())
        .default_headers({
            let mut headers = header::HeaderMap::new();
            headers.insert(
                header::ACCEPT,
                header::HeaderValue::from_static("application/json"),
            );
            headers.insert(
                header::CONTENT_TYPE,
                header::HeaderValue::from_static("application/json"),
            );

            let api_key = env::var("DATASYNC_API_KEY").expect("DATASYNC_API_KEY not set");
            headers.insert(
                HeaderName::from_static("x-api-key"),
                header::HeaderValue::try_from(api_key).expect("Failed to create X-Api-Key header"),
            );

            headers
        })
        .build()
        .expect("Failed to build HttpClient");
}

pub fn client() -> reqwest::Client {
    HTTP_CLIENT.clone()
}

pub fn user_agent() -> &'static str {
    USER_AGENT.as_str()
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct AddUserEntitlementsInput {
    pub uuid: String,
    pub entitlements: Vec<String>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct RemoveUserEntitlementsInput {
    pub uuid: String,
    pub entitlements: Vec<String>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct CreateEntitlementInput {
    pub namespace: String,
    pub path: String,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct CreateEntitlementResponse {
    pub id: String,
}
