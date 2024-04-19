import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let jobsStore = writable([]);

export async function schedulerList() {
	const res = await call('scheduler.List');

	let elementArray = res?.admin?.element ?? [];
	if (!Array.isArray(elementArray)) {
		elementArray = [elementArray];
	}

	jobsStore.set(elementArray);
	console.log('scheduler list res:', res);
}
