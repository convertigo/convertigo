import { call } from '$lib/utils/service';

/**
 * The projects closed in the workspace, as the Eclipse Studio closes one: they stay in the tree, without
 * loading, until they are opened again. The list of the projects of the engine leaves them out.
 */
export const closedProjects = $state({ names: /** @type {string[]} */ ([]) });

/**
 * @param {string} projectName
 * @returns {boolean}
 */
export function isProjectClosed(projectName) {
	return Boolean(projectName) && closedProjects.names.includes(projectName);
}

export async function refreshClosedProjects() {
	const result = await call('studio.treeview.Get', {});
	const children = Array.isArray(result?.children) ? result.children : [];
	const names = children
		.filter((child) => child?.closed === true && child?.id)
		.map((child) => String(child.id));
	if (names.join('\n') !== closedProjects.names.join('\n')) {
		closedProjects.names = names;
	}
}

/**
 * Closes projects, or opens them again.
 *
 * @param {string[]} projectNames
 * @param {boolean} closed
 * @returns {Promise<{ done: boolean, projects: string[], error?: string }>}
 */
export async function setProjectsClosed(projectNames, closed) {
	const result = await call('studio.project.Close', {
		projects: JSON.stringify(projectNames),
		open: String(!closed)
	});
	await refreshClosedProjects();
	return {
		done: result?.done === true,
		projects: Array.isArray(result?.projects) ? result.projects.map(String) : [],
		error: result?.error ? String(result.error) : undefined
	};
}
