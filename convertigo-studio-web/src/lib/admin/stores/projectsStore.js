import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export const projectsStore = writable([]);

let init = false;

export async function projectsCheck(forceUpdate = false) {
	if (!init || forceUpdate) {
		init = true; // Set to true to prevent multiple initializations unless forced.
		const response = await call('projects.List');
		console.log(response);
		if (response && response.admin && response.admin.projects && response.admin.projects.project) {
			projectsStore.set(
				Array.isArray(response.admin.projects.project)
					? response.admin.projects.project
					: [response.admin.projects.project]
			);
		}
	}
}
