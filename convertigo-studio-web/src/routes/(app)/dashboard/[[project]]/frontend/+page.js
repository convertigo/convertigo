import { redirect } from '@sveltejs/kit';
import { building } from '$app/environment';
import { resolve } from '$app/paths';
import Last from './Last.svelte';

/** @type {import('./$types').PageLoad} */
export function load({ params }) {
	redirect(
		302,
		resolve('/(app)/dashboard/[[project]]/frontend/[model]', {
			...params,
			model: building ? '_' : `${Last.model}_${Last.orientation}`
		})
	);
}
