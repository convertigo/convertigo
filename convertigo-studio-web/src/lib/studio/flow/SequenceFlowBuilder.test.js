import { describe, expect, it } from 'vitest';
import { SequenceFlowBuilder } from './SequenceFlowBuilder';

const sequenceId = 'Project.sq:Sequence';
const objectId = `${sequenceId}.st:object`;
const firstFieldId = `${objectId}.st:field`;
const movedFieldId = `${objectId}.st:field2`;
const ifId = `${objectId}.st:if`;
const ifFieldId = `${ifId}.st:field1`;

describe('SequenceFlowBuilder bottom container links', () => {
	it('keeps the child lane return link after a nested child is reordered before an if step', () => {
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('JsonObjectStep', objectId, '"object": {...}', [
					step('JsonFieldStep', firstFieldId, '"field": ""'),
					step('JsonFieldStep', movedFieldId, '"field2": ""'),
					step('IfStep', ifId, 'if(??)', [step('JsonFieldStep', ifFieldId, '"field1": ""')])
				])
			],
			palette()
		);

		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: objectId, portIndex: 1 },
				to: { nodeId: firstFieldId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: ifFieldId, portIndex: 0 },
				to: { nodeId: objectId, portIndex: 1 }
			})
		);
	});

	it('marks direct children of a branchless if step as the then branch', () => {
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('JsonObjectStep', objectId, '"object": {...}', [
					step('IfStep', ifId, 'if(??)', [step('JsonFieldStep', ifFieldId, '"field1": ""')])
				])
			],
			palette()
		);

		expect(flow.nodes.find((node) => node.id === ifFieldId)?.data?.parentBranch).toBe('true');
		expect(flow.nodes.find((node) => node.id === ifId)?.data?.hasThenBranch).toBe(true);
		expect(flow.nodes.find((node) => node.id === ifId)?.data?.hasElseBranch).toBe(false);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: ifId, portIndex: 0 },
				to: { nodeId: ifFieldId, portIndex: 0 }
			})
		);
	});

	it('places implicit then children below a branchless if while keeping continuation on the parent lane', () => {
		const nextFieldId = `${objectId}.st:field2`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('JsonObjectStep', objectId, '"object": {...}', [
					step('IfStep', ifId, 'if(??)', [step('JsonFieldStep', ifFieldId, '"field1": ""')]),
					step('JsonFieldStep', nextFieldId, '"field2": ""')
				])
			],
			palette()
		);

		const ifNode = flow.nodes.find((node) => node.id === ifId);
		const thenChild = flow.nodes.find((node) => node.id === ifFieldId);
		const continuation = flow.nodes.find((node) => node.id === nextFieldId);

		expect(thenChild?.data?.parentBranch).toBe('true');
		expect(Number.isFinite(ifNode?.y)).toBe(true);
		expect(thenChild?.y).toBeGreaterThan(ifNode?.y ?? Infinity);
		expect(continuation?.y).toBe(ifNode?.y);
	});

	it('keeps a simple if next output connected to the immediate conditional sibling', () => {
		const firstIfId = `${objectId}.st:firstIf`;
		const firstIfFieldId = `${firstIfId}.st:field`;
		const secondIfId = `${objectId}.st:secondIf`;
		const secondIfFieldId = `${secondIfId}.st:field`;
		const nextFieldId = `${objectId}.st:next`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('JsonObjectStep', objectId, '"object": {...}', [
					step('IfStep', firstIfId, 'if(first)', [
						step('JsonFieldStep', firstIfFieldId, '"field": ""')
					]),
					step('IfStep', secondIfId, 'if(second)', [
						step('JsonFieldStep', secondIfFieldId, '"field2": ""')
					]),
					step('JsonFieldStep', nextFieldId, '"next": ""')
				])
			],
			palette()
		);

		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: firstIfId, portIndex: 1 },
				to: { nodeId: secondIfId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: firstIfFieldId, portIndex: 0 },
				to: { nodeId: secondIfId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: secondIfId, portIndex: 1 },
				to: { nodeId: nextFieldId, portIndex: 0 }
			})
		);
	});

	it('marks explicit then and else branch lanes on conditional steps', () => {
		const thenFieldId = `${ifId}.st:thenField`;
		const elseFieldId = `${ifId}.st:elseField`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('IfThenElseStep', ifId, 'ifThenElse(??)', [
					step('ThenStep', `${ifId}.st:then`, 'then', [
						step('JsonFieldStep', thenFieldId, '"thenField": ""')
					]),
					step('ElseStep', `${ifId}.st:else`, 'else', [
						step('JsonFieldStep', elseFieldId, '"elseField": ""')
					])
				])
			],
			palette()
		);

		const ifNode = flow.nodes.find((node) => node.id === ifId);
		expect(ifNode?.data?.hasThenBranch).toBe(true);
		expect(ifNode?.data?.hasElseBranch).toBe(true);
		expect(flow.nodes.find((node) => node.id === thenFieldId)?.data?.parentBranch).toBe('true');
		expect(flow.nodes.find((node) => node.id === elseFieldId)?.data?.parentBranch).toBe('false');
	});
});

/**
 * @returns {Map<string, import('./types').PaletteItem>}
 */
function palette() {
	return new Map([
		paletteEntry('JsonObjectStep', { bottomInputs: 1, bottomOutputs: 1 }),
		paletteEntry('JsonFieldStep'),
		paletteEntry('IfStep', { outputs: 2, outputLabels: ['true', 'false'] }),
		paletteEntry('IfThenElseStep', { outputs: 2, outputLabels: ['true', 'false'] })
	]);
}

/**
 * @param {string} type
 * @param {{ outputs?: number, outputLabels?: string[], bottomInputs?: number, bottomOutputs?: number }} [options]
 * @returns {[string, import('./types').PaletteItem]}
 */
function paletteEntry(type, options = {}) {
	return [
		type,
		{
			type,
			label: type,
			color: '#22d3ee',
			inputs: 1,
			outputs: options.outputs ?? 1,
			outputLabels: options.outputLabels,
			bottomInputs: options.bottomInputs ?? 0,
			bottomOutputs: options.bottomOutputs ?? 0
		}
	];
}

/**
 * @param {string} simpleType
 * @param {string} id
 * @param {string} label
 * @param {import('./types').SequenceTreeNode[]} [children]
 * @returns {import('./types').SequenceTreeNode}
 */
function step(simpleType, id, label, children = []) {
	return {
		id,
		label,
		name: label,
		classname: `com.twinsoft.convertigo.beans.steps.${simpleType}`,
		icon: '',
		isLoop: false,
		isXml: false,
		isSourceContainer: false,
		hasChildren: children.length > 0,
		children
	};
}
