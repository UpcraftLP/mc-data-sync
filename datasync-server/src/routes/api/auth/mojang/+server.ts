import type { UUID } from 'crypto';
import type { RequestHandler } from './$types';
import { prisma } from '$lib/server/db';

interface LoginInput {
	id: UUID;
	username: string;
	token: string;
}

export const POST: RequestHandler = async ({ request }) => {
	const input = (await request.json()) as LoginInput;

	if (!input || !input.id || !input.username || !input.token) {
		return Response.json(
			{
				error: 'Malformed input'
			},
			{
				status: 400
			}
		);
	}

	const shortUUID = input.id.replace(/-/g, '');

	const challenge = await prisma.authChallenge.findUnique({
		where: {
			token: input.token,
			userId: input.id,
			expiresAt: {
				gt: new Date()
			}
		}
	});

	if (challenge === null) {
		return Response.json(
			{
				message: 'invalid or expired challenge'
			},
			{
				status: 401
			}
		);
	}

	const response = await fetch(
		`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${input.username}&serverId=${challenge.token}`,
		{
			headers: {
				Accept: 'application/json'
			}
		}
	);

	if (!response.ok) {
		return Response.json(
			{
				message: 'Mojang API validation failed'
			},
			{
				status: 401
			}
		);
	}

	const { id } = await response.json();

	if (!id || typeof id !== 'string' || id.replace(/-/g, '') !== shortUUID) {
		return Response.json(
			{
				message: 'auth response ID mismatch'
			},
			{
				status: 401
			}
		);
	}

	const session = await prisma.session.create({
		data: {
			expiresAt: new Date(Date.now() + 1000 * 60 * 60 * 1),
			user: {
				connectOrCreate: {
					where: {
						id: input.id
					},
					create: {
						id: input.id
					}
				}
			}
		},
		select: {
			id: true,
			userId: true,
			expiresAt: true
		}
	});

	return Response.json(
		{
			session_token: session.id,
			user_id: session.userId,
			expires_at: session.expiresAt.toISOString()
		},
		{
			status: 201
		}
	);
};
