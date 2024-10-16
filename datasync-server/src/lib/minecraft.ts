export async function playerDbLookup (fetch: (input: RequestInfo | URL, init?: RequestInit | undefined) => Promise<Response>, usernameOrId: string): Promise<PlayerDbPlayer> {
    const response = await fetch(`https://playerdb.co/api/player/minecraft/${usernameOrId}`);
    const json = await response.json() as PlayerDbResponse;
    if(!json.success) throw new Error(json.message);
    return json.data.player;
}

export interface PlayerDbResponse {
    code: string;
    message: string;
    data: PlayerDbData;
    success: boolean;
}

export interface PlayerDbData {
    player: PlayerDbPlayer;
}

export interface PlayerDbPlayer {
    username: string;
    id: string
    avatar: string;
    meta?: unknown;
}