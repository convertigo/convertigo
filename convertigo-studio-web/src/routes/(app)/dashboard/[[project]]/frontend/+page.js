import { redirect } from '@sveltejs/kit';
import Last from './Last.svelte';
import { resolveRoute } from '$app/paths';
import { building } from '$app/environment';

/** @type {import('./$types').PageLoad} */
export function load({ params }) {
	redirect(
		302,
		resolveRoute('/(app)/dashboard/[[project]]/frontend/[model]', {
			...params,
			model: building
				? '_'
				: `${Last.model.replaceAll(' ', '-')}_${Last.orientation.substring(0, 1)}`
		})
	);
}
