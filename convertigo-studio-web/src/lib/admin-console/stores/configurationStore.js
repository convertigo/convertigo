import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let configurations = writable(/** @type {any} */ {});

export async function refreshConfigurations() {
	configurations.set(await call('configuration.List', {}));
}
