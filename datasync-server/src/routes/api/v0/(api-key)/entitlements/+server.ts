import { prisma } from '$lib/server/db';
import { isvalidNamespace, isvalidPath } from '$lib/util';
import type { RequestHandler } from './$types';

interface AddEntitlementInput {
	namespace: string;
	path: string;
}

export const PUT: RequestHandler = async ({ request }) => {
	const json = await request.json();
	const input = json as AddEntitlementInput;

	if (!input || !isvalidNamespace(input.namespace) || !isvalidPath(input.path)) {
		return Response.json(
			{
				error: 'Malformed input'
			},
			{
				status: 400
			}
		);
	}

	const id = `${input.namespace}:${input.path}`;

	await prisma.entitlement.upsert({
		where: {
			id
		},
		create: {
			id
		},
		update: {
			updatedAt: new Date()
		}
	});

	return Response.json({
		id
	});
};
