import { env } from "$env/dynamic/private";
import type { Handle, HandleServerError } from "@sveltejs/kit";
import { sequence } from "@sveltejs/kit/hooks";

const authorizationHandle: Handle = async ({ event, resolve }) => {
    // Check if the route is protected by an API key
    if(event.route.id?.includes('/(api-key)/')) {
        const apiKey = event.request.headers.get('x-api-key');
        const expectedApiKey = env.API_KEY;
        if(expectedApiKey === undefined || apiKey !== expectedApiKey) {
            // respond with a 404 error to avoid leaking the existence of the route
            return Response.json({
                error: 'Not found'
            }, {
                status: 404
            });
        }
    }

    return resolve(event);
};

const log: Handle = async ({ event, resolve }) => {
    const response = await resolve(event);
    console.log(`${event.request.method} ${event.url} {${JSON.stringify(event.params)}} -> ${response.status}`);
    return response;
};

// -----------------------------------------------------------------------------

export const handle: Handle = sequence(authorizationHandle, log);

export const handleError: HandleServerError = async ({ error, status }) => {
    if(status !== 404) {
        console.log(error);
    }
};
