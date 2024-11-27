import { browser } from '$app/environment';

/** @type {import('./$types').PageLoad} */
export function load({ url }) {
	return { redirect: browser ? url.searchParams.get('redirect') : '' };
}
