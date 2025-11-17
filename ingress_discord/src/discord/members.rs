use crate::models::RoleMapping;
use crate::util::db::GuildRoleFilter;
use crate::util::http::{
    AddUserEntitlementsInput, CreateEntitlementInput, CreateEntitlementResponse,
};
use crate::util::identifier::Identifier;
use crate::util::{db, http};
use actix_web::web;
use anyhow::Context;
use diesel::r2d2::ConnectionManager;
use diesel::SqliteConnection;
use itertools::Itertools;
use lazy_static::lazy_static;
use r2d2::Pool;
use std::collections::HashMap;
use tracing::info;
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

impl DiscordRoleMapping {
    fn from_role_mapping(role_mapping: &RoleMapping) -> Self {
        DiscordRoleMapping {
            guild_id: role_mapping
                .guild_id
                .to_string()
                .parse()
                .expect("failed to convert snowflake into ID marker"),
            role_id: role_mapping
                .role_id
                .to_string()
                .parse()
                .expect("failed to convert snowflake into ID marker"),
            reward_id: role_mapping
                .role_reward
                .parse()
                .expect("failed to convert role reward into Identifier"),
        }
    }
}

fn role_mappings_by_guild(
    connection: &mut SqliteConnection,
    filter: Option<GuildRoleFilter>,
) -> anyhow::Result<HashMap<Id<GuildMarker>, Vec<DiscordRoleMapping>>> {
    let role_mappings = db::get_role_mappings(connection, filter)?;
    let mut result: HashMap<Id<GuildMarker>, Vec<DiscordRoleMapping>> = HashMap::new();
    for mapping in role_mappings {
        let new_mapping = DiscordRoleMapping::from_role_mapping(&mapping);
        result
            .entry(new_mapping.guild_id)
            .or_default()
            .push(new_mapping);
    }

    Ok(result)
}

fn get_desired_state(
    roles: &[Id<RoleMarker>],
    active_mappings: &Vec<DiscordRoleMapping>,
) -> Vec<Identifier> {
    let mut result: Vec<Identifier> = Vec::new();
    for mapping in active_mappings {
        if roles.contains(&mapping.role_id) {
            result.push(mapping.reward_id.clone());
        }
    }

    result
}

// FIXME only run DB access on the web::block thread, not everything
pub async fn update_users(
    pool: Pool<ConnectionManager<SqliteConnection>>,
    filter: Option<GuildRoleFilter>,
) -> anyhow::Result<()> {
    info!("Updating users...");

    let mut conn = pool.get().expect("Failed to get connection from pool");

    let by_guild = role_mappings_by_guild(&mut conn, filter)?;

    let mut desired_state: HashMap<Id<UserMarker>, Vec<Identifier>> = HashMap::new();
    for (guild_id, active_mappings) in by_guild {
        info!("Checking guild {guild_id}");

        // TODO this needs to be chained if it gets 1000 users at once
        // TODO error handling
        for member in CLIENT
            .guild_members(guild_id)
            .limit(1000)?
            .await?
            .models()
            .await?
        {
            let member_desired_state = get_desired_state(&member.roles, &active_mappings);
            if !member_desired_state.is_empty() {
                desired_state
                    .entry(member.user.id)
                    .or_default()
                    .extend(member_desired_state);
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
                "{api_url}/v0/users/add-entitlements",
                api_url = *API_URL
            ))
            .json(&data)
            .send()
            .await?
            .error_for_status()?;
    }
    Ok(())
}

pub async fn update_single_user(
    pool: Pool<ConnectionManager<SqliteConnection>>,
    user_snowflake: Id<UserMarker>,
    guild: Id<GuildMarker>,
    roles: &[Id<RoleMarker>],
) -> anyhow::Result<()> {
    // first check if user even has a minecraft account linked
    let clone_pool = pool.clone();
    let Some(mc_user) = web::block(move || {
        let mut conn = clone_pool
            .get()
            .expect("Failed to get connection from pool");
        db::get_minecraft_user(&mut conn, user_snowflake)
    })
    .await
    .expect("blocking error")?
    else {
        // TODO do we need better handling here?
        return Ok(());
    };

    let by_guild = web::block(move || {
        let mut conn = pool.get().expect("Failed to get connection from pool");
        role_mappings_by_guild(&mut conn, None)
    })
    .await
    .expect("blocking error")?;
    let mut desired_state: Vec<Identifier> = Vec::new();
    for (guild_id, active_mappings) in by_guild {
        // if we already have the roles, no need to query again
        if guild_id == guild {
            desired_state.append(&mut get_desired_state(roles, &active_mappings));
            continue;
        }

        // else try to get the member from discord.
        // this will return an error if the user is not in the guild, so we ignore that
        if let Ok(result) = CLIENT.guild_member(guild_id, user_snowflake).await {
            let member = result.model().await?;
            desired_state.append(&mut get_desired_state(&member.roles, &active_mappings));
        }
    }

    let data = AddUserEntitlementsInput {
        uuid: mc_user.minecraft_uuid.clone(),
        entitlements: desired_state
            .iter()
            .unique()
            .map(|id| id.to_string())
            .collect(),
    };

    if data.entitlements.is_empty() {
        info!(
            "No entitlements to update for user {mc_uuid}",
            mc_uuid = mc_user.minecraft_uuid
        );
        return Ok(());
    }

    let client = http::client();
    client
        .post(format!(
            "{api_url}/v0/users/add-entitlements",
            api_url = *API_URL
        ))
        .json(&data)
        .send()
        .await
        .with_context(|| {
            format!(
                "Failed to send request to {api_url} for user {mc_uuid}",
                api_url = *API_URL,
                mc_uuid = mc_user.minecraft_uuid
            )
        })?
        .error_for_status()
        .with_context(|| {
            format!(
                "Failed to send request to {api_url} for user {mc_uuid}",
                api_url = *API_URL,
                mc_uuid = mc_user.minecraft_uuid
            )
        })?;

    Ok(())
}

pub async fn create_entitlement(id: &Identifier) -> anyhow::Result<CreateEntitlementResponse> {
    let input = CreateEntitlementInput {
        namespace: id.namespace.clone(),
        path: id.path.clone(),
    };

    let client = http::client();
    client
        .put(format!("{api_url}/v0/entitlements", api_url = *API_URL))
        .json(&input)
        .send()
        .await?
        .error_for_status()?
        .json::<CreateEntitlementResponse>()
        .await
        .map_err(Into::into)
}
