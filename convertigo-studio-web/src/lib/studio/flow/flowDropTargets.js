import { areEquivalentDboObjectIds, isDescendantObjectId, objectNameFromId } from '$lib/studio/dnd';

const DEFAULT_NODE_SIZE = { width: 150, height: 72 };

/**
 * @typedef {{
 *  point: { x: number, y: number },
 *  flow: import('./types').Flow | null,
 *  xyNodes: import('@xyflow/svelte').Node[],
 *  sourceId?: string,
 *  selectedSequenceId?: string,
 *  nodeSize?: { width: number, height: number }
 * }} LaneDropInput
 */

/**
 * @param {LaneDropInput} input
 * @returns {{
 *  target: string,
 *  position: 'before' | 'after',
 *  indicator: 'before' | 'after',
 *  targetNodeId: string,
 *  fallbackTarget?: string,
 *  fallbackPosition?: 'inside',
 *  dropBranch?: string,
 *  dropHostLabel?: string
 * } | null}
 */
function findFlowLaneDropTarget(input) {
	const { point, flow, xyNodes, sourceId = '', selectedSequenceId = '' } = input;
	const nodeSize = input.nodeSize ?? DEFAULT_NODE_SIZE;
	if (!flow) {
		return null;
	}
	const lanes = collectVisibleSiblingLanes(flow, xyNodes, sourceId, selectedSequenceId);
	let best =
		/** @type {{ lane: { parentId: string, branch: string, nodes: import('@xyflow/svelte').Node[] }, yDistance: number, xDistance: number, inBand: boolean } | null} */ (
			null
		);
	for (const lane of lanes) {
		const rank = rankFlowLaneAtPoint(point, lane.nodes, nodeSize);
		if (!rank) {
			continue;
		}
		const depth = flowLaneDepth(lane.parentId, selectedSequenceId);
		if (
			!best ||
			(rank.inBand && !best.inBand) ||
			(rank.inBand === best.inBand &&
				(rank.yDistance < best.yDistance ||
					(rank.yDistance === best.yDistance &&
						(rank.xDistance < best.xDistance ||
							(rank.xDistance === best.xDistance &&
								depth > flowLaneDepth(best.lane.parentId, selectedSequenceId))))))
		) {
			best = {
				lane,
				yDistance: rank.yDistance,
				xDistance: rank.xDistance,
				inBand: rank.inBand
			};
		}
	}
	if (!best) {
		return null;
	}
	const lane = [...best.lane.nodes].sort(
		(left, right) => nodeCenter(left, nodeSize).x - nodeCenter(right, nodeSize).x
	);
	const firstNode = lane[0];
	if (!firstNode) {
		return null;
	}
	for (let index = 0; index < lane.length; index += 1) {
		const node = lane[index];
		if (point.x >= nodeCenter(node, nodeSize).x) {
			continue;
		}
		return {
			target: xyFlowObjectId(node),
			position: 'before',
			indicator: 'before',
			targetNodeId: node.id,
			fallbackTarget: best.lane.parentId,
			fallbackPosition: 'inside',
			dropBranch: best.lane.branch,
			dropHostLabel: dropHostLabel(best.lane.parentId, selectedSequenceId)
		};
	}
	const previousNode = lane.at(-1) ?? firstNode;
	return {
		target: xyFlowObjectId(previousNode),
		position: 'after',
		indicator: 'after',
		targetNodeId: previousNode.id,
		fallbackTarget: best.lane.parentId,
		fallbackPosition: 'inside',
		dropBranch: best.lane.branch,
		dropHostLabel: dropHostLabel(best.lane.parentId, selectedSequenceId)
	};
}

/**
 * @param {string} parentId
 * @param {string} selectedSequenceId
 * @returns {number}
 */
function flowLaneDepth(parentId, selectedSequenceId) {
	if (!parentId || parentId === selectedSequenceId) {
		return 0;
	}
	return parentId.split(/[.:/]/).filter(Boolean).length;
}

/**
 * @param {import('./types').Flow} flow
 * @param {import('@xyflow/svelte').Node[]} xyNodes
 * @param {string} sourceId
 * @param {string} selectedSequenceId
 * @returns {{ parentId: string, branch: string, nodes: import('@xyflow/svelte').Node[] }[]}
 */
function collectVisibleSiblingLanes(flow, xyNodes, sourceId, selectedSequenceId) {
	const groups = new Map();
	for (const xyNode of xyNodes) {
		const objectId = xyFlowObjectId(xyNode);
		if (!objectId || isDraggedObjectOrDescendant(sourceId, objectId)) {
			continue;
		}
		const flowNode = flow.nodes.find((node) => node.id === xyNode.id);
		if (!flowNode) {
			continue;
		}
		const parentId = flowParentId(flowNode, selectedSequenceId);
		const branch = flowParentBranch(flowNode);
		const key = `${parentId}\u0000${branch}`;
		const group = groups.get(key) ?? { parentId, branch, nodes: [] };
		group.nodes.push(xyNode);
		groups.set(key, group);
	}
	return Array.from(groups.values()).filter((group) => group.nodes.length > 0);
}

/**
 * @param {string} sourceId
 * @param {string} candidateId
 * @returns {boolean}
 */
function isDraggedObjectOrDescendant(sourceId, candidateId) {
	return Boolean(
		sourceId &&
		candidateId &&
		(areEquivalentDboObjectIds(candidateId, sourceId) ||
			isDescendantObjectId(candidateId, sourceId))
	);
}

/**
 * @param {{ x: number, y: number }} point
 * @param {import('@xyflow/svelte').Node[]} lane
 * @param {{ width: number, height: number }} nodeSize
 * @returns {{ yDistance: number, xDistance: number, inBand: boolean } | null}
 */
function rankFlowLaneAtPoint(point, lane, nodeSize) {
	if (!lane.length) {
		return null;
	}
	const centers = lane.map((node) => nodeCenter(node, nodeSize));
	const minY = Math.min(...centers.map((center) => center.y));
	const maxY = Math.max(...centers.map((center) => center.y));
	const tolerance = nodeSize.height * 0.78;
	const inBand = point.y >= minY - tolerance && point.y <= maxY + tolerance;
	const yDistance = inBand
		? Math.abs(point.y - averageXyNodeY(lane, nodeSize))
		: Math.min(Math.abs(point.y - minY), Math.abs(point.y - maxY));
	if (!inBand && yDistance > nodeSize.height * 1.1) {
		return null;
	}
	return {
		yDistance,
		xDistance: Math.min(...centers.map((center) => Math.abs(point.x - center.x))),
		inBand
	};
}

/**
 * @param {import('@xyflow/svelte').Node[]} lane
 * @param {{ width: number, height: number }} nodeSize
 * @returns {number}
 */
function averageXyNodeY(lane, nodeSize) {
	return lane.reduce((sum, node) => sum + nodeCenter(node, nodeSize).y, 0) / lane.length;
}

/**
 * @param {import('@xyflow/svelte').Node} node
 * @param {{ width: number, height: number }} nodeSize
 * @returns {{ x: number, y: number }}
 */
function nodeCenter(node, nodeSize = DEFAULT_NODE_SIZE) {
	return {
		x: node.position.x + nodeSize.width / 2,
		y: node.position.y + nodeSize.height / 2
	};
}

/**
 * @param {import('./types').FlowNode} node
 * @param {string} selectedSequenceId
 * @returns {string}
 */
function flowParentId(node, selectedSequenceId) {
	return typeof node.data?.parentId === 'string' && node.data.parentId
		? node.data.parentId
		: selectedSequenceId;
}

/**
 * @param {import('./types').FlowNode} node
 * @returns {string}
 */
function flowParentBranch(node) {
	return typeof node.data?.parentBranch === 'string' ? node.data.parentBranch : '';
}

/**
 * @param {import('@xyflow/svelte').Node} node
 * @returns {string}
 */
function xyFlowObjectId(node) {
	return typeof node.data?.originalId === 'string' && node.data.originalId
		? node.data.originalId
		: '';
}

/**
 * @param {string | undefined} parentId
 * @param {string} selectedSequenceId
 * @returns {string}
 */
function dropHostLabel(parentId, selectedSequenceId) {
	if (!parentId || parentId === selectedSequenceId) {
		return '';
	}
	return objectNameFromId(parentId);
}

export { findFlowLaneDropTarget };
