import { resolve as svelteResolve } from '$app/paths';

export function ensureTrailingSlash(url = '') {
	const raw = String(url ?? '');
	if (!raw || /^[A-Za-z][A-Za-z\d+.-]*:/.test(raw) || raw.startsWith('//')) {
		return raw;
	}

	let hash = '';
	let query = '';
	let pathname = raw;

	const hashIndex = pathname.indexOf('#');
	if (hashIndex !== -1) {
		hash = pathname.slice(hashIndex);
		pathname = pathname.slice(0, hashIndex);
	}

	const queryIndex = pathname.indexOf('?');
	if (queryIndex !== -1) {
		query = pathname.slice(queryIndex);
		pathname = pathname.slice(0, queryIndex);
	}

	if (!pathname || pathname.endsWith('/') || pathname.endsWith('.html')) {
		return `${pathname}${query}${hash}`;
	}

	return `${pathname}/${query}${hash}`;
}

export function resolve(path, params) {
	return ensureTrailingSlash(svelteResolve(path, params));
}
