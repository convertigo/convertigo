import { SvelteSet } from 'svelte/reactivity';

/**
 * The objects selected together in the tree, as the tree of the Eclipse Studio selects several of them
 * with Ctrl or ⌘ and Shift: copy, cut, delete, enable and disable run on all of them.
 */
export const treeSelection = $state({ ids: /** @type {string[]} */ ([]) });

/**
 * @param {string} id the object to add to the selection, or to remove from it
 * @param {string} primary the object selected before, which joins the selection
 */
export function toggleTreeSelection(id, primary) {
	const ids = treeSelection.ids.length
		? [...new SvelteSet(treeSelection.ids)]
		: primary
			? [primary]
			: [];
	treeSelection.ids = ids.includes(id) ? ids.filter((selected) => selected !== id) : [...ids, id];
}

/**
 * Selects the objects shown between two of them, in the order of the tree.
 * @param {string} from
 * @param {string} to
 */
export function selectTreeRange(from, to) {
	const shown = [...document.querySelectorAll('.studio-tree-node__content[data-node-id]')].map(
		(element) => String(/** @type {HTMLElement} */ (element).dataset.nodeId ?? '')
	);
	const start = shown.indexOf(from);
	const end = shown.indexOf(to);
	treeSelection.ids =
		start < 0 || end < 0
			? [to]
			: [
					...new SvelteSet(
						shown.slice(Math.min(start, end), Math.max(start, end) + 1).filter(Boolean)
					)
				];
}

export function clearTreeSelection() {
	if (treeSelection.ids.length) {
		treeSelection.ids = [];
	}
}

/**
 * @param {string} id an object an action runs on
 * @returns {string[]} the objects the action runs on: the selection when the object is part of it
 */
export function treeSelectionOf(id) {
	return treeSelection.ids.length > 1 && treeSelection.ids.includes(id)
		? [...new SvelteSet(treeSelection.ids)]
		: [id];
}
