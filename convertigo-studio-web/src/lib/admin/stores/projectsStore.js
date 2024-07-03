import { call, checkArray } from '$lib/utils/service';
import { writable } from 'svelte/store';

export const projectsStore = writable(/** @type {any[]} */ ([]));

let init = false;

export async function projectsCheck(refresh = false) {
	if (!init || refresh) {
		init = true;
		const res = await call('projects.List');
		projectsStore.set(checkArray(res?.admin?.projects?.project));
	}
}
