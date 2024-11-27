import { browser } from '$app/environment';

/** @type {import('./$types').PageLoad} */
export function load({ url }) {
	if (browser) {
		return { redirect: url.searchParams.get('redirect') };
	}
}
