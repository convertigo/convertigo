import { call } from '$lib/utils/service';
import { loadStepPalette } from './palette';
import { SequenceFlowBuilder } from './SequenceFlowBuilder';

/**
 * @typedef {import('./types').Flow} Flow
 * @typedef {import('./types').SequenceTreeNode} SequenceTreeNode
 * @typedef {import('./types').TreeviewItem} TreeviewItem
 * @typedef {import('./types').TreeviewResponse} TreeviewResponse
 */

const builder = new SequenceFlowBuilder();

/**
 * @param {string} projectName
 * @param {string} sequenceName
 * @returns {Promise<Flow>}
 */
async function loadSequenceFlow(projectName, sequenceName) {
	const sequenceId = `${projectName}.sq:${sequenceName}`;
	const palette = await loadStepPalette(sequenceId);
	const children = await fetchSequenceChildren(sequenceId, new Set());
	return builder.buildFlowFromTree(projectName, sequenceName, children, palette);
}

/**
 * @returns {string[]}
 */
function getLoopStepIds() {
	return builder.getLoopStepIds();
}

/**
 * @param {string} id
 * @param {Set<string>} visited
 * @returns {Promise<SequenceTreeNode[]>}
 */
async function fetchSequenceChildren(id, visited) {
	if (!id || visited.has(id)) {
		return [];
	}
	visited.add(id);
	/** @type {TreeviewResponse & { isError?: boolean, error?: unknown }} */
	const response = await call('studio.treeview.Get', { id, flow: true });
	if (response?.isError) {
		throw new Error(String(response.error ?? 'Unable to load flow tree'));
	}
	const children = normalizeTreeChildren(response?.children);
	return hydrateChildren(children, visited);
}

/**
 * @param {TreeviewItem[] | undefined} children
 * @returns {TreeviewItem[]}
 */
function normalizeTreeChildren(children) {
	return Array.isArray(children) ? children : [];
}

/**
 * @param {TreeviewItem[]} children
 * @param {Set<string>} visited
 * @returns {Promise<SequenceTreeNode[]>}
 */
async function hydrateChildren(children, visited) {
	const filtered = children.filter((child) => !shouldSkipTreeviewItem(child));
	return Promise.all(filtered.map((child) => hydrateNode(child, visited)));
}

/**
 * @param {TreeviewItem} item
 * @param {Set<string>} visited
 * @returns {Promise<SequenceTreeNode>}
 */
async function hydrateNode(item, visited) {
	const base = normalizeTreeItem(item);
	if (!base.hasChildren) {
		return { ...base, children: [] };
	}
	const directChildren = Array.isArray(item.children) ? item.children : void 0;
	if (directChildren?.length) {
		return {
			...base,
			children: await hydrateChildren(directChildren, visited)
		};
	}
	return {
		...base,
		children: await fetchSequenceChildren(base.id, visited)
	};
}

/**
 * @param {TreeviewItem} item
 * @returns {Omit<SequenceTreeNode, 'children'>}
 */
function normalizeTreeItem(item) {
	const label = (item.label || item.name || item.id || '').trim();
	const hasChildren =
		item.children === true || (Array.isArray(item.children) && item.children.length > 0);
	return {
		id: item.id,
		label: label || item.id,
		name: item.name,
		icon: item.icon,
		classname: item.classname,
		isLoop: !!item.isLoop,
		isXml: !!item.isXml,
		isSourceContainer: !!item.isSourceContainer,
		hasChildren
	};
}

/**
 * @param {TreeviewItem} item
 * @returns {boolean}
 */
function shouldSkipTreeviewItem(item) {
	const classname = item.classname?.toLowerCase() || '';
	if (classname.includes('stepvariable') || classname.includes('testcase')) {
		return true;
	}
	return (item.id || '').includes(':tc');
}
export { getLoopStepIds, loadSequenceFlow };
