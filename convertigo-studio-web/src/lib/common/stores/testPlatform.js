import { writable } from 'svelte/store';
import { call, deepObject } from '$lib/utils/service';

export let testPlatformStore = writable({});

export async function checkTestPlatform(projectName) {
	const res = await call('projects.GetTestPlatform', { projectName });
	testPlatformStore.update((tp) => {
		tp[projectName] = deepObject(res?.admin?.project ?? {});
		return tp;
	});
}
