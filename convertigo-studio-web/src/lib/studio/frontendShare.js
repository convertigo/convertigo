/**
 * The address of a frontend as another device reaches it: its path on the origin the Studio is served from, absolute.
 * The development server is served through the gateway of this Convertigo (a capability path, no session needed) and
 * the production application by this Convertigo, while the server only knows its own, maybe loopback, origin.
 * @param {unknown} value
 */
export function publicFrontendUrl(value) {
	const candidate = String(value ?? '');
	if (!candidate || typeof window === 'undefined') {
		return candidate;
	}
	try {
		const url = new URL(candidate, window.location.href);
		return new URL(`${url.pathname}${url.search}${url.hash}`, window.location.origin).href;
	} catch {
		return candidate;
	}
}

/**
 * Whether an address only reaches the machine that opened it (a phone cannot use it).
 * @param {unknown} value
 */
export function isLocalAddress(value) {
	try {
		return /^(localhost|127\.|0\.0\.0\.0$|\[::1\]$)/.test(new URL(String(value)).hostname);
	} catch {
		return false;
	}
}
