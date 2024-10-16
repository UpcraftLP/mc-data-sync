import { prisma } from '$lib/server/db.js';
import type { UUID } from 'crypto';

interface AddUserEntitlementsInput {
    uuid: UUID;
    entitlements: string[];
}

export async function POST({ request }) {
    const json = await request.json();
    const input = json as AddUserEntitlementsInput;
    if (!input) {
        return Response.json({
            error: "Malformed input"
        }, {
            status: 400
        });
    }

    if(input.entitlements.length === 0) {
        return Response.json({
            error: "No entitlements provided"
        }, {
            status: 400
        });
    }

    console.log(`Adding entitlements for user ${input.uuid}: [${input.entitlements.join(',')}]`);

    const user = await prisma.minecraftUser.findUnique({
        where: {
            id: input.uuid
        },
        select: {
            entitlements: {
                select: {
                    id: true
                }
            }
        }
    });

    // short-circuit if user already has all relevant entitlements
    if(user !== null) {
        const userEntitlements = user.entitlements.map(e => e.id);
        const newEntitlements = input.entitlements.filter(e => !userEntitlements.includes(e));
        if(newEntitlements.length === 0) {
            return Response.json({
                message: "User already has all entitlements"
            });
        }
    }

    const entitlements = await prisma.entitlement.findMany({
        where: {
            id: {
                in: input.entitlements
            }
        }
    });

    if(entitlements.length !== input.entitlements.length) {
        return Response.json({
            error: "Invalid entitlements provided",
            not_found: input.entitlements.filter(e => !entitlements.map(e => e.id).includes(e))
        }, {
            status: 400
        });
    }

    // if user does not exist, there are no entitlements to connect to
    if(user === null) {
        await prisma.minecraftUser.create({
            data: {
                id: input.uuid,
                entitlements: {
                    connect: entitlements.map(e => ({ id: e.id }))
                }
            }
        });

        return new Response(JSON.stringify({
            success: true
        }));
    }

    const newEntitlementList = [
        ...entitlements.map(e => e.id),
        ...user.entitlements.map(e => e.id)
    ];

    await prisma.minecraftUser.update({
        where: {
            id: input.uuid,
        },
        data: {
            entitlements: {
                connect: newEntitlementList.map(entitlementId => ({ id: entitlementId }))
            }
        },
    });

    return Response.json({
        success: true
    });
}