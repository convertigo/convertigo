import { redirect } from '@sveltejs/kit';
import { building } from '$app/environment';
import { resolve } from '$app/paths';
import Last from './Last.svelte';

export function load() {
	redirect(302, resolve('/(app)/admin/logs/[tab]', { tab: building ? '_' : Last.tab }));
}
