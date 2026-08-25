/**
 * Apply a successful source-backed frontend mutation to the already loaded
 * client tree. The server remains authoritative and is queried afterwards, but
 * users see the confirmed insertion/reorder without waiting for a complete
 * virtual-tree reconstruction.
 * @param {any[]} roots
 * @param {import('./dnd').DboDropResult} mutation
 * @param {(left: string | undefined, right: string | undefined) => boolean} idsEqual
 * @returns {boolean}
 */
function applyProjectedTreeMutation(roots, mutation, idsEqual) {
	if (
		!mutation?.done ||
		(!mutation.optimistic && !mutation.selectionSourcePath && !mutation.projectedSourcePath) ||
		!mutation.parentId
	) {
		return false;
	}
	if (mutation.pendingId) {
		removeProjectedTreeNode(roots, mutation.pendingId, idsEqual);
	}
	const parent = findTreeNode(roots, mutation.parentId, idsEqual);
	if (!parent || !Array.isArray(parent.children)) {
		return false;
	}

	let child;
	if (mutation.payload?.type === 'treeData') {
		const source = findTreeEntry(roots, mutation.payload.data?.id, idsEqual);
		if (!source?.node || !source.parent || !Array.isArray(source.parent.children)) {
			return false;
		}
		child = source.node;
		source.parent.children.splice(source.parent.children.indexOf(child), 1);
	} else if (mutation.payload?.type === 'paletteData') {
		child = projectedPaletteNode(mutation);
		if (!child?.id) {
			return false;
		}
		const duplicate = findTreeEntry(roots, child.id, idsEqual);
		if (duplicate?.node && duplicate.parent && Array.isArray(duplicate.parent.children)) {
			duplicate.parent.children.splice(duplicate.parent.children.indexOf(duplicate.node), 1);
		}
	} else {
		return false;
	}

	if (mutation.selectedId) {
		child.id = mutation.selectedId;
	}
	const targetIndex = parent.children.findIndex((node) => idsEqual(node?.id, mutation.target));
	const index =
		mutation.position === 'before'
			? Math.max(0, targetIndex)
			: mutation.position === 'after'
				? targetIndex < 0
					? parent.children.length
					: targetIndex + 1
				: parent.children.length;
	parent.children.splice(index, 0, child);
	return true;
}

/** @param {import('./dnd').DboDropResult} mutation */
function projectedPaletteNode(mutation) {
	const data = mutation.payload?.data ?? {};
	const insert = data.insert ?? {};
	const canContainChildren = Boolean(
		data.canContainChildren ||
		(data.slots && Object.keys(data.slots).length > 0) ||
		(Array.isArray(data.traits) && data.traits.includes('ui.container'))
	);
	const label = String(
		insert.label ?? insert.text ?? data.name ?? mutation.selectionId ?? 'New block'
	);
	return {
		id: mutation.selectedId ?? mutation.id ?? '',
		name: label,
		label,
		icon: data.iconFile16 ?? data.icon ?? 'folder',
		iconify: data.iconify,
		pending: Boolean(mutation.optimistic),
		children: canContainChildren ? true : false
	};
}

/**
 * Remove a locally projected placeholder without waiting for an authoritative
 * tree reload. Used to roll back a refused mutation and to replace an
 * optimistic node with the id returned by the engine.
 * @param {any[]} roots
 * @param {string | undefined} id
 * @param {(left: string | undefined, right: string | undefined) => boolean} idsEqual
 * @returns {boolean}
 */
function removeProjectedTreeNode(roots, id, idsEqual) {
	const entry = findTreeEntry(roots, id, idsEqual);
	if (!entry?.node || !entry.parent || !Array.isArray(entry.parent.children)) {
		return false;
	}
	entry.parent.children.splice(entry.parent.children.indexOf(entry.node), 1);
	return true;
}

/**
 * @param {any[]} nodes
 * @param {string | undefined} id
 * @param {(left: string | undefined, right: string | undefined) => boolean} idsEqual
 * @returns {any}
 */
function findTreeNode(nodes, id, idsEqual) {
	return findTreeEntry(nodes, id, idsEqual)?.node;
}

/**
 * @param {any[]} nodes
 * @param {string | undefined} id
 * @param {(left: string | undefined, right: string | undefined) => boolean} idsEqual
 * @param {any=} parent
 * @returns {{ node: any, parent: any } | undefined}
 */
function findTreeEntry(nodes, id, idsEqual, parent) {
	for (const node of nodes ?? []) {
		if (idsEqual(node?.id, id)) {
			return { node, parent };
		}
		if (Array.isArray(node?.children)) {
			const found = findTreeEntry(node.children, id, idsEqual, node);
			if (found) {
				return found;
			}
		}
	}
	return undefined;
}

export { applyProjectedTreeMutation, removeProjectedTreeNode };
