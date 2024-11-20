use actix_web::{web, App, HttpRequest, HttpServer};
use diesel::r2d2::ConnectionManager;
use diesel::SqliteConnection;
use r2d2::Pool;
use rusty_interaction::handler::InteractionHandler;
use std::sync::Mutex;

#[derive(Clone)]
struct ServerData {
    handler: InteractionHandler,
    pool: Pool<ConnectionManager<SqliteConnection>>,
}

pub async fn start(
    handler: InteractionHandler,
    pool: Pool<ConnectionManager<SqliteConnection>>,
    port: u16,
) -> anyhow::Result<()> {
    let data = ServerData { handler, pool };

    let app_data = web::Data::new(Mutex::new(data.clone()));
    HttpServer::new(move || {
        App::new()
            .app_data(app_data.clone())
            .route("/api/discord/interactions",
                   web::post().to(|data: web::Data<Mutex<ServerData>>, req: HttpRequest, body: String| async move {
                       let mut handler: InteractionHandler;
                       {
                           let data = data.lock().unwrap();
                           handler = data.handler.clone();
                       }
                       handler.interaction(req, body).await
                   }),
            )
    })
        .bind(format!("0.0.0.0:{}", port))?
        .run()
        .await
        .map_err(Into::into)
}
