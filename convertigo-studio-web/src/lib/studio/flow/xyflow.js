/**
 * @typedef {import('./types').Flow} Flow
 * @typedef {import('./types').FlowLink} FlowLink
 * @typedef {import('./types').FlowNode} FlowNode
 * @typedef {import('./types').FlowStepNodeData} FlowStepNodeData
 * @typedef {FlowLink & { synthetic?: boolean }} VisibleFlowLink
 * @typedef {Object} ToXyFlowOptions
 * @property {Set<string>=} hiddenNodeIds
 * @property {Set<string>=} collapsedNodeIds
 * @property {Map<string, number>=} descendantCountByNodeId
 * @property {(function(string, boolean): void)=} onToggleSubsteps
 * @property {string=} selectedObjectId
 * @property {string=} dropTargetNodeId
 * @property {boolean=} dropDenied
 * @property {'inside' | 'before' | 'after'=} dropPosition
 * @property {string=} dropBranch
 * @property {string=} dropHostLabel
 * @property {string=} renameObjectId
 * @property {(function(string, string): void)=} onRenameObject
 * @property {(function(string): void)=} onRequestRenameObject
 * @property {(function(string): void)=} onDeleteObject
 */

import { areEquivalentDboObjectIds } from '$lib/studio/dnd';
import { branchLabelForStep } from './flowStepLabels';

const NODE_WIDTH = 150;
const NODE_HEIGHT = 72;
const EDGE_BRANCH_LABELS = new Set(['Then', 'Else', 'Next', 'Loop', 'Done']);

/**
 * @param {Flow} flow
 * @param {ToXyFlowOptions} [options]
 * @returns {{ nodes: import('@xyflow/svelte').Node<FlowStepNodeData, 'flow-step'>[], edges: import('@xyflow/svelte').Edge[] }}
 */
function toXyFlow(flow, options = {}) {
	const hiddenNodeIds = options.hiddenNodeIds ?? new Set();
	const visibleNodeIds = new Set(
		flow.nodes.filter((node) => !hiddenNodeIds.has(node.id)).map((node) => node.id)
	);
	const visibleNodes = flow.nodes.filter((node) => visibleNodeIds.has(node.id));
	const visibleLinks = collectVisibleLinks(flow, visibleNodeIds, hiddenNodeIds);
	const positions = layoutVisibleFlow(visibleNodes);
	/** @type {import('@xyflow/svelte').Node<FlowStepNodeData, 'flow-step'>[]} */
	const nodes = visibleNodes.map((node) => ({
		id: node.id,
		type: 'flow-step',
		position: positions.get(node.id) ?? { x: node.x, y: node.y },
		data: toStepNodeData(node, options),
		draggable: false,
		dragHandle: '.flow-step-node__drag-handle',
		selectable: true,
		selected: isSelectedFlowNode(node, options.selectedObjectId),
		style: nodeStyle()
	}));
	const terminalFlow = createTerminalFlow(flow, visibleNodes, visibleLinks, positions);
	nodes.unshift(...terminalFlow.nodes.filter((node) => node.data.terminalKind === 'request'));
	nodes.push(...terminalFlow.nodes.filter((node) => node.data.terminalKind === 'response'));
	const visibleNodeById = new Map(visibleNodes.map((node) => [node.id, node]));
	/** @type {import('@xyflow/svelte').Edge[]} */
	const edges = visibleLinks.map((link) => toXyEdge(link, visibleNodeById));
	edges.unshift(...terminalFlow.edges.filter((edge) => edge.source.includes('__request')));
	edges.push(...terminalFlow.edges.filter((edge) => edge.target.includes('__response')));
	return { nodes, edges };
}

/**
 * @param {VisibleFlowLink} link
 * @param {Map<string, FlowNode>} nodeById
 * @returns {import('@xyflow/svelte').Edge}
 */
function toXyEdge(link, nodeById) {
	const label = edgeBranchLabel(link, nodeById);
	return {
		id: link.id,
		source: link.from.nodeId,
		target: link.to.nodeId,
		sourceHandle: outputHandleId(link.from.portIndex),
		targetHandle: inputHandleId(link.to.portIndex),
		type: edgeType(link),
		animated: false,
		selectable: false,
		style: link.synthetic ? 'stroke-dasharray: 6 4;' : void 0,
		zIndex: 0,
		label: label || void 0
	};
}

/**
 * @param {VisibleFlowLink} link
 * @returns {string}
 */
function edgeType(link) {
	if (link.routing === 'orthogonal') {
		return 'step';
	}
	return 'smoothstep';
}

/**
 * @param {VisibleFlowLink} link
 * @param {Map<string, FlowNode>} nodeById
 * @returns {string}
 */
function edgeBranchLabel(link, nodeById) {
	const sourceNode = nodeById.get(link.from.nodeId);
	const label = branchLabelForStep(sourceNode?.outputLabels?.[link.from.portIndex] ?? '', {
		type: sourceNode?.type,
		classname:
			typeof sourceNode?.data?.classname === 'string' ? sourceNode.data.classname : undefined,
		hasElseBranch: sourceNode?.data?.hasElseBranch === true
	});
	return EDGE_BRANCH_LABELS.has(label) ? label.toUpperCase() : '';
}

/**
 * @param {Flow} flow
 * @param {FlowNode[]} visibleNodes
 * @param {VisibleFlowLink[]} visibleLinks
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {{ nodes: import('@xyflow/svelte').Node<FlowStepNodeData, 'flow-step'>[], edges: import('@xyflow/svelte').Edge[] }}
 */
function createTerminalFlow(flow, visibleNodes, visibleLinks, positions) {
	if (!visibleNodes.length) {
		return { nodes: [], edges: [] };
	}
	const positioned = visibleNodes.map((node) => ({
		node,
		position: positions.get(node.id) ?? { x: node.x, y: node.y }
	}));
	const minX = Math.min(...positioned.map(({ position }) => position.x));
	const incoming = new Set(visibleLinks.map((link) => link.to.nodeId));
	const outgoing = new Set(visibleLinks.map((link) => link.from.nodeId));
	const roots = visibleNodes.filter((node) => !incoming.has(node.id));
	const tails = visibleNodes.filter((node) => !outgoing.has(node.id));
	const nonReturnRoots = roots.filter((node) => !isReturnNode(node));
	const rootNodes = nonReturnRoots.length
		? nonReturnRoots
		: roots.length
			? roots
			: [leftmostNode(positioned)];
	const fallbackTailNodes = tails.length ? tails : [rightmostNode(positioned)];
	const sideOutgoing = collectSideOutgoingNodeIds(visibleNodes, visibleLinks);
	const bottomDescendants = collectBottomDescendantNodeIds(visibleNodes, visibleLinks);
	const returnNodes = visibleNodes.filter((node) => isReturnNode(node));
	const responsePrimaryNodes = visibleNodes.filter(
		(node) => !sideOutgoing.has(node.id) && !bottomDescendants.has(node.id) && !isReturnNode(node)
	);
	const responseNodes = uniqueNodes(
		(responsePrimaryNodes.length ? responsePrimaryNodes : fallbackTailNodes).concat(returnNodes)
	);
	const responsePositionNodes = responsePrimaryNodes.length
		? responsePrimaryNodes
		: responseNodes.length
			? responseNodes
			: fallbackTailNodes;
	const requestId = `${flow.id}.__request`;
	const responseId = `${flow.id}.__response`;
	const requestNode = createTerminalNode(
		requestId,
		'request',
		{ x: minX - NODE_WIDTH - 110, y: averageNodeY(rootNodes, positions) },
		0,
		1
	);
	const responseNode = createTerminalNode(
		responseId,
		'response',
		{
			x: maxNodeX(responsePositionNodes, positions) + NODE_WIDTH + 110,
			y: averageNodeY(responsePositionNodes, positions)
		},
		1,
		0
	);
	const edges = [
		...rootNodes.map((node) =>
			createTerminalEdge(
				`${requestId}->${node.id}`,
				requestId,
				node.id,
				outputHandleId(0),
				firstInputHandle(node)
			)
		),
		...responseNodes.map((node) =>
			createTerminalEdge(
				`${node.id}->${responseId}`,
				node.id,
				responseId,
				responseOutputHandle(node),
				inputHandleId(0)
			)
		)
	];
	return { nodes: [requestNode, responseNode], edges };
}

/**
 * @param {string} id
 * @param {'request' | 'response'} kind
 * @param {{ x: number, y: number }} position
 * @param {number} inputs
 * @param {number} outputs
 * @returns {import('@xyflow/svelte').Node<FlowStepNodeData, 'flow-step'>}
 */
function createTerminalNode(id, kind, position, inputs, outputs) {
	return {
		id,
		type: 'flow-step',
		position,
		data: {
			id,
			label: kind,
			name: kind,
			type: kind === 'request' ? 'Request' : 'Response',
			color: kind === 'request' ? '#22c55e' : '#f59e0b',
			inputs,
			outputs,
			inputLabels: [],
			outputLabels: [],
			bottomInputs: 0,
			bottomOutputs: 0,
			isFlowTerminal: true,
			terminalKind: kind
		},
		draggable: false,
		selectable: false,
		style: nodeStyle()
	};
}

/**
 * @param {string} id
 * @param {string} source
 * @param {string} target
 * @param {string | undefined} sourceHandle
 * @param {string | undefined} targetHandle
 * @returns {import('@xyflow/svelte').Edge}
 */
function createTerminalEdge(id, source, target, sourceHandle, targetHandle) {
	/** @type {import('@xyflow/svelte').Edge} */
	const edge = {
		id,
		source,
		target,
		type: 'smoothstep',
		animated: false,
		selectable: false,
		style: 'stroke-dasharray: 7 5; opacity: 0.8;',
		zIndex: 0
	};
	if (sourceHandle) {
		edge.sourceHandle = sourceHandle;
	}
	if (targetHandle) {
		edge.targetHandle = targetHandle;
	}
	return edge;
}

/**
 * @returns {string}
 */
function nodeStyle() {
	return `width: ${NODE_WIDTH}px; height: ${NODE_HEIGHT}px;`;
}

/**
 * @param {FlowNode[]} nodes
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function averageNodeY(nodes, positions) {
	return Math.round(
		nodes.reduce((sum, node) => sum + (positions.get(node.id)?.y ?? node.y ?? 0), 0) / nodes.length
	);
}

/**
 * @param {{ node: FlowNode, position: { x: number, y: number }}[]} positioned
 * @returns {FlowNode}
 */
function leftmostNode(positioned) {
	return positioned.reduce((left, current) =>
		current.position.x < left.position.x ? current : left
	).node;
}

/**
 * @param {{ node: FlowNode, position: { x: number, y: number }}[]} positioned
 * @returns {FlowNode}
 */
function rightmostNode(positioned) {
	return positioned.reduce((right, current) =>
		current.position.x > right.position.x ? current : right
	).node;
}

/**
 * @param {FlowNode} node
 * @returns {string | undefined}
 */
function lastOutputHandle(node) {
	const outputs = node.outputs ?? 0;
	return outputs > 0 ? outputHandleId(outputs - 1) : undefined;
}

/**
 * @param {FlowNode} node
 * @returns {string | undefined}
 */
function firstInputHandle(node) {
	const inputs = node.inputs ?? 0;
	return inputs > 0 ? inputHandleId(0) : undefined;
}

/**
 * @param {FlowNode[]} nodes
 * @param {VisibleFlowLink[]} links
 * @returns {Set<string>}
 */
function collectSideOutgoingNodeIds(nodes, links) {
	const nodeById = new Map(nodes.map((node) => [node.id, node]));
	const sideOutgoing = new Set();
	for (const link of links) {
		const sourceNode = nodeById.get(link.from.nodeId);
		if (sourceNode && !isBottomOutputPort(sourceNode, link.from.portIndex)) {
			sideOutgoing.add(sourceNode.id);
		}
	}
	return sideOutgoing;
}

/**
 * @param {FlowNode[]} nodes
 * @param {VisibleFlowLink[]} links
 * @returns {Set<string>}
 */
function collectBottomDescendantNodeIds(nodes, links) {
	const nodeById = new Map(nodes.map((node) => [node.id, node]));
	const outgoing = new Map();
	for (const link of links) {
		const list = outgoing.get(link.from.nodeId) ?? [];
		list.push(link);
		outgoing.set(link.from.nodeId, list);
	}
	const descendants = new Set();
	for (const link of links) {
		const parentNode = nodeById.get(link.from.nodeId);
		if (!parentNode || !isBottomOutputPort(parentNode, link.from.portIndex)) {
			continue;
		}
		const parentId = parentNode.id;
		const queue = [link.to.nodeId];
		const visited = new Set();
		while (queue.length) {
			const nodeId = queue.shift();
			if (!nodeId || nodeId === parentId || visited.has(nodeId)) {
				continue;
			}
			visited.add(nodeId);
			descendants.add(nodeId);
			for (const nextLink of outgoing.get(nodeId) ?? []) {
				if (nextLink.to.nodeId !== parentId) {
					queue.push(nextLink.to.nodeId);
				}
			}
		}
	}
	return descendants;
}

/**
 * @param {FlowNode} node
 * @returns {boolean}
 */
function isBottomOutputPort(node, portIndex) {
	const outputs = node.outputs ?? 0;
	const bottomOutputs = node.bottomOutputs ?? 0;
	return Boolean(bottomOutputs && outputs && portIndex >= outputs - bottomOutputs);
}

/**
 * @param {FlowNode} node
 * @returns {string | undefined}
 */
function responseOutputHandle(node) {
	return lastSideOutputHandle(node) ?? lastOutputHandle(node);
}

/**
 * @param {FlowNode} node
 * @returns {string | undefined}
 */
function lastSideOutputHandle(node) {
	const outputs = node.outputs ?? 0;
	const bottomOutputs = node.bottomOutputs ?? 0;
	const sideOutputs = Math.max(0, outputs - bottomOutputs);
	return sideOutputs > 0 ? outputHandleId(sideOutputs - 1) : undefined;
}

/**
 * @param {FlowNode[]} nodes
 * @returns {FlowNode[]}
 */
function uniqueNodes(nodes) {
	const seen = new Set();
	return nodes.filter((node) => {
		if (seen.has(node.id)) {
			return false;
		}
		seen.add(node.id);
		return true;
	});
}

/**
 * @param {FlowNode[]} nodes
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function maxNodeX(nodes, positions) {
	if (!nodes.length) {
		return 0;
	}
	return Math.max(...nodes.map((node) => positions.get(node.id)?.x ?? node.x ?? 0));
}

/**
 * @param {FlowNode} node
 * @returns {boolean}
 */
function isReturnNode(node) {
	if (node.data && typeof node.data.isReturn === 'boolean') {
		return node.data.isReturn;
	}
	const classname = typeof node.data?.classname === 'string' ? node.data.classname : '';
	return simpleTypeName(classname || node.type).toLowerCase() === 'returnstep';
}

/**
 * @param {string} value
 * @returns {string}
 */
function simpleTypeName(value) {
	const parts = value.split('.');
	return parts[parts.length - 1] || value;
}

/**
 * @param {Flow} flow
 * @param {Set<string>} visibleNodeIds
 * @param {Set<string>} hiddenNodeIds
 * @returns {VisibleFlowLink[]}
 */
function collectVisibleLinks(flow, visibleNodeIds, hiddenNodeIds) {
	/** @type {VisibleFlowLink[]} */
	const links = [];
	const linkKeys = new Set();
	/** @type {Map<string, FlowLink[]>} */
	const outgoing = new Map();
	for (const link of flow.links) {
		const list = outgoing.get(link.from.nodeId) ?? [];
		list.push(link);
		outgoing.set(link.from.nodeId, list);
	}
	/**
	 * @param {VisibleFlowLink} link
	 */
	const addLink = (link) => {
		if (link.from.nodeId === link.to.nodeId) {
			return;
		}
		const key = `${link.from.nodeId}:${link.from.portIndex}->${link.to.nodeId}:${link.to.portIndex}`;
		if (linkKeys.has(key)) {
			return;
		}
		linkKeys.add(key);
		links.push(link);
	};
	for (const link of flow.links) {
		if (visibleNodeIds.has(link.from.nodeId) && visibleNodeIds.has(link.to.nodeId)) {
			addLink(link);
		}
	}
	for (const link of flow.links) {
		if (!visibleNodeIds.has(link.from.nodeId) || !hiddenNodeIds.has(link.to.nodeId)) {
			continue;
		}
		const visitedHidden = new Set();
		const queue = [link.to.nodeId];
		while (queue.length) {
			const hiddenNodeId = queue.shift();
			if (!hiddenNodeId || visitedHidden.has(hiddenNodeId)) {
				continue;
			}
			visitedHidden.add(hiddenNodeId);
			for (const nextLink of outgoing.get(hiddenNodeId) ?? []) {
				if (visibleNodeIds.has(nextLink.to.nodeId)) {
					addLink({
						id: `collapsed_${link.id}_${nextLink.id}`,
						from: { nodeId: link.from.nodeId, portIndex: link.from.portIndex },
						to: { nodeId: nextLink.to.nodeId, portIndex: nextLink.to.portIndex },
						routing: 'orthogonal',
						synthetic: true
					});
					continue;
				}
				if (hiddenNodeIds.has(nextLink.to.nodeId)) {
					queue.push(nextLink.to.nodeId);
				}
			}
		}
	}
	return links;
}

/**
 * @param {FlowNode[]} nodes
 * @returns {Map<string, { x: number, y: number }>}
 */
function layoutVisibleFlow(nodes) {
	/** @type {Map<string, { x: number, y: number }>} */
	const positions = new Map();
	if (!nodes.length) {
		return positions;
	}
	const positionedNodes = nodes.filter(
		(node) => Number.isFinite(node.x) && Number.isFinite(node.y)
	);
	if (positionedNodes.length !== nodes.length) {
		nodes.forEach((node, index) => {
			positions.set(node.id, {
				x: 40 + index * (NODE_WIDTH + 110),
				y: 40
			});
		});
		return positions;
	}
	const minX = Math.min(...nodes.map((node) => node.x));
	const minY = Math.min(...nodes.map((node) => node.y));
	for (const node of nodes) {
		positions.set(node.id, {
			x: Math.round(node.x - minX + 40),
			y: Math.round(node.y - minY + 40)
		});
	}
	return positions;
}

/**
 * @param {number} index
 * @returns {string}
 */
function inputHandleId(index) {
	return `input-${index}`;
}

/**
 * @param {number} index
 * @returns {string}
 */
function outputHandleId(index) {
	return `output-${index}`;
}

/**
 * @param {FlowNode} node
 * @param {string | undefined} selectedObjectId
 * @returns {boolean}
 */
function isSelectedFlowNode(node, selectedObjectId) {
	if (!selectedObjectId) {
		return false;
	}
	return (
		areEquivalentDboObjectIds(node.id, selectedObjectId) ||
		(typeof node.data?.originalId === 'string' &&
			areEquivalentDboObjectIds(node.data.originalId, selectedObjectId))
	);
}

/**
 * @param {FlowNode} node
 * @param {ToXyFlowOptions} options
 * @returns {FlowStepNodeData}
 */
function toStepNodeData(node, options) {
	const data = node.data ?? {};
	const inputs = node.inputs ?? 0;
	const outputs = node.outputs ?? 0;
	const substepDescendantCount = options.descendantCountByNodeId?.get(node.id) ?? 0;
	return {
		id: node.id,
		label: node.label,
		name: node.name ?? node.label,
		type: node.type,
		color: node.color,
		icon: typeof data.icon === 'string' ? data.icon : void 0,
		classname: typeof data.classname === 'string' ? data.classname : void 0,
		group: node.group,
		isLoop: Boolean(data.isLoop),
		isReturn: Boolean(data.isReturn),
		isBreak: Boolean(data.isBreak),
		isXml: Boolean(data.isXml),
		isSourceContainer: Boolean(data.isSourceContainer),
		hasChildren: Boolean(data.hasChildren),
		hasThenBranch: data.hasThenBranch === true,
		hasElseBranch: data.hasElseBranch === true,
		inputs,
		outputs,
		inputLabels: node.inputLabels ?? [],
		outputLabels: node.outputLabels ?? [],
		bottomInputs: node.bottomInputs ?? 0,
		bottomOutputs: node.bottomOutputs ?? 0,
		isSubstepCollapsed: options.collapsedNodeIds?.has(node.id) ?? false,
		substepDescendantCount,
		onToggleSubsteps: substepDescendantCount ? options.onToggleSubsteps : void 0,
		originalId: typeof data.originalId === 'string' ? data.originalId : void 0,
		isSelected: isSelectedFlowNode(node, options.selectedObjectId),
		isDropTarget: Boolean(options.dropTargetNodeId && node.id === options.dropTargetNodeId),
		isDropDenied: Boolean(
			options.dropDenied && options.dropTargetNodeId && node.id === options.dropTargetNodeId
		),
		dropPosition:
			options.dropTargetNodeId && node.id === options.dropTargetNodeId
				? options.dropPosition
				: undefined,
		dropBranch:
			options.dropTargetNodeId && node.id === options.dropTargetNodeId
				? options.dropBranch
				: undefined,
		dropHostLabel:
			options.dropTargetNodeId && node.id === options.dropTargetNodeId
				? options.dropHostLabel
				: undefined,
		isRenaming: isSelectedFlowNode(node, options.renameObjectId),
		parentBranch: typeof data.parentBranch === 'string' ? data.parentBranch : void 0,
		onRename: options.onRenameObject,
		onRequestRename: options.onRequestRenameObject,
		onDelete: options.onDeleteObject
	};
}
export { inputHandleId, outputHandleId, toXyFlow };
