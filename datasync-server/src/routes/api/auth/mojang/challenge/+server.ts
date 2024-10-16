import { AUTH_EXPIRY } from "$lib/server/auth";
import { prisma } from "$lib/server/db";
import type { UUID } from "crypto";
import type { RequestHandler } from "./$types";

interface AuthChallengeInput {
    id: UUID;
}

export const POST: RequestHandler = async ({ request }) => {
    const json = await request.json();
    const input = json as AuthChallengeInput;
    if (!input || !input.id) {
        return Response.json({
            error: "Malformed input"
        }, {
            status: 400
        });
    }

    await prisma.authChallenge.deleteMany({
        where: {
            expiresAt: {
                lt: new Date()
            },
        }
    });

    const challenge = await prisma.authChallenge.create({
        data: {
            userId: input.id,
            expiresAt: new Date(Date.now() + AUTH_EXPIRY),
        },
        select: {
            token: true,
            userId: true,
            expiresAt: true
        }
    });

    return Response.json({
        token: challenge.token,
        user_id: challenge.userId,
        expires_in: challenge.expiresAt.getTime() - Date.now(),
    });
};