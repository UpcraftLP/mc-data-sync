import { prisma } from '$lib/server/db.js';
import type { UUID } from 'crypto';

interface RemoveUserEntitlementsInput {
    uuid: UUID;
    entitlements: string[];
}

export async function POST({ request }) {
    const json = await request.json();
    const input = json as RemoveUserEntitlementsInput;
    if (!input) {
        return Response.json({
            error: "Malformed input"
        }, {
            status: 400
        });
    }

    if (input.entitlements.length === 0) {
        return Response.json({
            error: "No entitlements provided"
        }, {
            status: 400
        });
    }

    console.log(`removing entitlements for user ${input.uuid}: [${input.entitlements.join(',')}]`);

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
    if (user === null) {
        return Response.json({
            message: "User not found"
        }, {
            status: 404
        });
    }

    await prisma.minecraftUser.update({
        where: {
            id: input.uuid,
        },
        data: {
            entitlements: {
                connect: user.entitlements.filter(e => !input.entitlements.includes(e.id)).map(e => ({ id: e.id }))
            }
        },
    });

    return Response.json({
        success: true
    });
}