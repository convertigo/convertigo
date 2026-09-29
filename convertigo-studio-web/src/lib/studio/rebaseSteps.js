/**
 * The steps of an interactive rebase, as git rebase -i edits them: the commits replayed, oldest first, each
 * with its action.
 *
 * @typedef {'pick' | 'reword' | 'edit' | 'squash' | 'fixup' | 'drop'} RebaseAction
 * @typedef {{ id: string, subject: string, body?: string, action: RebaseAction, message?: string }} RebaseStep
 */

/** @type {{ value: RebaseAction, label: string, hint: string }[]} */
export const REBASE_ACTIONS = [
	{ value: 'pick', label: 'Pick', hint: 'kept as it is' },
	{ value: 'reword', label: 'Reword', hint: 'kept with a new message' },
	{ value: 'edit', label: 'Edit', hint: 'the rebase stops on it to change it' },
	{ value: 'squash', label: 'Squash', hint: 'merged into the commit before, messages combined' },
	{ value: 'fixup', label: 'Fixup', hint: 'merged into the commit before, its message dropped' },
	{ value: 'drop', label: 'Drop', hint: 'removed' }
];

/**
 * @param {{ id: string, subject: string, body?: string }[]} todo the commits of the rebase, oldest first
 * @returns {RebaseStep[]} each picked
 */
export function initialSteps(todo) {
	return todo.map((commit) => ({
		...commit,
		action: 'pick',
		message: commit.body?.trim() ?? commit.subject
	}));
}

/**
 * @param {RebaseStep[]} steps
 * @param {number} index
 * @param {number} delta -1 to replay the commit earlier, 1 later
 * @returns {RebaseStep[]}
 */
export function moveStep(steps, index, delta) {
	const target = index + delta;
	if (target < 0 || target >= steps.length) {
		return steps;
	}
	const moved = [...steps];
	[moved[index], moved[target]] = [moved[target], moved[index]];
	return moved;
}

/**
 * @param {RebaseStep[]} steps
 * @returns {string} why the steps cannot be replayed, empty when they can
 */
export function stepsError(steps) {
	const kept = steps.filter((step) => step.action !== 'drop');
	if (!kept.length) {
		return 'Keep at least a commit, or reset the branch instead.';
	}
	if (kept[0].action === 'squash' || kept[0].action === 'fixup') {
		return `The first commit kept, ${kept[0].id}, has no commit before it to be merged into.`;
	}
	const reworded = kept.find((step) => step.action === 'reword' && !step.message?.trim());
	if (reworded) {
		return `The commit ${reworded.id} needs its new message.`;
	}
	return '';
}

/**
 * @param {RebaseStep[]} steps
 * @returns {{ id: string, action: RebaseAction, message?: string }[]} as the engine takes them
 */
export function stepsParameter(steps) {
	return steps.map((step) =>
		step.action === 'reword'
			? { id: step.id, action: step.action, message: step.message?.trim() ?? '' }
			: { id: step.id, action: step.action }
	);
}
