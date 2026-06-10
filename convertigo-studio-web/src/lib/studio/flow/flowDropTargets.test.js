import { describe, expect, it } from 'vitest';
import { findFlowLaneDropTarget } from './flowDropTargets';

const sequenceId = 'Project.sq:Sequence';
const objectId = `${sequenceId}.st:object`;
const nodeSize = { width: 150, height: 72 };

describe('Studio flow lane drop targets', () => {
	it('targets first when a moved structured child is dropped before the first visible child', () => {
		const target = findFlowLaneDropTarget({
			point: { x: 60, y: 236 },
			flow: flowFixture(),
			xyNodes: xyFixture(),
			sourceId: `${objectId}.field2`,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toEqual({
			target: `${objectId}.field`,
			position: 'before',
			indicator: 'before',
			targetNodeId: `${objectId}.field`,
			fallbackTarget: objectId,
			fallbackPosition: 'inside',
			dropBranch: '',
			dropHostLabel: 'object'
		});
	});

	it('targets the next visible child directly when highlighting before it', () => {
		const target = findFlowLaneDropTarget({
			point: { x: 250, y: 236 },
			flow: flowFixture(),
			xyNodes: xyFixture(),
			sourceId: `${objectId}.field2`,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toMatchObject({
			target: `${objectId}.field1`,
			position: 'before',
			indicator: 'before',
			targetNodeId: `${objectId}.field1`,
			fallbackTarget: objectId,
			dropHostLabel: 'object'
		});
	});

	it('does not let the dragged object become its own lane target', () => {
		const target = findFlowLaneDropTarget({
			point: { x: 660, y: 236 },
			flow: flowFixture(),
			xyNodes: xyFixture(),
			sourceId: `${objectId}.field2`,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toMatchObject({
			target: `${objectId}.field1`,
			position: 'after',
			indicator: 'after',
			targetNodeId: `${objectId}.field1`
		});
	});

	it('does not let the dragged object become its own lane target with equivalent structured ids', () => {
		const target = findFlowLaneDropTarget({
			point: { x: 660, y: 236 },
			flow: flowFixture(),
			xyNodes: xyFixture(),
			sourceId: `${objectId}.st:field2`,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toMatchObject({
			target: `${objectId}.field1`,
			position: 'after',
			indicator: 'after',
			targetNodeId: `${objectId}.field1`
		});
	});

	it('targets a lower structured child lane when dragging a top-level sibling into it', () => {
		const target = findFlowLaneDropTarget({
			point: { x: 610, y: 436 },
			flow: {
				...flowFixture(),
				nodes: [
					...flowFixture().nodes,
					flowNode(`${sequenceId}.st:topField`, 4, sequenceId),
					flowNode(`${objectId}.field3`, 3, objectId)
				]
			},
			xyNodes: [
				...xyFixture(),
				xyNode(`${sequenceId}.st:topField`, 780, 0),
				xyNode(`${objectId}.field3`, 520, 400)
			],
			sourceId: `${sequenceId}.st:topField`,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toMatchObject({
			target: `${objectId}.field3`,
			position: 'after',
			indicator: 'after',
			targetNodeId: `${objectId}.field3`,
			fallbackTarget: objectId,
			dropHostLabel: 'object'
		});
	});

	it('keeps top-level drops in the top-level lane', () => {
		const target = findFlowLaneDropTarget({
			point: { x: 350, y: 76 },
			flow: flowFixture(),
			xyNodes: xyFixture(),
			sourceId: `${objectId}.field2`,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toMatchObject({
			target: objectId,
			position: 'after',
			indicator: 'after',
			targetNodeId: objectId,
			fallbackTarget: sequenceId,
			dropHostLabel: ''
		});
	});

	it('keeps branch context for implicit then children of an if step', () => {
		const ifId = `${objectId}.if`;
		const target = findFlowLaneDropTarget({
			point: { x: 250, y: 436 },
			flow: {
				...flowFixture(),
				nodes: [
					...flowFixture().nodes,
					flowNode(ifId, 3, objectId),
					flowNode(`${ifId}.field`, 0, ifId, 'true'),
					flowNode(`${ifId}.field1`, 1, ifId, 'true')
				]
			},
			xyNodes: [
				...xyFixture(),
				xyNode(ifId, 780, 200),
				xyNode(`${ifId}.field`, 0, 400),
				xyNode(`${ifId}.field1`, 260, 400)
			],
			sourceId: `${sequenceId}.st:topField`,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toMatchObject({
			target: `${ifId}.field1`,
			position: 'before',
			indicator: 'before',
			targetNodeId: `${ifId}.field1`,
			fallbackTarget: ifId,
			dropBranch: 'true',
			dropHostLabel: 'if'
		});
	});

	it('prefers a nested branch lane over its parent lane when both share the same row', () => {
		const ifId = `${objectId}.if`;
		const target = findFlowLaneDropTarget({
			point: { x: 610, y: 236 },
			flow: {
				...flowFixture(),
				nodes: [
					flowNode(objectId, 0, sequenceId),
					flowNode(`${objectId}.field`, 0, objectId),
					flowNode(ifId, 1, objectId),
					flowNode(`${ifId}.field`, 0, ifId, 'true'),
					flowNode(`${ifId}.field1`, 1, ifId, 'true'),
					flowNode(`${objectId}.field2`, 2, objectId)
				]
			},
			xyNodes: [
				xyNode(objectId, 160, 0),
				xyNode(`${objectId}.field`, 0, 200),
				xyNode(ifId, 260, 200),
				xyNode(`${ifId}.field`, 520, 200),
				xyNode(`${ifId}.field1`, 700, 200),
				xyNode(`${objectId}.field2`, 960, 200)
			],
			sourceId: `${sequenceId}.st:topField`,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toMatchObject({
			target: `${ifId}.field1`,
			position: 'before',
			indicator: 'before',
			targetNodeId: `${ifId}.field1`,
			fallbackTarget: ifId,
			dropBranch: 'true',
			dropHostLabel: 'if'
		});
	});

	it('uses the deepest lane when parent and child lanes overlap exactly', () => {
		const ifId = `${objectId}.if`;
		const target = findFlowLaneDropTarget({
			point: { x: 250, y: 236 },
			flow: {
				...flowFixture(),
				nodes: [
					flowNode(objectId, 0, sequenceId),
					flowNode(`${objectId}.field`, 0, objectId),
					flowNode(ifId, 1, objectId),
					flowNode(`${ifId}.field`, 0, ifId, 'true'),
					flowNode(`${ifId}.field1`, 1, ifId, 'true')
				]
			},
			xyNodes: [
				xyNode(objectId, 160, 0),
				xyNode(`${objectId}.field`, 0, 200),
				xyNode(ifId, 260, 200),
				xyNode(`${ifId}.field`, 0, 200),
				xyNode(`${ifId}.field1`, 260, 200)
			],
			sourceId: `${sequenceId}.st:topField`,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toMatchObject({
			target: `${ifId}.field1`,
			position: 'before',
			indicator: 'before',
			targetNodeId: `${ifId}.field1`,
			fallbackTarget: ifId,
			dropBranch: 'true',
			dropHostLabel: 'if'
		});
	});

	it('does not offer descendants as lane targets when moving their parent', () => {
		const target = findFlowLaneDropTarget({
			point: { x: 250, y: 236 },
			flow: flowFixture(),
			xyNodes: xyFixture(),
			sourceId: objectId,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toBeNull();
	});

	it('still allows the parent sibling lane when moving a container near top-level siblings', () => {
		const target = findFlowLaneDropTarget({
			point: { x: 500, y: 76 },
			flow: {
				...flowFixture(),
				nodes: [...flowFixture().nodes, flowNode(`${sequenceId}.st:next`, 4, sequenceId)]
			},
			xyNodes: [...xyFixture(), xyNode(`${sequenceId}.st:next`, 360, 0)],
			sourceId: objectId,
			selectedSequenceId: sequenceId,
			nodeSize
		});

		expect(target).toMatchObject({
			target: `${sequenceId}.st:next`,
			position: 'after',
			indicator: 'after',
			targetNodeId: `${sequenceId}.st:next`
		});
	});
});

function flowFixture() {
	return {
		id: sequenceId,
		name: 'Sequence',
		nodes: [
			flowNode(objectId, 0, sequenceId),
			flowNode(`${objectId}.field`, 0, objectId),
			flowNode(`${objectId}.field1`, 1, objectId),
			flowNode(`${objectId}.field2`, 2, objectId)
		],
		links: []
	};
}

/**
 * @param {string} id
 * @param {number} orderIndex
 * @param {string} parentId
 * @param {string=} parentBranch
 */
function flowNode(id, orderIndex, parentId, parentBranch = '') {
	return {
		id,
		type: 'JsonFieldStep',
		label: id,
		x: 0,
		y: 0,
		inputs: 1,
		outputs: 1,
		data: {
			originalId: id,
			parentId,
			parentBranch,
			orderIndex
		}
	};
}

function xyFixture() {
	return [
		xyNode(objectId, 160, 0),
		xyNode(`${objectId}.field`, 0, 200),
		xyNode(`${objectId}.field1`, 260, 200),
		xyNode(`${objectId}.field2`, 520, 200)
	];
}

/**
 * @param {string} id
 * @param {number} x
 * @param {number} y
 */
function xyNode(id, x, y) {
	return {
		id,
		type: 'flow-step',
		position: { x, y },
		data: {
			originalId: id
		}
	};
}
