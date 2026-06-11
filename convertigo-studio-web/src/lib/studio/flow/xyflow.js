/**
 * @typedef {import('./types').Flow} Flow
 * @typedef {import('./types').FlowLink} FlowLink
 * @typedef {import('./types').FlowNode} FlowNode
 * @typedef {import('./types').FlowStepNodeData} FlowStepNodeData
 * @typedef {FlowLink & { synthetic?: boolean }} VisibleFlowLink
 * @typedef {Object} TerminalEdgeOptions
 * @property {string=} class
 * @property {string=} style
 * @property {string=} type
 * @property {number=} zIndex
 * @property {Record<string, unknown>=} data
 * @typedef {Object} HorizontalSegment
 * @property {number} start
 * @property {number} end
 * @property {number} y
 * @typedef {Object} VerticalSegment
 * @property {number} x
 * @property {number} start
 * @property {number} end
 * @typedef {ReturnType<typeof createRoutePlanner>} RoutePlanner
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
	const positions = layoutVisibleFlow(visibleNodes, hiddenNodeIds.size > 0);
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
	const routePlanner = createRoutePlanner(visibleNodes, positions);
	const edgeDataByLinkId = createLoopReturnEdgeData(
		visibleLinks,
		visibleNodeById,
		positions,
		routePlanner
	);
	createLoopReturnBusSegments(
		visibleLinks,
		visibleNodeById,
		positions,
		edgeDataByLinkId,
		routePlanner
	);
	for (const [linkId, data] of createLoopBodyEdgeData(
		visibleLinks,
		visibleNodeById,
		positions,
		routePlanner
	)) {
		edgeDataByLinkId.set(linkId, { ...(edgeDataByLinkId.get(linkId) ?? {}), ...data });
	}
	for (const [linkId, data] of createBranchEdgeData(
		visibleLinks,
		visibleNodeById,
		positions,
		routePlanner
	)) {
		edgeDataByLinkId.set(linkId, { ...(edgeDataByLinkId.get(linkId) ?? {}), ...data });
	}
	/** @type {import('@xyflow/svelte').Edge[]} */
	const edges = visibleLinks.map((link) =>
		toXyEdge(link, visibleNodeById, edgeDataByLinkId.get(link.id))
	);
	edges.unshift(...terminalFlow.edges.filter((edge) => edge.source.includes('__request')));
	edges.push(...terminalFlow.edges.filter((edge) => edge.target.includes('__response')));
	return { nodes, edges };
}

/**
 * @param {VisibleFlowLink} link
 * @param {Map<string, FlowNode>} nodeById
 * @param {Record<string, unknown> | undefined} data
 * @returns {import('@xyflow/svelte').Edge}
 */
function toXyEdge(link, nodeById, data) {
	const label = edgeBranchLabel(link, nodeById);
	const targetNode = nodeById.get(link.to.nodeId);
	return {
		id: link.id,
		source: link.from.nodeId,
		target: link.to.nodeId,
		sourceHandle: outputHandleId(link.from.portIndex),
		targetHandle: inputHandleId(targetPortIndex(link, targetNode)),
		type: edgeType(link, nodeById, data),
		animated: false,
		selectable: false,
		style: edgeStyle(link),
		zIndex: 0,
		data,
		label: label || void 0
	};
}

/**
 * @param {VisibleFlowLink} link
 * @param {FlowNode | undefined} targetNode
 * @returns {number}
 */
function targetPortIndex(link, targetNode) {
	if (link.routing === 'loop-return' && targetNode) {
		return loopReturnInputPortIndex(targetNode);
	}
	return link.to.portIndex;
}

/**
 * @param {FlowNode[]} nodes
 * @param {Map<string, { x: number, y: number }>} positions
 */
function createRoutePlanner(nodes, positions) {
	/** @type {HorizontalSegment[]} */
	const horizontalSegments = [];
	/** @type {VerticalSegment[]} */
	const verticalSegments = [];
	/**
	 * @param {number} startX
	 * @param {number} endX
	 * @param {number} y
	 */
	const reserveHorizontal = (startX, endX, y) => {
		const span = normalizeHorizontalSpan(startX, endX);
		horizontalSegments.push({ start: span.start, end: span.end, y });
	};
	/**
	 * @param {number} x
	 * @param {number} startY
	 * @param {number} endY
	 */
	const reserveVertical = (x, startY, endY) => {
		const span = normalizeVerticalSpan(startY, endY);
		verticalSegments.push({ x, start: span.start, end: span.end });
	};
	/**
	 * @param {number} startX
	 * @param {number} endX
	 * @param {number} y
	 * @returns {boolean}
	 */
	const isHorizontalOccupied = (startX, endX, y) =>
		isHorizontalSegmentOccupied(horizontalSegments, startX, endX, y);
	/**
	 * @param {number} x
	 * @param {number} startY
	 * @param {number} endY
	 * @param {Set<string>} ignoredNodeIds
	 * @returns {boolean}
	 */
	const isVerticalBlocked = (x, startY, endY, ignoredNodeIds) =>
		nodes.some((node) =>
			isVerticalSegmentBlocked(node, positions, x, startY, endY, ignoredNodeIds)
		);
	/**
	 * @param {{
	 *  handleX: number,
	 *  startY: number,
	 *  endY: number,
	 *  preferredOffset?: number,
	 *  step?: number,
	 *  maxOffset?: number,
	 *  ignoredNodeIds?: Set<string>
	 * }} options
	 * @returns {number}
	 */
	const nextVerticalLeadOffset = ({
		handleX,
		startY,
		endY,
		preferredOffset = 44,
		step = 34,
		maxOffset = Number.POSITIVE_INFINITY,
		ignoredNodeIds = new Set()
	}) => {
		const span = normalizeVerticalSpan(startY, endY);
		let offset = preferredOffset;
		const limit = Number.isFinite(maxOffset)
			? Math.max(preferredOffset, maxOffset)
			: Number.POSITIVE_INFINITY;
		while (offset <= limit) {
			const x = handleX + offset;
			if (
				!isVerticalSegmentOccupied(verticalSegments, x, span.start, span.end) &&
				!isVerticalBlocked(x, span.start, span.end, ignoredNodeIds)
			) {
				reserveVertical(x, span.start, span.end);
				return offset;
			}
			offset += step;
		}
		reserveVertical(handleX + preferredOffset, span.start, span.end);
		return preferredOffset;
	};
	return {
		horizontalSegments,
		verticalSegments,
		reserveHorizontal,
		reserveVertical,
		isHorizontalOccupied,
		isVerticalBlocked,
		nextVerticalLeadOffset
	};
}

/**
 * @param {VisibleFlowLink[]} links
 * @param {Map<string, FlowNode>} nodeById
 * @param {Map<string, { x: number, y: number }>} positions
 * @param {RoutePlanner} routePlanner
 * @returns {Map<string, Record<string, unknown>>}
 */
function createLoopReturnEdgeData(links, nodeById, positions, routePlanner) {
	const groups = new Map();
	for (const link of links) {
		if (link.routing !== 'loop-return') {
			continue;
		}
		const targetNode = nodeById.get(link.to.nodeId);
		const sourceNode = nodeById.get(link.from.nodeId);
		if (!targetNode || !sourceNode) {
			continue;
		}
		const list = groups.get(targetNode.id) ?? [];
		list.push({ link, sourceNode, targetNode });
		groups.set(targetNode.id, list);
	}
	const groupLayouts = [];
	for (const group of groups.values()) {
		const targetNode = group[0]?.targetNode;
		if (!targetNode) {
			continue;
		}
		group.sort((left, right) => compareNodePosition(left.sourceNode, right.sourceNode, positions));
		const targetPosition = positions.get(targetNode.id) ?? {
			x: targetNode.x ?? 0,
			y: targetNode.y ?? 0
		};
		const subtreeBottom = loopSubtreeBottom(targetNode, nodeById, positions);
		const sourceBottom = Math.max(
			...group.map(({ sourceNode }) => {
				const position = positions.get(sourceNode.id) ?? {
					x: sourceNode.x ?? 0,
					y: sourceNode.y ?? 0
				};
				return position.y + NODE_HEIGHT;
			})
		);
		groupLayouts.push({
			group,
			targetNode,
			targetPosition,
			depth: loopContainmentDepth(targetNode, groups.keys(), nodeById),
			joinX: targetPosition.x - 86,
			laneY: Math.max(subtreeBottom, sourceBottom) + 74
		});
	}
	groupLayouts.sort((left, right) => {
		if (right.depth !== left.depth) {
			return right.depth - left.depth;
		}
		if (Math.abs(left.laneY - right.laneY) > 1e-3) {
			return left.laneY - right.laneY;
		}
		return left.targetPosition.x - right.targetPosition.x;
	});
	const assignedLoopLanes = [];
	const dataByLinkId = new Map();
	for (const layout of groupLayouts) {
		const span = loopReturnHorizontalSpan(layout.group, layout.targetPosition, positions);
		while (
			assignedLoopLanes.some(
				(lane) => spansOverlap(span, lane.span) && Math.abs(layout.laneY - lane.y) < 72
			)
		) {
			layout.laneY += 72;
		}
		assignedLoopLanes.push({ span, y: layout.laneY });
		const { group, joinX, laneY } = layout;
		const busLinkId = rightmostLoopReturnSource(group, positions)?.link.id;
		group.forEach(({ link, sourceNode }) => {
			const sourceLeadOffset = nextLoopReturnSourceLeadOffset(
				sourceNode,
				link,
				positions,
				laneY,
				routePlanner
			);
			dataByLinkId.set(link.id, {
				connectToLoop: link.id === busLinkId,
				joinX,
				laneY,
				sourceLeadOffset
			});
		});
	}
	return dataByLinkId;
}

/**
 * @param {VisibleFlowLink[]} links
 * @param {Map<string, FlowNode>} nodeById
 * @param {Map<string, { x: number, y: number }>} positions
 * @param {RoutePlanner} routePlanner
 * @returns {Map<string, Record<string, unknown>>}
 */
function createLoopBodyEdgeData(links, nodeById, positions, routePlanner) {
	const dataByLinkId = new Map();
	const nodes = Array.from(nodeById.values());
	for (const link of links) {
		if (!isLoopBodyLink(link, nodeById)) {
			continue;
		}
		const sourceNode = nodeById.get(link.from.nodeId);
		const targetNode = nodeById.get(link.to.nodeId);
		if (!sourceNode || !targetNode) {
			continue;
		}
		const sourcePosition = positions.get(sourceNode.id) ?? {
			x: sourceNode.x ?? 0,
			y: sourceNode.y ?? 0
		};
		const targetPosition = positions.get(targetNode.id) ?? {
			x: targetNode.x ?? 0,
			y: targetNode.y ?? 0
		};
		const sourceLeadX = sourcePosition.x + NODE_WIDTH + 36;
		const targetLeadX = targetPosition.x - 28;
		const targetY = targetPosition.y + NODE_HEIGHT / 2;
		const ignoredNodeIds = new Set([sourceNode.id, targetNode.id]);
		const blockers = nodes.filter((node) =>
			isHorizontalSegmentBlocked(node, positions, sourceLeadX, targetLeadX, targetY, ignoredNodeIds)
		);
		if (!blockers.length && !routePlanner.isHorizontalOccupied(sourceLeadX, targetLeadX, targetY)) {
			continue;
		}
		const blockerBottom = blockers.length
			? Math.max(
					...blockers.map((node) => {
						const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
						return position.y + NODE_HEIGHT;
					})
				)
			: targetY;
		let laneY = blockers.length ? blockerBottom + 74 : targetY + 54;
		const minimumLaneY = blockers.length ? blockerBottom + 28 : Math.min(sourcePosition.y, targetY);
		const preferredLaneY = laneY - 36;
		if (
			routePlanner.isHorizontalOccupied(sourceLeadX, targetLeadX, laneY) &&
			preferredLaneY >= minimumLaneY &&
			isLoopBodyLaneFree(
				nodes,
				positions,
				sourceLeadX,
				targetLeadX,
				preferredLaneY,
				ignoredNodeIds,
				routePlanner
			)
		) {
			laneY = preferredLaneY;
		}
		while (
			!isLoopBodyLaneFree(
				nodes,
				positions,
				sourceLeadX,
				targetLeadX,
				laneY,
				ignoredNodeIds,
				routePlanner
			)
		) {
			laneY += 36;
		}
		routePlanner.reserveHorizontal(sourceLeadX, targetLeadX, laneY);
		dataByLinkId.set(link.id, { laneY });
	}
	return dataByLinkId;
}

/**
 * @param {VisibleFlowLink[]} links
 * @param {Map<string, FlowNode>} nodeById
 * @param {Map<string, { x: number, y: number }>} positions
 * @param {RoutePlanner} routePlanner
 * @returns {Map<string, Record<string, unknown>>}
 */
function createBranchEdgeData(links, nodeById, positions, routePlanner) {
	const groups = new Map();
	for (const link of links) {
		if (!isBranchRouteCandidate(link, nodeById)) {
			continue;
		}
		const sourceNode = nodeById.get(link.from.nodeId);
		const targetNode = nodeById.get(link.to.nodeId);
		if (!sourceNode || !targetNode) {
			continue;
		}
		const group = groups.get(sourceNode.id) ?? [];
		group.push({ link, sourceNode, targetNode });
		groups.set(sourceNode.id, group);
	}
	const dataByLinkId = new Map();
	for (const group of groups.values()) {
		if (group.length < 2) {
			continue;
		}
		group.sort(
			(left, right) =>
				branchOutputVisualIndex(left.sourceNode, left.link.from.portIndex) -
					branchOutputVisualIndex(right.sourceNode, right.link.from.portIndex) ||
				compareNodePosition(left.targetNode, right.targetNode, positions)
		);
		for (const { link, sourceNode, targetNode } of group) {
			const sourceHandleX = outputHandleX(sourceNode, link.from.portIndex, positions);
			const sourceHandleY = outputHandleY(sourceNode, link.from.portIndex, positions);
			const targetHandleIndex = targetPortIndex(link, targetNode);
			const targetHandleX = inputHandleX(targetNode, targetHandleIndex, positions);
			const targetHandleY = inputHandleY(targetNode, targetHandleIndex, positions);
			const sourceLeadOffset = routePlanner.nextVerticalLeadOffset({
				handleX: sourceHandleX,
				startY: sourceHandleY,
				endY: targetHandleY,
				preferredOffset:
					36 + Math.min(3, branchOutputVisualIndex(sourceNode, link.from.portIndex)) * 34,
				step: 34,
				maxOffset: Math.max(36, targetHandleX - sourceHandleX - 30),
				ignoredNodeIds: new Set([sourceNode.id, targetNode.id])
			});
			const sourceLeadX = sourceHandleX + sourceLeadOffset;
			routePlanner.reserveHorizontal(sourceHandleX, sourceLeadX, sourceHandleY);
			routePlanner.reserveHorizontal(sourceLeadX, targetHandleX, targetHandleY);
			dataByLinkId.set(link.id, {
				routeKind: 'branch',
				sourceLeadOffset
			});
		}
	}
	return dataByLinkId;
}

/**
 * @param {VisibleFlowLink[]} links
 * @param {Map<string, FlowNode>} nodeById
 * @param {Map<string, { x: number, y: number }>} positions
 * @param {Map<string, Record<string, unknown>>} edgeDataByLinkId
 * @param {RoutePlanner} routePlanner
 * @returns {HorizontalSegment[]}
 */
function createLoopReturnBusSegments(links, nodeById, positions, edgeDataByLinkId, routePlanner) {
	/** @type {HorizontalSegment[]} */
	const segments = [];
	for (const link of links) {
		if (link.routing !== 'loop-return') {
			continue;
		}
		const data = edgeDataByLinkId.get(link.id);
		const sourceNode = nodeById.get(link.from.nodeId);
		if (!sourceNode || data?.connectToLoop !== true) {
			continue;
		}
		const joinX = typeof data.joinX === 'number' ? data.joinX : undefined;
		const laneY = typeof data.laneY === 'number' ? data.laneY : undefined;
		if (joinX === undefined || laneY === undefined) {
			continue;
		}
		const sourceLeadOffset = typeof data.sourceLeadOffset === 'number' ? data.sourceLeadOffset : 44;
		const sourceLeadX = loopReturnSourceHandleX(sourceNode, link, positions) + sourceLeadOffset;
		const segment = {
			start: Math.min(sourceLeadX, joinX),
			end: Math.max(sourceLeadX, joinX),
			y: laneY
		};
		segments.push(segment);
		routePlanner.reserveHorizontal(segment.start, segment.end, segment.y);
	}
	return segments;
}

/**
 * Finds a readable vertical lead for a loop return without stacking it on top of
 * another return lead.
 * @param {FlowNode} sourceNode Returning node.
 * @param {VisibleFlowLink} link Return link.
 * @param {Map<string, { x: number, y: number }>} positions Node positions.
 * @param {number} laneY Shared return bus lane.
 * @param {RoutePlanner} routePlanner Shared route planner.
 * @returns {number}
 */
function nextLoopReturnSourceLeadOffset(sourceNode, link, positions, laneY, routePlanner) {
	const sourceHandleX = loopReturnSourceHandleX(sourceNode, link, positions);
	const sourceHandleY = loopReturnSourceHandleY(sourceNode, link, positions);
	return routePlanner.nextVerticalLeadOffset({
		handleX: sourceHandleX,
		startY: sourceHandleY,
		endY: laneY,
		preferredOffset: 44,
		step: 34,
		ignoredNodeIds: new Set([sourceNode.id])
	});
}

/**
 * @param {FlowNode} node
 * @param {VisibleFlowLink} link
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function loopReturnSourceHandleX(node, link, positions) {
	const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
	if (!isBottomOutputPort(node, link.from.portIndex)) {
		return position.x + NODE_WIDTH + 4;
	}
	const outputs = node.outputs ?? 0;
	const bottomOutputs = node.bottomOutputs ?? 0;
	const sideOutputs = Math.max(0, outputs - bottomOutputs);
	const bottomIndex = Math.max(0, link.from.portIndex - sideOutputs);
	const totalBottomPorts = bottomOutputs + (node.bottomInputs ?? 0);
	return position.x + ((bottomIndex + 1) / (Math.max(1, totalBottomPorts) + 1)) * NODE_WIDTH;
}

/**
 * @param {FlowNode} node
 * @param {VisibleFlowLink} link
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function loopReturnSourceHandleY(node, link, positions) {
	const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
	if (isBottomOutputPort(node, link.from.portIndex)) {
		return position.y + NODE_HEIGHT + 4;
	}
	const sideOutputs = Math.max(0, (node.outputs ?? 0) - (node.bottomOutputs ?? 0));
	if (sideOutputs <= 1) {
		return position.y + NODE_HEIGHT / 2;
	}
	const sideIndex = Math.min(link.from.portIndex, sideOutputs - 1);
	return position.y + ((sideIndex + 1) / (sideOutputs + 1)) * NODE_HEIGHT;
}

/**
 * @param {FlowNode} node
 * @param {number} portIndex
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function outputHandleX(node, portIndex, positions) {
	const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
	if (!isBottomOutputPort(node, portIndex)) {
		return position.x + NODE_WIDTH + 4;
	}
	const outputs = node.outputs ?? 0;
	const bottomOutputs = node.bottomOutputs ?? 0;
	const sideOutputs = Math.max(0, outputs - bottomOutputs);
	const bottomIndex = Math.max(0, portIndex - sideOutputs);
	const totalBottomPorts = bottomOutputs + (node.bottomInputs ?? 0);
	return position.x + ((bottomIndex + 1) / (Math.max(1, totalBottomPorts) + 1)) * NODE_WIDTH;
}

/**
 * @param {FlowNode} node
 * @param {number} portIndex
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function outputHandleY(node, portIndex, positions) {
	const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
	if (isBottomOutputPort(node, portIndex)) {
		return position.y + NODE_HEIGHT + 4;
	}
	const sideOutputs = Math.max(0, (node.outputs ?? 0) - (node.bottomOutputs ?? 0));
	if (sideOutputs <= 1) {
		return position.y + NODE_HEIGHT / 2;
	}
	const visualIndex = branchOutputVisualIndex(node, portIndex);
	return position.y + ((visualIndex + 1) / (sideOutputs + 1)) * NODE_HEIGHT;
}

/**
 * @param {FlowNode} node
 * @param {number} portIndex
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function inputHandleX(node, portIndex, positions) {
	const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
	const bottomInputs = node.bottomInputs ?? 0;
	const inputs = node.inputs ?? 0;
	const sideInputs = Math.max(0, inputs - bottomInputs);
	if (bottomInputs && portIndex >= sideInputs) {
		const bottomIndex = Math.max(0, portIndex - sideInputs);
		const totalBottomPorts = bottomInputs + (node.bottomOutputs ?? 0);
		return position.x + ((bottomIndex + 1) / (Math.max(1, totalBottomPorts) + 1)) * NODE_WIDTH;
	}
	return position.x - 4;
}

/**
 * @param {FlowNode} node
 * @param {number} portIndex
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function inputHandleY(node, portIndex, positions) {
	const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
	const bottomInputs = node.bottomInputs ?? 0;
	const inputs = node.inputs ?? 0;
	const sideInputs = Math.max(0, inputs - bottomInputs);
	if (bottomInputs && portIndex >= sideInputs) {
		return position.y + NODE_HEIGHT + 4;
	}
	if (sideInputs <= 1) {
		return position.y + NODE_HEIGHT / 2;
	}
	const sideIndex = Math.min(portIndex, sideInputs - 1);
	return position.y + ((sideIndex + 1) / (sideInputs + 1)) * NODE_HEIGHT;
}

/**
 * @param {number} left
 * @param {number} right
 * @returns {{ start: number, end: number }}
 */
function normalizeVerticalSpan(left, right) {
	return {
		start: Math.min(left, right),
		end: Math.max(left, right)
	};
}

/**
 * @param {VerticalSegment[]} segments
 * @param {number} x
 * @param {number} start
 * @param {number} end
 * @returns {boolean}
 */
function isVerticalSegmentOccupied(segments, x, start, end) {
	return segments.some(
		(segment) =>
			Math.abs(segment.x - x) < 24 &&
			Math.min(segment.end, end) - Math.max(segment.start, start) > 38
	);
}

/**
 * @param {FlowNode[]} nodes
 * @param {Map<string, { x: number, y: number }>} positions
 * @param {number} sourceLeadX
 * @param {number} targetLeadX
 * @param {number} laneY
 * @param {Set<string>} ignoredNodeIds
 * @param {RoutePlanner} routePlanner
 * @returns {boolean}
 */
function isLoopBodyLaneFree(
	nodes,
	positions,
	sourceLeadX,
	targetLeadX,
	laneY,
	ignoredNodeIds,
	routePlanner
) {
	return (
		!nodes.some((node) =>
			isHorizontalSegmentBlocked(node, positions, sourceLeadX, targetLeadX, laneY, ignoredNodeIds)
		) && !routePlanner.isHorizontalOccupied(sourceLeadX, targetLeadX, laneY)
	);
}

/**
 * @param {HorizontalSegment[]} occupiedSegments
 * @param {number} startX
 * @param {number} endX
 * @param {number} y
 * @returns {boolean}
 */
function isHorizontalSegmentOccupied(occupiedSegments, startX, endX, y) {
	return occupiedSegments.some(
		(segment) =>
			Math.abs(segment.y - y) < 28 &&
			spansOverlap(normalizeHorizontalSpan(startX, endX), {
				start: segment.start,
				end: segment.end
			})
	);
}

/**
 * @param {number} start
 * @param {number} end
 * @returns {{ start: number, end: number }}
 */
function normalizeHorizontalSpan(start, end) {
	return {
		start: Math.min(start, end),
		end: Math.max(start, end)
	};
}

/**
 * @param {FlowNode} node
 * @param {Map<string, { x: number, y: number }>} positions
 * @param {number} startX
 * @param {number} endX
 * @param {number} y
 * @param {Set<string>} ignoredNodeIds
 * @returns {boolean}
 */
function isHorizontalSegmentBlocked(node, positions, startX, endX, y, ignoredNodeIds) {
	if (ignoredNodeIds.has(node.id)) {
		return false;
	}
	const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
	const pad = 10;
	const nodeLeft = position.x - pad;
	const nodeRight = position.x + NODE_WIDTH + pad;
	const nodeTop = position.y - pad;
	const nodeBottom = position.y + NODE_HEIGHT + pad;
	const { start, end } = normalizeHorizontalSpan(startX, endX);
	return y > nodeTop && y < nodeBottom && end > nodeLeft && start < nodeRight;
}

/**
 * @param {FlowNode} node
 * @param {Map<string, { x: number, y: number }>} positions
 * @param {number} x
 * @param {number} startY
 * @param {number} endY
 * @param {Set<string>} ignoredNodeIds
 * @returns {boolean}
 */
function isVerticalSegmentBlocked(node, positions, x, startY, endY, ignoredNodeIds) {
	if (ignoredNodeIds.has(node.id)) {
		return false;
	}
	const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
	const pad = 10;
	const nodeLeft = position.x - pad;
	const nodeRight = position.x + NODE_WIDTH + pad;
	const nodeTop = position.y - pad;
	const nodeBottom = position.y + NODE_HEIGHT + pad;
	const { start, end } = normalizeVerticalSpan(startY, endY);
	return x > nodeLeft && x < nodeRight && end > nodeTop && start < nodeBottom;
}

/**
 * @param {FlowNode} loopNode
 * @param {Map<string, FlowNode>} nodeById
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function loopSubtreeBottom(loopNode, nodeById, positions) {
	let bottom = (positions.get(loopNode.id)?.y ?? loopNode.y ?? 0) + NODE_HEIGHT;
	for (const node of nodeById.values()) {
		if (!isNodeContainedBy(loopNode, node)) {
			continue;
		}
		const position = positions.get(node.id) ?? { x: node.x ?? 0, y: node.y ?? 0 };
		bottom = Math.max(bottom, position.y + NODE_HEIGHT);
	}
	return bottom;
}

/**
 * @param {FlowNode} loopNode
 * @param {Iterable<string>} loopNodeIds
 * @param {Map<string, FlowNode>} nodeById
 * @returns {number}
 */
function loopContainmentDepth(loopNode, loopNodeIds, nodeById) {
	let depth = 0;
	for (const id of loopNodeIds) {
		const candidate = nodeById.get(id);
		if (candidate && isNodeContainedBy(candidate, loopNode)) {
			depth += 1;
		}
	}
	return depth;
}

/**
 * @param {{ link: VisibleFlowLink, sourceNode: FlowNode }[]} group
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {{ link: VisibleFlowLink, sourceNode: FlowNode } | undefined}
 */
function rightmostLoopReturnSource(group, positions) {
	/** @type {{ link: VisibleFlowLink, sourceNode: FlowNode } | undefined} */
	let rightmost;
	for (const current of group) {
		if (!rightmost) {
			rightmost = current;
			continue;
		}
		const currentPosition = positions.get(current.sourceNode.id) ?? {
			x: current.sourceNode.x ?? 0,
			y: current.sourceNode.y ?? 0
		};
		const rightmostPosition = positions.get(rightmost.sourceNode.id) ?? {
			x: rightmost.sourceNode.x ?? 0,
			y: rightmost.sourceNode.y ?? 0
		};
		if (currentPosition.x !== rightmostPosition.x) {
			rightmost = currentPosition.x > rightmostPosition.x ? current : rightmost;
			continue;
		}
		rightmost =
			compareNodePosition(current.sourceNode, rightmost.sourceNode, positions) > 0
				? current
				: rightmost;
	}
	return rightmost;
}

/**
 * @param {{ sourceNode: FlowNode }[]} group
 * @param {{ x: number, y: number }} targetPosition
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {{ start: number, end: number }}
 */
function loopReturnHorizontalSpan(group, targetPosition, positions) {
	const sourceXs = group.map(({ sourceNode }) => {
		const position = positions.get(sourceNode.id) ?? { x: sourceNode.x ?? 0, y: sourceNode.y ?? 0 };
		return position.x;
	});
	const start = Math.min(targetPosition.x - NODE_WIDTH, ...sourceXs);
	const end = Math.max(targetPosition.x + NODE_WIDTH, ...sourceXs.map((x) => x + NODE_WIDTH));
	return { start, end };
}

/**
 * @param {{ start: number, end: number }} left
 * @param {{ start: number, end: number }} right
 * @returns {boolean}
 */
function spansOverlap(left, right) {
	return left.start < right.end && left.end > right.start;
}

/**
 * @param {VisibleFlowLink} link
 * @param {Map<string, FlowNode>} nodeById
 * @param {Record<string, unknown> | undefined} data
 * @returns {string}
 */
function edgeType(link, nodeById, data) {
	if (link.routing === 'loop-return') {
		return 'loop-return';
	}
	if (isLoopBodyLink(link, nodeById)) {
		return 'loop-body';
	}
	if (data?.routeKind === 'branch') {
		return 'branch';
	}
	if (link.routing === 'orthogonal') {
		return 'step';
	}
	return 'smoothstep';
}

/**
 * @param {VisibleFlowLink} link
 * @param {Map<string, FlowNode>} nodeById
 * @returns {boolean}
 */
function isLoopBodyLink(link, nodeById) {
	const sourceNode = nodeById.get(link.from.nodeId);
	if (!sourceNode || !sourceNode.data?.isLoop) {
		return false;
	}
	const label = branchPortLabel(sourceNode, link.from.portIndex);
	return label === 'Loop';
}

/**
 * @param {VisibleFlowLink} link
 * @param {Map<string, FlowNode>} nodeById
 * @returns {boolean}
 */
function isBranchRouteCandidate(link, nodeById) {
	if (link.routing === 'loop-return') {
		return false;
	}
	const sourceNode = nodeById.get(link.from.nodeId);
	if (!sourceNode || isBottomOutputPort(sourceNode, link.from.portIndex)) {
		return false;
	}
	const sideOutputs = Math.max(0, (sourceNode.outputs ?? 0) - (sourceNode.bottomOutputs ?? 0));
	if (sideOutputs < 2) {
		return false;
	}
	const label = branchPortLabel(sourceNode, link.from.portIndex);
	return label !== 'Loop' && EDGE_BRANCH_LABELS.has(label);
}

/**
 * @param {FlowNode} node
 * @param {number} portIndex
 * @returns {number}
 */
function branchOutputVisualIndex(node, portIndex) {
	const sideOutputs = Math.max(0, (node.outputs ?? 0) - (node.bottomOutputs ?? 0));
	const sideIndex = Math.min(Math.max(0, portIndex), Math.max(0, sideOutputs - 1));
	const label = branchPortLabel(node, portIndex);
	if (sideOutputs === 2) {
		if (label === 'Then') {
			return 1;
		}
		if (label === 'Else' || label === 'Next') {
			return 0;
		}
	}
	return sideIndex;
}

/**
 * @param {FlowNode} node
 * @param {number} portIndex
 * @returns {string}
 */
function branchPortLabel(node, portIndex) {
	return branchLabelForStep(node.outputLabels?.[portIndex] ?? '', {
		type: node.type,
		classname: typeof node.data?.classname === 'string' ? node.data.classname : undefined,
		hasElseBranch: node.data?.hasElseBranch === true
	});
}

/**
 * @param {VisibleFlowLink} link
 * @returns {string | undefined}
 */
function edgeStyle(link) {
	if (link.routing === 'loop-return') {
		return 'stroke-dasharray: 7 5; opacity: 0.92; stroke-width: 2.2;';
	}
	if (link.synthetic) {
		return 'stroke-dasharray: 6 4;';
	}
	return void 0;
}

/**
 * @param {VisibleFlowLink} link
 * @param {Map<string, FlowNode>} nodeById
 * @returns {string}
 */
function edgeBranchLabel(link, nodeById) {
	if (link.routing === 'loop-return') {
		return '';
	}
	const sourceNode = nodeById.get(link.from.nodeId);
	const label = sourceNode ? branchPortLabel(sourceNode, link.from.portIndex) : '';
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
	const responseNormalNodes = responsePrimaryNodes.length
		? responsePrimaryNodes
		: returnNodes.length
			? []
			: fallbackTailNodes;
	const sortedResponseNormalNodes = uniqueNodes(responseNormalNodes).sort((left, right) =>
		compareNodePosition(left, right, positions)
	);
	const sortedReturnNodes = uniqueNodes(returnNodes).sort((left, right) =>
		compareNodePosition(left, right, positions)
	);
	const responseNodes = uniqueNodes(sortedResponseNormalNodes.concat(sortedReturnNodes));
	const responsePositionNodes = sortedResponseNormalNodes.length
		? sortedResponseNormalNodes
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
		Math.max(1, sortedResponseNormalNodes.length + (sortedReturnNodes.length ? 1 : 0)),
		0
	);
	const returnLaneBaseY = terminalReturnLaneY(visibleNodes, positions);
	const returnJoinX = responseNode.position.x - 72;
	const returnInputIndex = sortedResponseNormalNodes.length ? sortedResponseNormalNodes.length : 0;
	const returnBusStartX = sortedReturnNodes.length
		? Math.min(...sortedReturnNodes.map((node) => responseReturnBusStartX(node, positions)))
		: returnJoinX;
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
		...sortedResponseNormalNodes.map((node, index) =>
			createTerminalEdge(
				`${node.id}->${responseId}`,
				node.id,
				responseId,
				responseOutputHandle(node),
				inputHandleId(index),
				responseTerminalEdgeOptions(node, returnLaneBaseY, returnJoinX, returnBusStartX, false)
			)
		),
		...sortedReturnNodes.map((node, index) =>
			createTerminalEdge(
				`${node.id}->${responseId}`,
				node.id,
				responseId,
				responseOutputHandle(node),
				inputHandleId(returnInputIndex),
				responseTerminalEdgeOptions(
					node,
					returnLaneBaseY,
					returnJoinX,
					returnBusStartX,
					index === sortedReturnNodes.length - 1
				)
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
 * @param {TerminalEdgeOptions} [options]
 * @returns {import('@xyflow/svelte').Edge}
 */
function createTerminalEdge(id, source, target, sourceHandle, targetHandle, options = {}) {
	/** @type {import('@xyflow/svelte').Edge} */
	const edge = {
		id,
		source,
		target,
		type: options.type ?? 'smoothstep',
		animated: false,
		selectable: false,
		class: options.class,
		style: options.style ?? 'stroke-dasharray: 7 5; opacity: 0.8;',
		zIndex: options.zIndex ?? 0,
		data: options.data
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
 * @param {FlowNode} node
 * @param {number} laneY
 * @param {number} joinX
 * @param {number} busStartX
 * @param {boolean} connectToResponse
 * @returns {TerminalEdgeOptions}
 */
function responseTerminalEdgeOptions(node, laneY, joinX, busStartX, connectToResponse) {
	if (isReturnNode(node)) {
		return {
			class: 'flow-terminal-edge--return',
			style: 'stroke-dasharray: 7 5; opacity: 0.95; stroke-width: 2.4;',
			type: 'terminal-return',
			zIndex: 2,
			data: { busStartX, connectToResponse, joinX, laneY }
		};
	}
	return {
		class: 'flow-terminal-edge--response',
		style: 'stroke-dasharray: 7 5; opacity: 0.68;',
		zIndex: 1
	};
}

/**
 * @param {FlowNode} node
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function responseReturnBusStartX(node, positions) {
	return outputHandleX(node, 0, positions) + 32;
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
	if (isReturnNode(node)) {
		return outputHandleId(0);
	}
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
 * @param {FlowNode} node
 * @returns {number}
 */
function loopReturnInputPortIndex(node) {
	return Math.max(0, (node.inputs ?? 0) - (node.bottomInputs ?? 0));
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
 * @param {FlowNode[]} nodes
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function terminalReturnLaneY(nodes, positions) {
	const maxY = Math.max(...nodes.map((node) => positions.get(node.id)?.y ?? node.y ?? 0));
	return maxY + NODE_HEIGHT + 90;
}

/**
 * @param {FlowNode} left
 * @param {FlowNode} right
 * @param {Map<string, { x: number, y: number }>} positions
 * @returns {number}
 */
function compareNodePosition(left, right, positions) {
	const leftPosition = positions.get(left.id) ?? { x: left.x ?? 0, y: left.y ?? 0 };
	const rightPosition = positions.get(right.id) ?? { x: right.x ?? 0, y: right.y ?? 0 };
	return leftPosition.y - rightPosition.y || leftPosition.x - rightPosition.x;
}

/**
 * @param {FlowNode} parentNode
 * @param {FlowNode} node
 * @returns {boolean}
 */
function isNodeContainedBy(parentNode, node) {
	const parentId = parentNode.id;
	const nodeId = node.id;
	const nodeParentId = node.data?.parentId;
	if (!parentId || !nodeId || nodeId === parentId) {
		return false;
	}
	return (
		nodeId.startsWith(`${parentId}.`) ||
		nodeParentId === parentId ||
		(typeof nodeParentId === 'string' && nodeParentId.startsWith(`${parentId}.`))
	);
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
 * @param {boolean} [compactGaps]
 * @returns {Map<string, { x: number, y: number }>}
 */
function layoutVisibleFlow(nodes, compactGaps = false) {
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
	const xByOriginalX = compactGaps ? compactedXPositions(nodes) : undefined;
	for (const node of nodes) {
		positions.set(node.id, {
			x: xByOriginalX?.get(node.x) ?? Math.round(node.x - minX + 40),
			y: Math.round(node.y - minY + 40)
		});
	}
	return positions;
}

/**
 * @param {FlowNode[]} nodes
 * @returns {Map<number, number>}
 */
function compactedXPositions(nodes) {
	const orderedX = Array.from(new Set(nodes.map((node) => node.x))).sort(
		(left, right) => left - right
	);
	return new Map(orderedX.map((x, index) => [x, 40 + index * (NODE_WIDTH + 110)]));
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
	const isReturn = isReturnNode(node);
	const outputs = node.outputs ?? 0;
	const hasLoopReturnInput = Boolean(data.isLoop);
	const loopReturnInputIndex = loopReturnInputPortIndex(node);
	const renderedInputs = inputs + (hasLoopReturnInput ? 1 : 0);
	const renderedOutputs = isReturn && outputs === 0 ? 1 : outputs;
	const bottomInputs = node.bottomInputs ?? 0;
	const inputLabels = [...(node.inputLabels ?? [])];
	if (hasLoopReturnInput) {
		inputLabels.splice(loopReturnInputIndex, 0, '');
	}
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
		isReturn,
		isBreak: Boolean(data.isBreak),
		isXml: Boolean(data.isXml),
		isSourceContainer: Boolean(data.isSourceContainer),
		hasChildren: Boolean(data.hasChildren),
		hasThenBranch: data.hasThenBranch === true,
		hasElseBranch: data.hasElseBranch === true,
		inputs: renderedInputs,
		outputs: renderedOutputs,
		inputLabels,
		outputLabels: node.outputLabels ?? [],
		bottomInputs,
		bottomOutputs: node.bottomOutputs ?? 0,
		loopReturnInputIndex: hasLoopReturnInput ? loopReturnInputIndex : void 0,
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
