import { redirect } from '@sveltejs/kit';
import Last from './Last.svelte';

export function load() {
	redirect(302, `${Last.category}/`);
}
