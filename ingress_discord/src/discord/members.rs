use crate::util::db::GuildRoleFilter;
use crate::util::http::AddUserEntitlementsInput;
use crate::util::identifier::Identifier;
use crate::util::{db, http};
use diesel::r2d2::ConnectionManager;
use diesel::SqliteConnection;
use lazy_static::lazy_static;
use r2d2::Pool;
use std::collections::HashMap;
use twilight_http::Client;
use twilight_model::id::marker::{GuildMarker, RoleMarker, UserMarker};
use twilight_model::id::Id;

lazy_static! {
    static ref CLIENT: Client =
        Client::new(std::env::var("DISCORD_TOKEN").expect("DISCORD_TOKEN not set"));
    static ref API_URL: String =
        std::env::var("DATASYNC_API_URL").expect("DATASYNC_API_URL not set");
}

#[derive(Debug, Clone)]
struct DiscordRoleMapping {
    guild_id: Id<GuildMarker>,
    role_id: Id<RoleMarker>,
    reward_id: Identifier,
}

// FIXME only run DB access on the web::block thread, not everything
pub async fn update_users(
    pool: Pool<ConnectionManager<SqliteConnection>>,
    filter: Option<GuildRoleFilter>,
) -> anyhow::Result<()> {
    log::info!("Updating users...");

    let mut conn = pool.get().expect("Failed to get connection from pool");

    let role_mappings = db::get_role_mappings(&mut conn, filter)?;
    let mut by_guild: HashMap<Id<GuildMarker>, Vec<DiscordRoleMapping>> = HashMap::new();

    for mapping in role_mappings {
        let new_mapping = DiscordRoleMapping {
            guild_id: mapping
                .guild_id
                .to_string()
                .parse()
                .expect("failed to convert snowflake into ID marker"),
            role_id: mapping
                .role_id
                .to_string()
                .parse()
                .expect("failed to convert snowflake into ID marker"),
            reward_id: mapping
                .role_reward
                .parse()
                .expect("failed to convert role reward into Identifier"),
        };
        by_guild
            .entry(new_mapping.guild_id)
            .or_default()
            .push(new_mapping);
    }

    let mut desired_state: HashMap<Id<UserMarker>, Vec<Identifier>> = HashMap::new();
    for (guild_id, active_mappings) in by_guild {
        log::info!("Checking guild {}", guild_id);

        // TODO this needs to be chained if it gets 1000 users at once
        // TODO error handling
        for member in CLIENT
            .guild_members(guild_id)
            .limit(1000)?
            .await?
            .models()
            .await?
        {
            for mapping in &active_mappings {
                if member.roles.contains(&mapping.role_id) {
                    desired_state
                        .entry(member.user.id)
                        .or_default()
                        .push(mapping.reward_id.clone());
                }
            }
        }
    }

    let update_state: Vec<AddUserEntitlementsInput> =
        db::map_minecraft_users(&mut conn, desired_state)?
            .iter()
            .map(|(uuid, reward_ids)| AddUserEntitlementsInput {
                uuid: uuid.clone(),
                entitlements: reward_ids.iter().map(|id| id.to_string()).collect(),
            })
            .collect::<Vec<_>>();

    let client = http::client();

    for data in update_state {
        client
            .post(format!(
                "{api_url}/users/add-entitlement",
                api_url = *API_URL
            ))
            .json(&data)
            .send()
            .await?
            .error_for_status()?;
    }
    Ok(())
}
