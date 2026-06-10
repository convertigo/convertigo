/**
 * @typedef {{ x: number, y: number }} Point
 * @typedef {{ width: number, height: number }} Size
 */

/**
 * @param {Point} point
 * @param {import('@xyflow/svelte').Node} source
 * @param {import('@xyflow/svelte').Node} target
 * @param {import('@xyflow/svelte').Edge} edge
 * @param {Size} nodeSize
 * @returns {number}
 */
function flowEdgeDistanceToPoint(point, source, target, edge, nodeSize) {
	const points = flowEdgePolyline(source, target, edge, nodeSize);
	let best = Number.POSITIVE_INFINITY;
	for (let index = 1; index < points.length; index += 1) {
		best = Math.min(best, pointSegmentDistance(point, points[index - 1], points[index]));
	}
	return best;
}

/**
 * @param {import('@xyflow/svelte').Node} source
 * @param {import('@xyflow/svelte').Node} target
 * @param {import('@xyflow/svelte').Edge} edge
 * @param {Size} nodeSize
 * @returns {Point[]}
 */
function flowEdgePolyline(source, target, edge, nodeSize) {
	if (edge.type !== 'step') {
		return [nodeCenter(source, nodeSize), nodeCenter(target, nodeSize)];
	}
	const start = edgeHandlePoint(source, edge.sourceHandle, 'source', nodeSize);
	const end = edgeHandlePoint(target, edge.targetHandle, 'target', nodeSize);
	const sourceBottom = isBottomHandle(source, edge.sourceHandle, 'source');
	const targetBottom = isBottomHandle(target, edge.targetHandle, 'target');
	if (!sourceBottom && !targetBottom) {
		const midX = (start.x + end.x) / 2;
		return [start, { x: midX, y: start.y }, { x: midX, y: end.y }, end];
	}
	const midY = (start.y + end.y) / 2;
	return [start, { x: start.x, y: midY }, { x: end.x, y: midY }, end];
}

/**
 * @param {import('@xyflow/svelte').Node} node
 * @param {string | undefined | null} handleId
 * @param {'source' | 'target'} kind
 * @returns {boolean}
 */
function isBottomHandle(node, handleId, kind) {
	const data = /** @type {Record<string, unknown>} */ (node.data ?? {});
	const portCount = Number(data[kind === 'source' ? 'outputs' : 'inputs'] ?? 0);
	const bottomCount = Number(data[kind === 'source' ? 'bottomOutputs' : 'bottomInputs'] ?? 0);
	const sideCount = Math.max(0, portCount - bottomCount);
	return bottomCount > 0 && handleIndex(handleId) >= sideCount;
}

/**
 * @param {import('@xyflow/svelte').Node} node
 * @param {string | undefined | null} handleId
 * @param {'source' | 'target'} kind
 * @param {Size} nodeSize
 * @returns {Point}
 */
function edgeHandlePoint(node, handleId, kind, nodeSize) {
	const data = /** @type {Record<string, unknown>} */ (node.data ?? {});
	const portCount = Number(data[kind === 'source' ? 'outputs' : 'inputs'] ?? 0);
	const bottomCount = Number(data[kind === 'source' ? 'bottomOutputs' : 'bottomInputs'] ?? 0);
	const oppositeBottomCount = Number(
		data[kind === 'source' ? 'bottomInputs' : 'bottomOutputs'] ?? 0
	);
	const sideCount = Math.max(0, portCount - bottomCount);
	const index = handleIndex(handleId);
	if (index >= sideCount && bottomCount > 0) {
		const bottomIndex = Math.min(bottomCount - 1, Math.max(0, index - sideCount));
		const bottomOrder = kind === 'target' ? oppositeBottomCount + bottomIndex : bottomIndex;
		const bottomTotal = bottomCount + oppositeBottomCount;
		return {
			x: node.position.x + ((bottomOrder + 1) / (bottomTotal + 1)) * nodeSize.width,
			y: node.position.y + nodeSize.height
		};
	}
	return {
		x: node.position.x + (kind === 'source' ? nodeSize.width : 0),
		y: node.position.y + sidePortTop(index, sideCount) * nodeSize.height
	};
}

/**
 * @param {string | undefined | null} handleId
 * @returns {number}
 */
function handleIndex(handleId) {
	const match = `${handleId ?? ''}`.match(/-(\d+)$/);
	return match?.[1] ? Number(match[1]) : 0;
}

/**
 * @param {number} index
 * @param {number} count
 * @returns {number}
 */
function sidePortTop(index, count) {
	if (count <= 1) {
		return 0.5;
	}
	return (index + 1) / (count + 1);
}

/**
 * @param {import('@xyflow/svelte').Node} node
 * @param {Size} nodeSize
 * @returns {Point}
 */
function nodeCenter(node, nodeSize) {
	return {
		x: node.position.x + nodeSize.width / 2,
		y: node.position.y + nodeSize.height / 2
	};
}

/**
 * @param {Point} point
 * @param {Point} start
 * @param {Point} end
 * @returns {number}
 */
function pointSegmentDistance(point, start, end) {
	const dx = end.x - start.x;
	const dy = end.y - start.y;
	const lengthSquared = dx * dx + dy * dy;
	if (!lengthSquared) {
		return Math.hypot(point.x - start.x, point.y - start.y);
	}
	const t = Math.max(
		0,
		Math.min(1, ((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared)
	);
	return Math.hypot(point.x - (start.x + t * dx), point.y - (start.y + t * dy));
}

export { flowEdgeDistanceToPoint, flowEdgePolyline, pointSegmentDistance };
