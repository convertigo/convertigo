import { call } from '$lib/utils/service';
import { get, writable } from 'svelte/store';

export const projectsStore = writable([]);

let init = false;

export async function projectsCheck() {
	if (!init) {
		init = true;
		const response = await call('projects.List');
		if (response?.admin?.projects) {
			if (!Array.isArray(response.admin.projects.project)) {
				response.admin.projects.project = [response.admin.projects.project];
			}
			projectsStore.set(response.admin.projects.project);
		}
	}
}
