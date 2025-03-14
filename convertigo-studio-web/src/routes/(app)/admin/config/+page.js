import { redirect } from '@sveltejs/kit';
import { building } from '$app/environment';
import { resolveRoute } from '$app/paths';
import Last from './Last.svelte';

export function load() {
	redirect(
		302,
		resolveRoute('/(app)/admin/config/[category]', { category: building ? '_' : Last.category })
	);
}
