import { call } from '$lib/utils/service';

/**
 * @typedef {{ equivalentIds?: (id: string | undefined) => string[] }} ProjectTreeOptions
 */

/**
 * @param {any} node
 * @param {any=} previous
 * @param {ProjectTreeOptions=} options
 */
function normalizeProjectTreeNode(node, previous, options = {}) {
	if (!node || typeof node !== 'object') return node;
	if (
		previous &&
		typeof previous === 'object' &&
		shareProjectTreeId(previous.id, node.id, options)
	) {
		const previousChildren = Array.isArray(previous.children) ? previous.children : undefined;
		Object.assign(previous, node);
		if (Array.isArray(node.children)) {
			previous.children = normalizeProjectTreeChildren(node.children, previousChildren, options);
		} else if (node.children && previousChildren) {
			previous.children = previousChildren;
		}
		node = previous;
	} else if (Array.isArray(node.children)) {
		node.children = normalizeProjectTreeChildren(node.children, undefined, options);
	}
	if (node.name == null && node.label != null) node.name = node.label;
	if (node.label == null && node.name != null) node.label = node.name;
	return node;
}

/**
 * @param {any} children
 * @param {any[]=} previousChildren
 * @param {ProjectTreeOptions=} options
 */
function normalizeProjectTreeChildren(children, previousChildren = [], options = {}) {
	if (!Array.isArray(children)) return [];
	previousChildren = Array.isArray(previousChildren) ? previousChildren : [];
	const previousById = Object.create(null);
	for (const child of previousChildren) {
		for (const id of projectTreeEquivalentIds(child?.id, options)) {
			previousById[id] = child;
		}
	}
	return children.map((child) =>
		normalizeProjectTreeNode(
			child,
			findPreviousProjectTreeNode(child, previousById, options),
			options
		)
	);
}

/**
 * Applies one authoritative children response without turning a transient
 * transport/service error into an empty branch. An empty array is a valid
 * server answer; a missing/non-array payload is not.
 *
 * @param {any} node
 * @param {any} children
 * @param {ProjectTreeOptions=} options
 * @returns {boolean}
 */
function applyProjectTreeChildren(node, children, options = {}) {
	if (!node || !Array.isArray(children)) {
		return false;
	}
	node.children = normalizeProjectTreeChildren(children, node.children, options);
	return true;
}

/**
 * @param {any} child
 * @param {Record<string, any>} previousById
 * @param {ProjectTreeOptions} options
 * @returns {any}
 */
function findPreviousProjectTreeNode(child, previousById, options) {
	for (const id of projectTreeEquivalentIds(child?.id, options)) {
		if (previousById[id]) {
			return previousById[id];
		}
	}
	return undefined;
}

/**
 * @param {string | undefined} left
 * @param {string | undefined} right
 * @param {ProjectTreeOptions} options
 * @returns {boolean}
 */
function shareProjectTreeId(left, right, options) {
	if (!left || !right) {
		return false;
	}
	return projectTreeEquivalentIds(left, options).some((id) =>
		projectTreeEquivalentIds(right, options).includes(id)
	);
}

/**
 * @param {string | undefined} id
 * @param {ProjectTreeOptions} options
 * @returns {string[]}
 */
function projectTreeEquivalentIds(id, options) {
	const ids = [];
	for (const value of [id, ...(options.equivalentIds?.(id) ?? [])]) {
		if (value && !ids.includes(value)) {
			ids.push(value);
		}
	}
	return ids;
}

/**
 * @param {ProjectTreeOptions=} options
 */
export function createProjectTree(options = {}) {
	/** @type {any} */
	let rootNode = $state({
		id: 'ROOT',
		name: '',
		children: []
	});

	async function loadRoot() {
		const tree = await call('studio.treeview.Get', {});
		applyProjectTreeChildren(rootNode, tree?.children, options);
	}

	async function addProject(project) {
		if (!rootNode.children.some((child) => child.id === project)) {
			rootNode.children.push({ id: project, name: project, children: true });
			await checkChildren(rootNode);
		}
	}

	async function checkChildren(node = rootNode, force = false) {
		let toUpdate = {};
		if (force && node?.id) {
			toUpdate[node.id] = node;
		} else if (node.children && !Array.isArray(node.children)) {
			toUpdate[node.id] = node;
		} else if (Array.isArray(node.children)) {
			for (let child of node.children) {
				if (child.children && !Array.isArray(child.children)) {
					toUpdate[child.id] = child;
				}
			}
		}
		await updateChildren(toUpdate);
	}

	/**
	 * @param {any[]} nodes
	 * @param {boolean=} force
	 */
	async function checkNodes(nodes = [], force = false) {
		let toUpdate = {};
		for (const node of nodes) {
			if (!node?.id) {
				continue;
			}
			if (force || (node.children && !Array.isArray(node.children))) {
				toUpdate[node.id] = node;
			}
		}
		await updateChildren(toUpdate);
	}

	/**
	 * @param {Record<string, any>} toUpdate
	 */
	async function updateChildren(toUpdate) {
		const ids = Object.keys(toUpdate);
		if (ids.length > 0) {
			if (ids.length === 1) {
				const id = ids[0];
				const update = await call('studio.treeview.Get', { id });
				applyProjectTreeChildren(toUpdate[id], update?.children, options);
			} else {
				const updates = await call('studio.treeview.Get', {
					ids: JSON.stringify(ids)
				});
				for (let id in updates ?? {}) {
					if (toUpdate[id]) {
						applyProjectTreeChildren(toUpdate[id], updates[id], options);
					}
				}
			}
		}
	}

	function onExpandedChange({ expandedNodes }) {
		for (let node of expandedNodes) {
			checkChildren(node);
		}
	}

	return {
		get rootNode() {
			return rootNode;
		},
		loadRoot,
		addProject,
		checkChildren,
		checkNodes,
		onExpandedChange
	};
}

export { applyProjectTreeChildren, normalizeProjectTreeChildren, normalizeProjectTreeNode };
