import { call } from '$lib/utils/service';

export function createProjectTree() {
	/** @param {any} node */
	function normalizeNode(node) {
		if (!node || typeof node !== 'object') return node;
		if (node.name == null && node.label != null) node.name = node.label;
		if (node.label == null && node.name != null) node.label = node.name;
		return node;
	}

	/** @param {any} children */
	function normalizeChildren(children) {
		if (!Array.isArray(children)) return [];
		return children.map((child) => normalizeNode(child));
	}

	/** @type {any} */
	let rootNode = $state({
		id: 'ROOT',
		name: '',
		children: []
	});

	async function loadRoot() {
		const tree = await call('studio.treeview.Get', {});
		rootNode.children = normalizeChildren(tree?.children);
	}

	async function addProject(project) {
		if (!rootNode.children.some((child) => child.id === project)) {
			rootNode.children.push({ id: project, name: project, children: true });
			await checkChildren(rootNode);
		}
	}

	async function checkChildren(node = rootNode) {
		let toUpdate = {};
		if (node.children && !Array.isArray(node.children)) {
			toUpdate[node.id] = node;
		} else if (Array.isArray(node.children)) {
			for (let child of node.children) {
				if (child.children && !Array.isArray(child.children)) {
					toUpdate[child.id] = child;
				}
			}
		}
		const ids = Object.keys(toUpdate);
		if (ids.length > 0) {
			const updates = await call('studio.treeview.Get', {
				ids: JSON.stringify(ids)
			});
			for (let id in updates) {
				if (toUpdate[id]) {
					toUpdate[id].children = normalizeChildren(updates[id]);
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
		onExpandedChange
	};
}
