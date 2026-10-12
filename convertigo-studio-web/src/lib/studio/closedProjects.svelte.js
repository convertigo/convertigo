import { call } from '#lib/utils/service.js';
import { serviceErrorMessage } from '#lib/utils/serviceError.js';

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

/** the requests of the root of the tree just sent, by grouping, which the calls made with them share */
const rootRequests = new Map();

/**
 * The projects of the workspace as the root of the tree lists them. The closed projects and the tree
 * ask for it together, as when the projects are loaded: the calls made in the same turn share one
 * request, a later call sends its own, which tells the changes made since.
 * @param {boolean} [tagsGrouped]
 * @returns {Promise<any>}
 */
export function loadTreeRoot(tagsGrouped = false) {
	const key = Boolean(tagsGrouped);
	let request = rootRequests.get(key);
	if (!request) {
		request = call('studio.treeview.Get', { tagsGrouped: key });
		rootRequests.set(key, request);
		setTimeout(() => rootRequests.delete(key));
	}
	return request;
}

export async function refreshClosedProjects() {
	const result = await loadTreeRoot();
	// a failed request keeps the projects known closed
	if (!Array.isArray(result?.children)) {
		return;
	}
	const names = result.children
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
		error: result?.error ? serviceErrorMessage(result.error) : undefined
	};
}
