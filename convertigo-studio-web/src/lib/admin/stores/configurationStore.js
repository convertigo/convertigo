import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

/** @type {import('svelte/store').Writable<any>} */
export let configurations = writable({});

export async function refreshConfigurations() {
	configurations.set(await call('configuration.List'));
}

/**
 * @param {any} property
 */
export async function updateConfigurations(property) {
	await call('configuration.Update', { '@_xml': true, configuration: { property } });
	await refreshConfigurations();
	console.log(
		'save serv',
		await call('configuration.Update', { '@_xml': true, configuration: { property } })
	);
}
