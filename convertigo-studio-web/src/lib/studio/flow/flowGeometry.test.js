import { describe, expect, it } from 'vitest';
import { flowEdgeDistanceToPoint, flowEdgePolyline, pointSegmentDistance } from './flowGeometry';

const nodeSize = { width: 150, height: 72 };

describe('Studio flow edge geometry', () => {
	it('tracks the visible orthogonal path for step edges', () => {
		const source = xyNode('source', 0, 0, {
			outputs: 1,
			bottomOutputs: 0
		});
		const target = xyNode('target', 260, 160, {
			inputs: 1,
			bottomInputs: 0
		});
		const edge = {
			id: 'source-target',
			source: 'source',
			target: 'target',
			sourceHandle: 'output-0',
			targetHandle: 'input-0',
			type: 'step'
		};
		const visiblePoint = { x: 185, y: 36 };
		const diagonalDistance = pointSegmentDistance(
			visiblePoint,
			nodeCenter(source),
			nodeCenter(target)
		);

		expect(flowEdgePolyline(source, target, edge, nodeSize)).toEqual([
			{ x: 150, y: 36 },
			{ x: 205, y: 36 },
			{ x: 205, y: 196 },
			{ x: 260, y: 196 }
		]);
		expect(diagonalDistance).toBeGreaterThan(40);
		expect(flowEdgeDistanceToPoint(visiblePoint, source, target, edge, nodeSize)).toBe(0);
	});

	it('keeps smooth edges on the center segment used by the existing hit test', () => {
		const source = xyNode('source', 0, 0);
		const target = xyNode('target', 260, 160);
		const edge = {
			id: 'source-target',
			source: 'source',
			target: 'target',
			type: 'smoothstep'
		};

		expect(flowEdgePolyline(source, target, edge, nodeSize)).toEqual([
			nodeCenter(source),
			nodeCenter(target)
		]);
	});
});

/**
 * @param {string} id
 * @param {number} x
 * @param {number} y
 * @param {Record<string, unknown>} [data]
 * @returns {import('@xyflow/svelte').Node}
 */
function xyNode(id, x, y, data = {}) {
	return {
		id,
		position: { x, y },
		data
	};
}

/**
 * @param {import('@xyflow/svelte').Node} node
 * @returns {{ x: number, y: number }}
 */
function nodeCenter(node) {
	return {
		x: node.position.x + nodeSize.width / 2,
		y: node.position.y + nodeSize.height / 2
	};
}
