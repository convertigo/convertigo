/** @type {import('./$types').PageLoad} */
export function load({ url }) {
	console.log('load');
	return { redirect: url.searchParams.get('redirect') };
}
