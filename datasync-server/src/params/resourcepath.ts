import { isvalidPath } from '$lib/util';

export function match(param: string) {
	return isvalidPath(param);
}
