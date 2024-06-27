// src/lib/stores/testPlatform.js
import { writable } from 'svelte/store';
import { call, checkArray, deepObject } from '$lib/utils/service';

export let testPlatformStore = writable({});
export let connectorsStore = writable([]);
export let transactionStore = writable([]);
export async function checkTestPlatform(projectName) {
	const res = await call('projects.GetTestPlatform', {
		projectName
	});
	const sequencesData = checkArray(res?.admin?.project?.sequence);
	const connectorsData = checkArray(res?.admin?.project?.connector);

	connectorsData.unshift({ '@_name': 'Sequences', transaction: sequencesData });
	console.log('connectors', connectorsData);
	// Process and store connectors data
	//@ts-ignore
	connectorsStore.set(connectorsData.map(deepObject));

	// Update testPlatformStore
	testPlatformStore.update((tp) => {
		tp[projectName] = deepObject(res?.admin?.project ?? {});
		return tp;
	});
}
