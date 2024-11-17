export function isJsonString(str: string): boolean {
	try {
		JSON.parse(str);
	} catch {
		return false;
	}
	return true;
}

export function isvalidNamespace(str: string): boolean {
	return /^[a-z0-9_.-]+$/.test(str);
}

export function isvalidPath(str: string): boolean {
	return /^[a-z0-9/._-]+$/.test(str);
}
