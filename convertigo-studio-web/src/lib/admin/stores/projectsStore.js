import { call } from '$lib/utils/service';
import { getModalStore } from '@skeletonlabs/skeleton';
import { get, writable } from 'svelte/store';

export const projectsStore = writable([]);

let init = false;

export async function projectsCheck() {
	if (!init) {
		init = true;
		const response = await call('projects.List');
		console.log('project.list response:', response);
		if (response?.admin?.projects) {
			if (!Array.isArray(response.admin.projects.project)) {
				response.admin.projects.project = [response.admin.projects.project];
			}
			projectsStore.set(response.admin.projects.project);
			console.log(get(projectsStore));
		}
	}
}

export async function deleteProject(projectName) {
	if (!confirm(`Do you really want to delete the project '${projectName}'?`)) {
		return;
	}

	try {
		const response = await call('projects.Delete', { projectName });

		if (response) {
			projectsStore.update((projects) => {
				return projects.filter((project) => project['@_name'] !== projectName);
			});
		} else {
			alert('There was an error deleting the project.');
		}
	} catch (error) {
		alert(`An error occurred`);
	}
}

export async function reloadProject(projectName) {
	if (
		!confirm(
			`Do you really want to reload the project '${projectName}'? All unsaved changes will be lost.`
		)
	) {
		return;
	}

	try {
		const response = await call('projects.Reload', { projectName });
		console.log(response);
		if (response) {
			alert(`The project '${projectName}' has been successfully reloaded.`);
		} else {
			alert('There was an error reloading the project.');
		}
	} catch (error) {
		alert(`An error occurred`);
	}
}
