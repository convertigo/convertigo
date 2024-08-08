import { call, checkArray } from '$lib/utils/service';
import { decode } from 'html-entities';
import { writable } from 'svelte/store';

export const projectsStore = writable(/** @type {any[]} */ ([]));

let init = false;

export async function projectsCheck(refresh = false) {
	if (!init || refresh) {
		init = true;
		const res = await call('projects.List');
		const projects = checkArray(res?.admin?.projects?.project);
		for (const project of projects) {
			project['@_comment'] = decode(project['@_comment']);
			project.ref = checkArray(project.ref);
		}
		projectsStore.set(projects);
	}
}
