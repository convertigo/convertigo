import { call } from '$lib/utils/service';
import { expandableDboAncestorIds } from './dnd';

/**
 * The merges of projects stopped on conflicts, merged object by object: the conflicts to resolve, and the
 * objects merged from their side.
 *
 * @typedef {{ id: string, kind: string, description?: string, key?: string, name: string, type?: string,
 *  objectId?: string, parentId?: string, objectParentId?: string, property?: string, label?: string,
 *  base?: string, mine?: string, theirs?: string, editable?: boolean, choices: string[],
 *  resolution?: string, value?: string, path?: string }} MergeConflict
 * @typedef {{ status: 'added' | 'modified' | 'removed' | 'moved', origin: string, key: string, name: string,
 *  type?: string, objectId?: string, parentId?: string, objectParentId?: string, properties?: string[] }} MergeChange
 * @typedef {{ loading: boolean, error: string, merging: boolean, theirs: string, unresolved: number,
 *  conflicts: MergeConflict[], changes: MergeChange[], conflictsById: Record<string, MergeConflict[]>,
 *  changesById: Record<string, MergeChange>, ghosts: Record<string, (MergeConflict | MergeChange)[]>,
 *  ancestors: Record<string, boolean> }} ProjectMerge
 */

export const treeMerge = $state({
	/** @type {Record<string, ProjectMerge>} */
	projects: {}
});

/**
 * Indexes a merge for the nodes of the tree: the conflicts and the changes of the objects it shows, and
 * those of the objects it does not, added by them or removed by me, shown where they go.
 * @param {any} result the response of the TreeMerge service
 * @returns {ProjectMerge}
 */
export function indexTreeMerge(result) {
	/** @type {ProjectMerge} */
	const merge = {
		loading: false,
		error: '',
		merging: result?.merging === true,
		theirs: String(result?.theirs ?? ''),
		unresolved: Number(result?.unresolved) || 0,
		conflicts: Array.isArray(result?.conflicts) ? result.conflicts : [],
		changes: Array.isArray(result?.changes) ? result.changes : [],
		conflictsById: {},
		changesById: {},
		ghosts: {},
		ancestors: {}
	};
	/** @param {string} id */
	const holds = (id) => {
		for (const ancestor of expandableDboAncestorIds(id)) {
			if (ancestor !== id) {
				merge.ancestors[ancestor] = true;
			}
		}
	};
	for (const item of [...merge.conflicts, ...merge.changes]) {
		const isConflict = 'kind' in item;
		if (item.objectId && !(!isConflict && item.status === 'added')) {
			if (isConflict) {
				(merge.conflictsById[item.objectId] ??= []).push(/** @type {MergeConflict} */ (item));
			} else {
				merge.changesById[item.objectId] = /** @type {MergeChange} */ (item);
			}
			holds(item.objectId);
		} else if (item.parentId) {
			(merge.ghosts[item.parentId] ??= []).push(item);
			merge.ancestors[item.parentId] = true;
			holds(item.parentId);
		}
	}
	return merge;
}

/**
 * @param {string} projectName
 * @param {Record<string, string>} [parameters] an action of the merge
 * @returns {Promise<any>} the response of the service
 */
export async function loadTreeMerge(projectName, parameters = {}) {
	const current = treeMerge.projects[projectName];
	if (current) {
		treeMerge.projects[projectName] = { ...current, loading: true, error: '' };
	}
	try {
		const result = await call('studio.git.TreeMerge', { projectName, ...parameters });
		if (result?.merging) {
			treeMerge.projects[projectName] = indexTreeMerge(result);
		} else {
			delete treeMerge.projects[projectName];
		}
		return result;
	} catch (error) {
		if (treeMerge.projects[projectName]) {
			treeMerge.projects[projectName] = {
				...treeMerge.projects[projectName],
				loading: false,
				error: String(error instanceof Error ? error.message : error)
			};
		}
		return null;
	}
}

/**
 * @param {string} projectName
 * @param {string} id the conflict
 * @param {string} choice mine, theirs, both, edit, or clear to choose again
 * @param {string} [value] the value edited
 */
export function resolveConflict(projectName, id, choice, value) {
	return loadTreeMerge(projectName, {
		action: 'resolve',
		id,
		choice,
		...(value === undefined ? {} : { value })
	});
}

/**
 * @param {string} projectName
 * @param {string} choice mine or theirs for all the conflicts not resolved yet
 */
export function resolveAllConflicts(projectName, choice) {
	return loadTreeMerge(projectName, { action: 'resolveAll', choice });
}

/**
 * @param {string} id the id of a node
 * @returns {ProjectMerge | undefined} the merge of its project, when one is stopped on conflicts
 */
export function mergeOfNode(id) {
	const projectName = String(id ?? '').match(/^[^.:/~]+/)?.[0] ?? '';
	return treeMerge.projects[projectName];
}
