import { redirect } from '@sveltejs/kit';
import Last from './Last.svelte';
import { resolveRoute } from '$app/paths';
import { building } from '$app/environment';

export function load() {
	redirect(302, resolveRoute('/(app)/admin/logs/[tab]', { tab: building ? '_' : Last.tab }));
}
