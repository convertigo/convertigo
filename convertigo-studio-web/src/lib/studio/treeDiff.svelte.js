import { call } from '$lib/utils/service';
import { expandableDboAncestorIds } from './dnd';

/**
 * The Git mode of the tree: the objects of the projects changed since a commit, added, modified, renamed,
 * moved, or removed and shown where they were.
 *
 * @typedef {{ status: 'added' | 'modified' | 'removed', id: string, name: string, type?: string,
 *  classname?: string, parentId?: string, objectParentId?: string, oldName?: string, moved?: boolean,
 *  properties?: { name: string, label: string, old: string, new: string }[] }} TreeChange
 * @typedef {{ loading: boolean, error: string, repository: boolean, commit: any,
 *  changes: TreeChange[], byId: Record<string, TreeChange>, ghosts: Record<string, TreeChange[]>,
 *  ghostsByObject: Record<string, TreeChange[]>, ancestors: Record<string, boolean>,
 *  counts: { added: number, modified: number, removed: number } }} ProjectDiff
 */

export const treeDiff = $state({
	enabled: false,
	changedOnly: false,
	/** the commit compared to, a branch or an id */
	ref: 'HEAD',
	/** @type {Record<string, ProjectDiff>} */
	projects: {}
});

/** @type {Record<string, number>} */
const serials = {};
/** @type {ReturnType<typeof setTimeout> | undefined} */
let refreshTimer;

/**
 * @param {string} id the id of a node of the tree
 * @returns {string} its project
 */
export function projectOfNode(id) {
	return String(id ?? '').match(/^[^.:/~]+/)?.[0] ?? '';
}

/**
 * @returns {ProjectDiff}
 */
function emptyDiff() {
	return {
		loading: false,
		error: '',
		repository: true,
		commit: null,
		changes: [],
		byId: {},
		ghosts: {},
		ghostsByObject: {},
		ancestors: {},
		counts: { added: 0, modified: 0, removed: 0 }
	};
}

/**
 * Indexes the changes of a project for the nodes of the tree.
 * @param {any} result the response of the TreeDiff service
 * @returns {ProjectDiff}
 */
export function indexTreeDiff(result) {
	const diff = emptyDiff();
	diff.repository = result?.repository !== false;
	diff.commit = result?.commit ?? null;
	diff.changes = Array.isArray(result?.changes) ? result.changes : [];
	for (const change of diff.changes) {
		diff.counts[change.status] = (diff.counts[change.status] ?? 0) + 1;
		let shownId = change.id;
		if (change.status === 'removed') {
			const parentId = change.parentId ?? '';
			(diff.ghosts[parentId] ??= []).push(change);
			if (change.objectParentId) {
				(diff.ghostsByObject[change.objectParentId] ??= []).push(change);
			}
			// the nodes down to where it was contain a change
			diff.ancestors[parentId] = true;
			if (change.objectParentId) {
				diff.ancestors[change.objectParentId] = true;
			}
			shownId = parentId;
		} else {
			diff.byId[change.id] = change;
		}
		for (const ancestor of expandableDboAncestorIds(shownId)) {
			if (ancestor !== change.id) {
				diff.ancestors[ancestor] = true;
			}
		}
	}
	return diff;
}

/**
 * Compares a project to the commit of the mode.
 * @param {string} projectName
 */
export async function loadTreeDiff(projectName) {
	if (!treeDiff.enabled || !projectName) {
		return;
	}
	const serial = (serials[projectName] ?? 0) + 1;
	serials[projectName] = serial;
	const current = treeDiff.projects[projectName];
	treeDiff.projects[projectName] = { ...(current ?? emptyDiff()), loading: true, error: '' };
	try {
		const result = await call('studio.git.TreeDiff', { projectName, ref: treeDiff.ref });
		if (serials[projectName] !== serial || !treeDiff.enabled) {
			return;
		}
		treeDiff.projects[projectName] = indexTreeDiff(result);
	} catch (error) {
		if (serials[projectName] !== serial) {
			return;
		}
		treeDiff.projects[projectName] = {
			...emptyDiff(),
			error: String(error instanceof Error ? error.message : error)
		};
	}
}

/**
 * The projects compared are compared again, a moment after a change.
 */
export function scheduleTreeDiffRefresh() {
	if (!treeDiff.enabled) {
		return;
	}
	clearTimeout(refreshTimer);
	refreshTimer = setTimeout(() => {
		for (const projectName of Object.keys(treeDiff.projects)) {
			void loadTreeDiff(projectName);
		}
	}, 600);
}

/**
 * @param {boolean} enabled
 */
export function setTreeDiffEnabled(enabled) {
	treeDiff.enabled = enabled;
	if (!enabled) {
		treeDiff.projects = {};
		clearTimeout(refreshTimer);
	}
}

/**
 * Compares the projects to another commit.
 * @param {string} ref
 */
export function setTreeDiffRef(ref) {
	treeDiff.ref = ref || 'HEAD';
	const names = Object.keys(treeDiff.projects);
	treeDiff.projects = {};
	for (const projectName of names) {
		void loadTreeDiff(projectName);
	}
}

/**
 * @param {string} id the id of a node
 * @returns {{ diff: ProjectDiff | undefined, change: TreeChange | undefined, ancestor: boolean }}
 */
export function nodeDiff(id) {
	const diff = treeDiff.enabled ? treeDiff.projects[projectOfNode(id)] : undefined;
	return { diff, change: diff?.byId[id], ancestor: Boolean(diff?.ancestors[id]) };
}

/**
 * Gives back to an object the value a property had at the commit of the mode.
 * @param {string} id the id of the object in the tree
 * @param {string} property its property, beanData.name for a property of Ionic
 * @returns {Promise<boolean>} whether it is done
 */
export async function revertTreeProperty(id, property) {
	const result = await call('studio.git.TreeDiff', {
		projectName: projectOfNode(id),
		ref: treeDiff.ref,
		action: 'revert',
		id,
		property
	});
	return result?.done === true;
}
