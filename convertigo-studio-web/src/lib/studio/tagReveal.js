/**
 * Grouped by tags, an object can show in several tag groups, and so can its ancestors: revealing it
 * opens one occurrence only. It is the occurrence already showing it, else the one already opened
 * the deepest toward it, else the first one in the order of the tree.
 */

/**
 * Tree folders use ids like `Project:sq` or `Project.sq:Seq:st`, while database objects below them
 * use qnames like `Project.sq:Seq.st:Step`.
 * @param {string} nodeId
 * @param {string} target
 * @returns {boolean} whether the node holds the target
 */
function isAncestorId(nodeId, target) {
	if (!nodeId) return false;
	if (
		target.startsWith(`${nodeId}.`) ||
		target.startsWith(`${nodeId}:`) ||
		target.startsWith(`${nodeId}/`)
	) {
		return true;
	}
	const folder = nodeId.match(/^(.*):([^:.]+)$/);
	return Boolean(folder?.[1] && folder[2] && target.startsWith(`${folder[1]}.${folder[2]}:`));
}

/**
 * @param {any[]} nodes the root nodes of the tree, in their order
 * @param {string} target the id of the object to reveal
 * @param {{ isExpanded: (rowId: string) => boolean, equivalentIds?: (id: string) => string[] }} options
 * isExpanded tells whether a row is opened; equivalentIds lists the ids naming the same object
 * @returns {Set<string>} the row ids of the occurrence to open toward the target, from the root
 */
export function tagRevealRows(nodes, target, { isExpanded, equivalentIds = (id) => [id] }) {
	const targets = equivalentIds(target).filter(Boolean);
	/** @type {{ rows: string[], visible: boolean, depth: number }} */
	const best = { rows: [], visible: false, depth: -1 };
	/**
	 * @param {string[]} rows
	 * @param {boolean} visible
	 * @param {number} depth
	 */
	const consider = (rows, visible, depth) => {
		// the first occurrence wins a tie: the walk follows the order of the tree
		if ((visible && !best.visible) || (visible === best.visible && depth > best.depth)) {
			Object.assign(best, { rows, visible, depth });
		}
	};
	/**
	 * @param {any[]} children
	 * @param {string} prefix the row prefix of the occurrence holding the children
	 * @param {string[]} path the rows from the root
	 * @param {number} depth the rows opened from the root on that path
	 * @param {boolean} open whether all the rows of that path are opened
	 */
	const walk = (children, prefix, path, depth, open) => {
		for (const node of children ?? []) {
			const id = String(node?.id ?? '');
			const row = `${prefix}${node?.rowId ?? id}`;
			const reached = !node?.tagGroup && targets.includes(id);
			if (!node?.tagGroup && !reached && !targets.some((t) => isAncestorId(id, t))) continue;
			const rows = [...path, row];
			if (reached) {
				consider(rows, open, depth);
				continue;
			}
			const opened = open && isExpanded(row);
			const next = opened ? depth + 1 : depth;
			if (Array.isArray(node.children) && node.children.length) {
				walk(node.children, node.rowId && !node.tagGroup ? `${row}/` : prefix, rows, next, opened);
			} else if (!node.tagGroup) {
				// its children are not loaded yet: the target is below
				consider(rows, false, next);
			}
		}
	};
	walk(nodes, '', [], 0, true);
	return new Set(best.rows);
}
