import type { RequestHandler } from "../$types";
import { ID_ENTITLEMENTS } from "$lib";
import { prisma } from "$lib/server/db";
import { isJsonString } from "$lib/util";

export const GET: RequestHandler = async ({ params }) => {
    const id = `${params.namespace}:${params.path}`;

    if (id === ID_ENTITLEMENTS) {
        const data = await prisma.minecraftUser.findUnique({
            where: {
                id: params.uuid
            },
            select: {
                entitlements: {
                    select: {
                        id: true
                    }
                }
            }
        });

        if (data === null) {
            return Response.json({
                values: []
            });
        }

        return Response.json({
            values: data.entitlements.map(e => e.id)
        });
    }

    const data = await prisma.userConfigData.findUnique({
        where: {
            minecraftUserId_entitlementId: {
                minecraftUserId: params.uuid,
                entitlementId: id
            }
        },
        select: {
            value: true
        }
    });

    if (data !== null) {
        return new Response(data.value, {
            headers: {
                "Content-Type": "application/json"
            }
        });
    }

    return Response.json({
        error: "Resource not found"
    }, {
        status: 404
    });
}

export const POST: RequestHandler = async ({ params, request }) => {
    const id = `${params.namespace}:${params.path}`;
    if(id === ID_ENTITLEMENTS) {
        return Response.json({
            error: "Cannot modify entitlements"
        }, {
            status: 403
        });
    }

    const rawInput = await request.text();

    // verify that the data parses correctly
    if(rawInput.length === 0 || !isJsonString(rawInput)) {
        return Response.json({
            error: "Malformed input"
        }, {
            status: 400
        });
    }

    const user = await prisma.minecraftUser.findUnique({
        where: {
            id: params.uuid
        },
        include: {
            entitlements: {
                select: {
                    id: true
                }
            }
        }
    });

    if(user === null) {
        return Response.json({
            error: "User not found"
        }, {
            status: 404
        });
    }

    if(user.entitlements.find(e => e.id === id) === undefined) {
        return Response.json({
            error: `User does not have entitlement ${id}`
        }, {
            status: 403
        });
    }

    await prisma.userConfigData.upsert({
        where: {
            minecraftUserId_entitlementId: {
                minecraftUserId: params.uuid,
                entitlementId: id
            }
        },
        update: {
            value: rawInput
        },
        create: {
            entitlementId: id,
            minecraftUserId: params.uuid,
            value: rawInput,
        }
    });

    return Response.json({
        success: true
    });
};