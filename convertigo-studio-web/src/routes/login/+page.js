/** @type {import('./$types').PageLoad} */
export function load({ url }) {
	return { redirect: url.searchParams.get('redirect') };
}
