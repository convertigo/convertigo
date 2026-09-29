import { call } from '$lib/utils/service';
import { expandableDboAncestorIds } from './dnd';

/**
 * The conflicts of projects, of a merge, a rebase, a cherry-pick, a revert or a stash applied, merged object
 * by object: the conflicts to resolve, and the objects merged from their side.
 *
 * @typedef {{ id: string, kind: string, description?: string, key?: string, name: string, type?: string,
 *  objectId?: string, parentId?: string, objectParentId?: string, property?: string, label?: string,
 *  base?: string, mine?: string, theirs?: string, editable?: boolean, choices: string[],
 *  resolution?: string, value?: string, path?: string,
 *  versions?: { left: string, right: string, root: any, trees?: { left?: any, right?: any } },
 *  blocks?: any[] }} MergeConflict
 * @typedef {{ status: 'added' | 'modified' | 'removed' | 'moved', origin: string, key: string, name: string,
 *  type?: string, objectId?: string, parentId?: string, objectParentId?: string, properties?: string[] }} MergeChange
 * @typedef {{ kind: string, state: string, ours: string, theirs: string, branch?: string, onto?: string,
 *  step?: number, steps?: number, commit?: { id: string, subject: string, author: string },
 *  conflicts: number, projectConflicts: string[], canContinue: boolean, canSkip: boolean,
 *  canAbort: boolean, commitMessage?: string }} GitOperation
 * @typedef {{ loading: boolean, error: string, merging: boolean, kind: string, ours: string, theirs: string,
 *  operation: GitOperation | null, unresolved: number,
 *  conflicts: MergeConflict[], changes: MergeChange[], conflictsById: Record<string, MergeConflict[]>,
 *  changesById: Record<string, MergeChange>, ghosts: Record<string, (MergeConflict | MergeChange)[]>,
 *  ancestors: Record<string, boolean> }} ProjectMerge
 */

export const treeMerge = $state({
	/** @type {Record<string, ProjectMerge>} */
	projects: {},
	/** by project, a serial that changes as the engine loads it again for its merge */
	reloaded: /** @type {Record<string, number>} */ ({})
});

/** @type {ReturnType<typeof setTimeout> | undefined} */
let refreshTimer;

/**
 * The merges shown are read again a moment after a change of the Studio: its changes are mine.
 */
export function scheduleTreeMergeRefresh() {
	if (!Object.keys(treeMerge.projects).length) {
		return;
	}
	clearTimeout(refreshTimer);
	refreshTimer = setTimeout(() => {
		for (const projectName of Object.keys(treeMerge.projects)) {
			void loadTreeMerge(projectName);
		}
	}, 700);
}

/**
 * What the views of Git tell each other: a serial that changes as an operation changes the repository of a
 * project, and the message of the commit that ends a merge.
 */
export const gitEvents = $state({
	serial: 0,
	/** @type {Record<string, string>} */
	messages: {}
});

/**
 * @param {string} projectName
 * @param {string} [message] the message of the commit that ends its merge
 */
export function notifyGitChange(projectName, message) {
	if (message) {
		gitEvents.messages[projectName] = message;
	}
	gitEvents.serial += 1;
}

/**
 * @param {Partial<GitOperation> | null | undefined} operation
 * @returns {{ icon: string, title: string, detail: string, complete: string, done: string, ended: string }}
 *  how the views name an operation stopped: its title, the button that ends the resolution of its conflicts,
 *  what it says once they are resolved, and once it ended
 */
export function describeOperation(operation) {
	const commit = operation?.commit ? `${operation.commit.id} ${operation.commit.subject}` : '';
	switch (operation?.kind) {
		case 'rebase':
			return {
				icon: 'mdi:source-branch-sync',
				title: `Rebasing ${operation.branch || 'the branch'} onto ${operation.onto || 'another'}`,
				detail:
					(operation.steps ? `commit ${operation.step}/${operation.steps}` : '') +
					(commit ? `${operation.steps ? ': ' : ''}${commit}` : ''),
				complete: 'Continue the rebase',
				done: 'Rebase done',
				ended: 'Rebase done'
			};
		case 'cherry-pick':
			return {
				icon: 'mdi:fruit-cherries',
				title: `Cherry-picking ${operation.commit?.id ?? 'a commit'}`,
				detail: operation.commit?.subject ?? '',
				complete: 'Complete the cherry-pick',
				done: 'Commit cherry-picked',
				ended: 'Commit cherry-picked'
			};
		case 'revert':
			return {
				icon: 'mdi:undo-variant',
				title: `Reverting ${operation.commit?.id ?? 'a commit'}`,
				detail: operation.commit?.subject ?? '',
				complete: 'Complete the revert',
				done: 'Commit reverted',
				ended: 'Commit reverted'
			};
		case 'conflicts':
			return {
				icon: 'mdi:alert-circle-outline',
				title: 'Conflicts of the changes applied',
				detail: operation?.theirs ?? '',
				complete: 'Mark as resolved',
				done: 'Conflicts resolved',
				ended: 'Conflicts resolved'
			};
		default:
			return {
				icon: 'mdi:source-merge',
				title: `Merging ${operation?.theirs || 'a branch'} into ${operation?.ours || 'the branch'}`,
				detail: '',
				complete: 'Complete the merge',
				done: 'Merge ready to commit',
				ended: 'Merge committed'
			};
	}
}

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
		kind: String(result?.kind ?? 'merge'),
		ours: String(result?.ours ?? ''),
		theirs: String(result?.theirs ?? ''),
		operation: result?.operation ?? null,
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
		if (result?.reloaded) {
			// the project loaded again as the version its conflicts are against
			treeMerge.reloaded[projectName] = (treeMerge.reloaded[projectName] ?? 0) + 1;
		}
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
