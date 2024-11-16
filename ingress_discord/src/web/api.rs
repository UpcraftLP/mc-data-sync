use crate::util::db;
use actix_web::error::ErrorInternalServerError;
use actix_web::{web, HttpRequest, HttpResponse};
use diesel::r2d2::ConnectionManager;
use diesel::SqliteConnection;
use r2d2::Pool;
use rusty_interaction::types::Snowflake;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Deserialize, Serialize)]
struct RoleMappingInput {
    guild_id: Snowflake,
    role_id: Snowflake,
    reward: String,
}

pub async fn register_role_mapping(
    _: HttpRequest,
    body: String,
    pool: Pool<ConnectionManager<SqliteConnection>>,
) -> actix_web::Result<HttpResponse> {
    let input = serde_json::from_str::<RoleMappingInput>(&body).map_err(|e| {
        log::error!("Failed to parse JSON body: {e}");
        actix_web::error::ErrorBadRequest("Failed to parse JSON body")
    })?;

    web::block(move || {
        let mut conn = pool.get().expect("Failed to get connection from pool");

        db::register_role_mapping(
            &mut conn,
            input.role_id,
            input.guild_id,
            input.reward.as_str(),
        ).unwrap_or_else(|e| {
            log::error!("Failed to register role mapping: {e}");
        });
    })
    .await
    .map_err(ErrorInternalServerError)?;

    tokio::spawn(async move {
        // TODO apply new role to all users in the guild
    });

    Ok(HttpResponse::Created().finish())
}
