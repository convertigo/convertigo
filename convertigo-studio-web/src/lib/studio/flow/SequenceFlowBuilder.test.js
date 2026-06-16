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

	it('does not continue a returned then branch to the next sibling', () => {
		const concatId = `${ifId}.st:concat`;
		const returnId = `${ifId}.st:return`;
		const nextId = `${sequenceId}.st:Log`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('IfStep', ifId, 'if(isTokenStillValid)', [
					step('XMLConcatStep', concatId, '<Bearer>'),
					step('ReturnStep', returnId, 'return')
				]),
				step('LogStep', nextId, 'Log')
			],
			palette()
		);

		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: ifId, portIndex: 0 },
				to: { nodeId: concatId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: concatId, portIndex: 0 },
				to: { nodeId: returnId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: ifId, portIndex: 1 },
				to: { nodeId: nextId, portIndex: 0 }
			})
		);
		expect(flow.links).not.toContainEqual(
			expect.objectContaining({
				from: { nodeId: ifId, portIndex: 0 },
				to: { nodeId: nextId, portIndex: 0 }
			})
		);
		expect(flow.links).not.toContainEqual(
			expect.objectContaining({
				from: { nodeId: concatId, portIndex: 0 },
				to: { nodeId: nextId, portIndex: 0 }
			})
		);
	});

	it('does not bubble a nested returned then branch to an outer sibling', () => {
		const outerIfId = `${sequenceId}.st:outerIf`;
		const middleIfId = `${outerIfId}.st:middleIf`;
		const innerIfId = `${middleIfId}.st:innerIf`;
		const concatId = `${innerIfId}.st:concat`;
		const returnId = `${innerIfId}.st:return`;
		const nextId = `${sequenceId}.st:Log`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('IfStep', outerIfId, 'if(owner)', [
					step('IfStep', middleIfId, 'if(exists)', [
						step('IfStep', innerIfId, 'if(isTokenStillValid)', [
							step('XMLConcatStep', concatId, '<Bearer>'),
							step('ReturnStep', returnId, 'return')
						])
					])
				]),
				step('LogStep', nextId, 'Log')
			],
			palette()
		);

		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: innerIfId, portIndex: 0 },
				to: { nodeId: concatId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: innerIfId, portIndex: 1 },
				to: { nodeId: nextId, portIndex: 0 }
			})
		);
		expect(flow.links).not.toContainEqual(
			expect.objectContaining({
				from: { nodeId: innerIfId, portIndex: 0 },
				to: { nodeId: nextId, portIndex: 0 }
			})
		);
	});

	it('does not bubble the previous child before a returned nested branch to an outer sibling', () => {
		const outerIfId = `${sequenceId}.st:outerIf`;
		const middleIfId = `${outerIfId}.st:middleIf`;
		const ageLogId = `${middleIfId}.st:LogAge`;
		const innerIfId = `${middleIfId}.st:innerIf`;
		const concatId = `${innerIfId}.st:concat`;
		const returnId = `${innerIfId}.st:return`;
		const nextId = `${sequenceId}.st:Log`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('IfStep', outerIfId, 'if(owner)', [
					step('IfStep', middleIfId, 'if(exists)', [
						step('LogStep', ageLogId, 'Log age'),
						step('IfStep', innerIfId, 'if(isTokenStillValid)', [
							step('XMLConcatStep', concatId, '<Bearer>'),
							step('ReturnStep', returnId, 'return')
						])
					])
				]),
				step('LogStep', nextId, 'Log token does not exist')
			],
			palette()
		);

		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: ageLogId, portIndex: 0 },
				to: { nodeId: innerIfId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: innerIfId, portIndex: 1 },
				to: { nodeId: nextId, portIndex: 0 }
			})
		);
		expect(flow.links).not.toContainEqual(
			expect.objectContaining({
				from: { nodeId: ageLogId, portIndex: 0 },
				to: { nodeId: nextId, portIndex: 0 }
			})
		);
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

	it('keeps loop completion inside bottom containers before continuing after the container', () => {
		const iteratorId = `${objectId}.st:jIterator`;
		const complexId = `${iteratorId}.st:complex`;
		const afterId = `${sequenceId}.st:afterObject`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('JsonObjectStep', objectId, '"object": {...}', [
					loopStep('IteratorStep', iteratorId, 'jIterator', [
						step('JsonObjectStep', complexId, '"complex": {...}')
					])
				]),
				step('SimpleStep', afterId, 'after object')
			],
			palette()
		);

		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: iteratorId, portIndex: 1 },
				to: { nodeId: objectId, portIndex: 1 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: objectId, portIndex: 0 },
				to: { nodeId: afterId, portIndex: 0 }
			})
		);
		expect(flow.links).not.toContainEqual(
			expect.objectContaining({
				from: { nodeId: iteratorId, portIndex: 1 },
				to: { nodeId: afterId, portIndex: 0 }
			})
		);
	});

	it('keeps a loop child of a bottom container on the child lane', () => {
		const iteratorId = `${objectId}.st:jIterator`;
		const fieldId = `${objectId}.st:field1`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('JsonObjectStep', objectId, '"object": {...}', [
					loopStep('IteratorStep', iteratorId, 'jIterator'),
					step('JsonFieldStep', fieldId, '"field1": ""')
				])
			],
			palette()
		);

		const nodes = nodeById(flow);
		const object = nodes.get(objectId);
		const iterator = nodes.get(iteratorId);
		const field = nodes.get(fieldId);

		expect(iterator?.y).toBeGreaterThan(object?.y ?? Infinity);
		expect(field?.y).toBe(iterator?.y);
		expect(iterator?.x).toBe(object?.x);
		expect(field?.x).toBeGreaterThan(iterator?.x ?? Infinity);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: fieldId, portIndex: 0 },
				to: { nodeId: objectId, portIndex: 1 }
			})
		);
	});

	it('keeps a bottom container above a nested loop and the loop body', () => {
		const iteratorId = `${objectId}.st:jIterator`;
		const fieldId = `${iteratorId}.st:field1`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('JsonObjectStep', objectId, '"object": {...}', [
					loopStep('IteratorStep', iteratorId, 'jIterator', [
						step('JsonFieldStep', fieldId, '"field1": ""')
					])
				])
			],
			palette()
		);

		const nodes = nodeById(flow);
		const object = nodes.get(objectId);
		const iterator = nodes.get(iteratorId);
		const field = nodes.get(fieldId);

		expect(iterator?.y).toBeGreaterThan(object?.y ?? Infinity);
		expect(field?.y).toBeGreaterThan(iterator?.y ?? Infinity);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: iteratorId, portIndex: 1 },
				to: { nodeId: objectId, portIndex: 1 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: fieldId, portIndex: 0 },
				to: { nodeId: iteratorId, portIndex: 0 },
				routing: 'loop-return'
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
		const thenNode = flow.nodes.find((node) => node.id === thenFieldId);
		const elseNode = flow.nodes.find((node) => node.id === elseFieldId);
		expect(ifNode?.data?.hasThenBranch).toBe(true);
		expect(ifNode?.data?.hasElseBranch).toBe(true);
		expect(thenNode?.data?.parentBranch).toBe('true');
		expect(elseNode?.data?.parentBranch).toBe('false');
		expect(thenNode?.y).toBeGreaterThan(ifNode?.y ?? Infinity);
		expect(elseNode?.y).toBe(ifNode?.y);
	});

	it('keeps loop completion on the main lane while placing the loop body below', () => {
		const iteratorId = `${sequenceId}.st:Iterator`;
		const bodyId = `${iteratorId}.st:length`;
		const doneId = `${sequenceId}.st:afterLoop`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				loopStep('IteratorStep', iteratorId, 'Iterator', [
					step('SimpleSourceStep', bodyId, 'length @(value/text())')
				]),
				step('SimpleStep', doneId, 'after loop')
			],
			palette()
		);

		const iteratorNode = flow.nodes.find((node) => node.id === iteratorId);
		const bodyNode = flow.nodes.find((node) => node.id === bodyId);
		const doneNode = flow.nodes.find((node) => node.id === doneId);

		expect(bodyNode?.y).toBeGreaterThan(iteratorNode?.y ?? Infinity);
		expect(doneNode?.y).toBe(iteratorNode?.y);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: iteratorId, portIndex: 0 },
				to: { nodeId: bodyId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: iteratorId, portIndex: 1 },
				to: { nodeId: doneId, portIndex: 0 }
			})
		);
	});

	it('keeps a following loop on the completion lane after a previous loop', () => {
		const firstLoopId = `${sequenceId}.st:Iterator`;
		const firstBodyId = `${firstLoopId}.st:length`;
		const middleId = `${sequenceId}.st:middle`;
		const secondLoopId = `${sequenceId}.st:Iterator1`;
		const secondBodyId = `${secondLoopId}.st:length`;
		const afterId = `${sequenceId}.st:afterLoop`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				loopStep('IteratorStep', firstLoopId, 'Iterator', [
					step('SimpleSourceStep', firstBodyId, 'length @(value/text())')
				]),
				step('SimpleStep', middleId, 'between loops'),
				loopStep('IteratorStep', secondLoopId, 'Iterator1', [
					step('SimpleSourceStep', secondBodyId, 'length @(value/text())')
				]),
				step('SimpleStep', afterId, 'after loop')
			],
			palette()
		);

		const firstLoop = flow.nodes.find((node) => node.id === firstLoopId);
		const firstBody = flow.nodes.find((node) => node.id === firstBodyId);
		const middle = flow.nodes.find((node) => node.id === middleId);
		const secondLoop = flow.nodes.find((node) => node.id === secondLoopId);
		const secondBody = flow.nodes.find((node) => node.id === secondBodyId);
		const after = flow.nodes.find((node) => node.id === afterId);

		expect(firstBody?.y).toBeGreaterThan(firstLoop?.y ?? Infinity);
		expect(middle?.y).toBe(firstLoop?.y);
		expect(secondLoop?.y).toBe(firstLoop?.y);
		expect(secondBody?.y).toBeGreaterThan(secondLoop?.y ?? Infinity);
		expect(after?.y).toBe(secondLoop?.y);
	});

	it('places direct loop body steps on a horizontal lane below the iterator', () => {
		const iteratorId = `${sequenceId}.st:Iterator`;
		const lengthId = `${iteratorId}.st:length`;
		const keyId = `${iteratorId}.st:key`;
		const sequenceJsId = `${iteratorId}.st:Sequence_JS`;
		const doneId = `${sequenceId}.st:afterLoop`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				loopStep('IteratorStep', iteratorId, 'Iterator', [
					step('SimpleSourceStep', lengthId, 'length @(value/text())'),
					step('SimpleSourceStep', keyId, 'key @(key/item/text())'),
					step('SimpleStep', sequenceJsId, 'Sequence_JS')
				]),
				step('SimpleStep', doneId, 'after loop')
			],
			palette()
		);

		const nodes = nodeById(flow);
		const iterator = nodes.get(iteratorId);
		const bodyNodes = [lengthId, keyId, sequenceJsId].map((id) => nodes.get(id));

		expect(iterator).toBeTruthy();
		expect(bodyNodes.every((node) => !!node)).toBe(true);
		for (let index = 0; index < bodyNodes.length; index += 1) {
			expect(bodyNodes[index]?.x).toBeGreaterThan(iterator?.x ?? Infinity);
			expect(bodyNodes[index]?.y).toBeGreaterThan(iterator?.y ?? Infinity);
			if (index > 0) {
				expect(bodyNodes[index]?.x).toBeGreaterThan(bodyNodes[index - 1]?.x ?? Infinity);
				expect(bodyNodes[index]?.y).toBe(bodyNodes[index - 1]?.y);
			}
		}
		expect(nodes.get(doneId)?.y).toBe(iterator?.y);
		expect(flow.links.filter((link) => link.to.nodeId === iteratorId)).toEqual(
			expect.arrayContaining([expect.objectContaining({ routing: 'loop-return' })])
		);
	});

	it('keeps nested loops on the parent body lane and puts their own body below', () => {
		const outerLoopId = `${sequenceId}.st:Iterator`;
		const outerStartId = `${outerLoopId}.st:length`;
		const innerLoopId = `${outerLoopId}.st:Iterator1`;
		const innerBodyId = `${innerLoopId}.st:key`;
		const outerTailId = `${outerLoopId}.st:Sequence_JS`;
		const doneId = `${sequenceId}.st:afterLoop`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				loopStep('IteratorStep', outerLoopId, 'Iterator', [
					step('SimpleSourceStep', outerStartId, 'length @(value/text())'),
					loopStep('IteratorStep', innerLoopId, 'Iterator1', [
						step('SimpleSourceStep', innerBodyId, 'key @(key/item/text())')
					]),
					step('SimpleStep', outerTailId, 'Sequence_JS')
				]),
				step('SimpleStep', doneId, 'after loop')
			],
			palette()
		);

		const nodes = nodeById(flow);
		const outerLoop = nodes.get(outerLoopId);
		const outerStart = nodes.get(outerStartId);
		const innerLoop = nodes.get(innerLoopId);
		const innerBody = nodes.get(innerBodyId);
		const outerTail = nodes.get(outerTailId);

		expect(outerStart?.x).toBeGreaterThan(outerLoop?.x ?? Infinity);
		expect(innerLoop?.x).toBeGreaterThan(outerLoop?.x ?? Infinity);
		expect(innerLoop?.x).toBeGreaterThan(outerStart?.x ?? Infinity);
		expect(outerTail?.x).toBeGreaterThan(innerLoop?.x ?? Infinity);
		expect(outerStart?.y).toBeGreaterThan(outerLoop?.y ?? Infinity);
		expect(innerLoop?.y).toBe(outerStart?.y);
		expect(outerTail?.y).toBe(outerStart?.y);
		expect(innerBody?.x).toBeGreaterThan(innerLoop?.x ?? Infinity);
		expect(innerBody?.y).toBeGreaterThan(innerLoop?.y ?? Infinity);
		expect(nodes.get(doneId)?.y).toBe(outerLoop?.y);
		expect(flow.links.filter((link) => link.to.nodeId === innerLoopId)).toEqual(
			expect.arrayContaining([expect.objectContaining({ routing: 'loop-return' })])
		);
	});

	it('keeps adjacent loop bodies on distinct horizontal lanes', () => {
		const firstLoopId = `${sequenceId}.st:Iterator`;
		const firstLengthId = `${firstLoopId}.jSimpleSource`;
		const firstIfId = `${firstLoopId}.jIf`;
		const secondLoopId = `${sequenceId}.st:Iterator1`;
		const secondLengthId = `${secondLoopId}.jSimpleSource`;
		const secondIfId = `${secondLoopId}.jIf`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				loopStep('IteratorStep', firstLoopId, 'Iterator', [
					step('SimpleSourceStep', firstLengthId, 'length @(value/text())'),
					step('IfStep', firstIfId, 'if(+length > 1)')
				]),
				loopStep('IteratorStep', secondLoopId, 'Iterator1', [
					step('SimpleSourceStep', secondLengthId, 'length @(value/text())'),
					step('IfStep', secondIfId, 'if(+length > 1)')
				])
			],
			palette()
		);

		const nodes = nodeById(flow);
		const firstLoop = nodes.get(firstLoopId);
		const secondLoop = nodes.get(secondLoopId);
		const firstLength = nodes.get(firstLengthId);
		const firstIf = nodes.get(firstIfId);
		const secondLength = nodes.get(secondLengthId);
		const secondIf = nodes.get(secondIfId);

		expect(secondLoop?.y).toBe(firstLoop?.y);
		expect(firstLength?.y).toBeGreaterThan(firstLoop?.y ?? Infinity);
		expect(firstIf?.y).toBe(firstLength?.y);
		expect(firstIf?.x).toBeGreaterThan(firstLength?.x ?? Infinity);
		expect(secondLength?.y).toBeGreaterThan(firstLength?.y ?? Infinity);
		expect(secondLength?.x).toBeGreaterThan(firstIf?.x ?? Infinity);
		expect(secondIf?.y).toBe(secondLength?.y);
		expect(secondIf?.x).toBeGreaterThan(secondLength?.x ?? Infinity);
		expect(flow.links.filter((link) => link.to.nodeId === firstLoopId)).toEqual(
			expect.arrayContaining([expect.objectContaining({ routing: 'loop-return' })])
		);
		expect(flow.links.filter((link) => link.to.nodeId === secondLoopId)).toEqual(
			expect.arrayContaining([expect.objectContaining({ routing: 'loop-return' })])
		);
	});

	it('places branchless if then children below the loop body lane', () => {
		const iteratorId = `${sequenceId}.st:Iterator`;
		const lengthId = `${iteratorId}.jSimpleSource`;
		const ifId = `${iteratorId}.jIf`;
		const keyId = `${ifId}.jSimpleSource`;
		const tailId = `${ifId}.Sequence_JS`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				loopStep('IteratorStep', iteratorId, 'Iterator', [
					step('SimpleSourceStep', lengthId, 'length @(value/text())'),
					step('IfStep', ifId, 'if(+length > 1)', [
						step('SimpleSourceStep', keyId, 'key @(key/item/text())'),
						step('SimpleStep', tailId, 'Sequence_JS')
					])
				])
			],
			palette()
		);

		const nodes = nodeById(flow);
		const loop = nodes.get(iteratorId);
		const length = nodes.get(lengthId);
		const ifNode = nodes.get(ifId);
		const key = nodes.get(keyId);
		const tail = nodes.get(tailId);

		expect(length?.y).toBeGreaterThan(loop?.y ?? Infinity);
		expect(ifNode?.y).toBe(length?.y);
		expect(key?.y).toBeGreaterThan(ifNode?.y ?? Infinity);
		expect(tail?.y).toBe(key?.y);
		expect(tail?.x).toBeGreaterThan(key?.x ?? Infinity);
		expect(flow.links.filter((link) => link.to.nodeId === iteratorId)).toEqual(
			expect.arrayContaining([
				expect.objectContaining({
					from: { nodeId: ifId, portIndex: 1 },
					routing: 'loop-return'
				}),
				expect.objectContaining({
					from: { nodeId: tailId, portIndex: 0 },
					routing: 'loop-return'
				})
			])
		);
	});

	it('keeps every child aligned when a bottom container is shifted after child placement', () => {
		const builder = new SequenceFlowBuilder();
		const parentId = `${sequenceId}.st:groups`;
		const attributeId = `${parentId}.st:Attribute`;
		const iteratorId = `${parentId}.st:jIterator`;
		const fillerId = `${sequenceId}.st:filler`;
		const tailId = `${sequenceId}.st:tail`;
		const parent = flowNode('XMLComplexStep', parentId, '<groups>', {
			bottomInputs: 1,
			bottomOutputs: 1
		});
		const filler = flowNode('SimpleStep', fillerId, 'filler');
		const tail = flowNode('SimpleStep', tailId, 'tail');
		const attribute = flowNode('XMLAttributeStep', attributeId, '@type = "array"', {
			parentId
		});
		const iterator = flowNode('SimpleIteratorStep', iteratorId, 'jIterator', {
			parentId,
			outputs: 2,
			outputLabels: ['loop', 'done'],
			isLoop: true
		});
		const nodes = [parent, filler, tail, attribute, iterator];
		const nodeMap = new Map(nodes.map((node) => [node.id, node]));
		const substepsByParent = new Map([[parentId, [{ id: attributeId }, { id: iteratorId }]]]);
		const childOrderByParent = new Map([[parentId, [attributeId, iteratorId]]]);
		const alignmentTargetsByNode = new Map([
			[
				parentId,
				{
					tailIds: new Set([tailId]),
					branchByTailId: new Map([[tailId, 'true']])
				}
			]
		]);
		const bottomOutputChildren = new Map([[parentId, new Set([attributeId])]]);
		const links = [
			{
				id: 'parent-attribute',
				from: { nodeId: parentId, portIndex: 1 },
				to: { nodeId: attributeId, portIndex: 0 }
			},
			{
				id: 'attribute-iterator',
				from: { nodeId: attributeId, portIndex: 0 },
				to: { nodeId: iteratorId, portIndex: 0 }
			}
		];

		builder.layoutNodesHorizontally(
			nodes,
			nodeMap,
			substepsByParent,
			childOrderByParent,
			alignmentTargetsByNode,
			bottomOutputChildren,
			links
		);

		expect(attribute.x).toBe(parent.x);
		expect(iterator.x).toBeGreaterThan(attribute.x);
		expect(iterator.y).toBe(attribute.y);
	});

	it('keeps same-lane nodes far enough apart for branch ports and labels', () => {
		const builder = new SequenceFlowBuilder();
		const firstId = `${sequenceId}.st:first`;
		const secondId = `${sequenceId}.st:second`;
		const first = flowNode('IfExistStep', firstId, 'IfExist', {
			outputs: 2,
			outputLabels: ['true', 'false']
		});
		const second = flowNode('XMLErrorStep', secondId, '<error>');
		first.x = 340;
		first.y = 240;
		second.x = 510;
		second.y = 240;
		const nodeMap = new Map([
			[first.id, first],
			[second.id, second]
		]);

		builder.spreadRowOverlaps(nodeMap, new Map(), 170);

		expect(second.x).toBeGreaterThanOrEqual(first.x + 230);
	});

	it('renders step variables behind a requestable step vars port', () => {
		const callId = `${sequenceId}.st:Call_Sequence`;
		const variablesFolderId = `${callId}.variables`;
		const simpleVariableId = `${callId}.v:group`;
		const multiVariableId = `${callId}.v:returnedAttributes`;
		const nextId = `${sequenceId}.st:next`;
		const flow = new SequenceFlowBuilder().buildFlowFromTree(
			'Project',
			'Sequence',
			[
				step('SequenceStep', callId, 'Call_Sequence', [
					folder(variablesFolderId, 'Variables', [
						variable('StepVariable', simpleVariableId, 'group'),
						variable('StepMultiValuedVariable', multiVariableId, 'returnedAttributes')
					])
				]),
				step('XMLElementStep', nextId, '<next>')
			],
			palette()
		);
		const callNode = flow.nodes.find((node) => node.id === callId);
		const simpleVariableNode = flow.nodes.find((node) => node.id === simpleVariableId);
		const multiVariableNode = flow.nodes.find((node) => node.id === multiVariableId);

		expect(callNode).toEqual(
			expect.objectContaining({
				bottomOutputs: 1,
				bottomOutputLabels: ['vars']
			})
		);
		expect(simpleVariableNode).toEqual(
			expect.objectContaining({
				inputs: 1,
				outputs: 0,
				data: expect.objectContaining({ isVariable: true })
			})
		);
		expect(multiVariableNode).toEqual(
			expect.objectContaining({
				inputs: 1,
				outputs: 0,
				data: expect.objectContaining({ isVariable: true })
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: callId, portIndex: 1 },
				to: { nodeId: simpleVariableId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: callId, portIndex: 1 },
				to: { nodeId: multiVariableId, portIndex: 0 }
			})
		);
		expect(flow.links).toContainEqual(
			expect.objectContaining({
				from: { nodeId: callId, portIndex: 0 },
				to: { nodeId: nextId, portIndex: 0 }
			})
		);
		expect(flow.links).not.toContainEqual(
			expect.objectContaining({
				from: expect.objectContaining({ nodeId: simpleVariableId })
			})
		);
		expect(flow.links).not.toContainEqual(
			expect.objectContaining({
				from: expect.objectContaining({ nodeId: multiVariableId })
			})
		);
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
		paletteEntry('IfThenElseStep', { outputs: 2, outputLabels: ['true', 'false'] }),
		paletteEntry('IteratorStep', { outputs: 2, outputLabels: ['loop', 'done'] }),
		paletteEntry('SequenceStep', { bottomOutputs: 1, bottomOutputLabels: ['vars'] }),
		paletteEntry('SimpleSourceStep'),
		paletteEntry('SimpleStep')
	]);
}

/**
 * @param {string} type
 * @param {{ outputs?: number, outputLabels?: string[], bottomInputs?: number, bottomOutputs?: number, bottomOutputLabels?: string[] }} [options]
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
			bottomOutputs: options.bottomOutputs ?? 0,
			bottomOutputLabels: options.bottomOutputLabels
		}
	];
}

/**
 * @param {string} type
 * @param {string} id
 * @param {string} label
 * @param {{
 * 	parentId?: string,
 * 	outputs?: number,
 * 	outputLabels?: string[],
 * 	bottomInputs?: number,
 * 	bottomOutputs?: number,
 * 	isLoop?: boolean
 * }} [options]
 * @returns {import('./types').FlowNode}
 */
function flowNode(type, id, label, options = {}) {
	return {
		id,
		type,
		label,
		name: label,
		x: 0,
		y: 0,
		inputs: 1,
		outputs: options.outputs ?? 1,
		outputLabels: options.outputLabels,
		bottomInputs: options.bottomInputs ?? 0,
		bottomOutputs: options.bottomOutputs ?? 0,
		data: {
			parentId: options.parentId ?? sequenceId,
			isLoop: options.isLoop ?? false
		}
	};
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

/**
 * @param {string} simpleType
 * @param {string} id
 * @param {string} label
 * @param {import('./types').SequenceTreeNode[]} [children]
 * @returns {import('./types').SequenceTreeNode}
 */
function loopStep(simpleType, id, label, children = []) {
	return {
		...step(simpleType, id, label, children),
		isLoop: true
	};
}

/**
 * @param {string} id
 * @param {string} label
 * @param {import('./types').SequenceTreeNode[]} [children]
 * @returns {import('./types').SequenceTreeNode}
 */
function folder(id, label, children = []) {
	return {
		id,
		label,
		name: label,
		classname: '',
		icon: 'folder',
		isLoop: false,
		isXml: false,
		isSourceContainer: false,
		hasChildren: children.length > 0,
		children
	};
}

/**
 * @param {string} simpleType
 * @param {string} id
 * @param {string} label
 * @returns {import('./types').SequenceTreeNode}
 */
function variable(simpleType, id, label) {
	return {
		id,
		label,
		name: label,
		classname: `com.twinsoft.convertigo.beans.variables.${simpleType}`,
		icon: '',
		isLoop: false,
		isXml: false,
		isSourceContainer: false,
		hasChildren: false,
		children: []
	};
}

/**
 * @param {import('./types').Flow} flow
 * @returns {Map<string, import('./types').FlowNode>}
 */
function nodeById(flow) {
	return new Map(flow.nodes.map((node) => [node.id, node]));
}
