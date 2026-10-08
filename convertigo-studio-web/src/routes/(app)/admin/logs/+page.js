import { redirect } from '@sveltejs/kit';
import { resolve } from '#lib/utils/route.js';
import { building } from '$app/env';
import Last from './Last.svelte';

export function load() {
	redirect(302, resolve('/(app)/admin/logs/[tab]', { tab: building ? '_' : Last.tab }));
}
