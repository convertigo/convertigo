import { describe, expect, it } from 'vitest';
import { inputHandleId, outputHandleId, toXyFlow } from './xyflow';

describe('Studio flow xyflow terminals', () => {
	it('keeps request and response terminals virtual and non-selectable', () => {
		const { nodes } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:init',
					type: 'SimpleStep',
					label: 'Init',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:init' }
				}
			],
			links: []
		});

		const request = nodes.find((node) => node.data.terminalKind === 'request');
		const response = nodes.find((node) => node.data.terminalKind === 'response');
		expect(request).toEqual(
			expect.objectContaining({
				id: 'Project.sq:Sequence.__request',
				selectable: false,
				data: expect.objectContaining({ isFlowTerminal: true })
			})
		);
		expect(response).toEqual(
			expect.objectContaining({
				id: 'Project.sq:Sequence.__response',
				selectable: false,
				data: expect.objectContaining({ isFlowTerminal: true })
			})
		);
	});

	it('labels then/else branch edges from output port semantics', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:if',
					type: 'IfThenElseStep',
					label: 'if(??)',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['true', 'false'],
					data: { originalId: 'Project.sq:Sequence.st:if', hasElseBranch: true }
				},
				{
					id: 'Project.sq:Sequence.st:then',
					type: 'SimpleStep',
					label: 'Then step',
					x: 180,
					y: 0,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:then' }
				},
				{
					id: 'Project.sq:Sequence.st:else',
					type: 'SimpleStep',
					label: 'Else step',
					x: 180,
					y: 120,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:else' }
				}
			],
			links: [
				{
					id: 'if-then',
					from: { nodeId: 'Project.sq:Sequence.st:if', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:then', portIndex: 0 }
				},
				{
					id: 'if-else',
					from: { nodeId: 'Project.sq:Sequence.st:if', portIndex: 1 },
					to: { nodeId: 'Project.sq:Sequence.st:else', portIndex: 0 }
				}
			]
		});

		expect(edges).toContainEqual(
			expect.objectContaining({
				id: 'if-then',
				label: 'THEN'
			})
		);
		expect(edges).toContainEqual(
			expect.objectContaining({
				id: 'if-else',
				label: 'ELSE'
			})
		);
	});

	it('labels simple if false edges as next continuations', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:if',
					type: 'IfStep',
					label: 'if(??)',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['true', 'false'],
					data: { originalId: 'Project.sq:Sequence.st:if' }
				},
				{
					id: 'Project.sq:Sequence.st:next',
					type: 'SimpleStep',
					label: 'Next step',
					x: 180,
					y: 0,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:next' }
				}
			],
			links: [
				{
					id: 'if-next',
					from: { nodeId: 'Project.sq:Sequence.st:if', portIndex: 1 },
					to: { nodeId: 'Project.sq:Sequence.st:next', portIndex: 0 }
				}
			]
		});

		expect(edges).toContainEqual(
			expect.objectContaining({
				id: 'if-next',
				label: 'NEXT'
			})
		);
	});

	it('labels explicit next continuation edges', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:if',
					type: 'IfStep',
					label: 'if(??)',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 1,
					outputLabels: ['next'],
					data: { originalId: 'Project.sq:Sequence.st:if' }
				},
				{
					id: 'Project.sq:Sequence.st:next',
					type: 'SimpleStep',
					label: 'Next step',
					x: 180,
					y: 0,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:next' }
				}
			],
			links: [
				{
					id: 'if-next',
					from: { nodeId: 'Project.sq:Sequence.st:if', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:next', portIndex: 0 }
				}
			]
		});

		expect(edges).toContainEqual(
			expect.objectContaining({
				id: 'if-next',
				label: 'NEXT'
			})
		);
	});

	it('uses explicit step routing for orthogonal branch links', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:if',
					type: 'IfStep',
					label: 'if(??)',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['true', 'false'],
					data: { originalId: 'Project.sq:Sequence.st:if' }
				},
				{
					id: 'Project.sq:Sequence.st:next',
					type: 'SimpleStep',
					label: 'Next step',
					x: 260,
					y: 160,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:next' }
				}
			],
			links: [
				{
					id: 'if-next',
					from: { nodeId: 'Project.sq:Sequence.st:if', portIndex: 1 },
					to: { nodeId: 'Project.sq:Sequence.st:next', portIndex: 0 },
					routing: 'orthogonal'
				}
			]
		});

		expect(edges).toContainEqual(
			expect.objectContaining({
				id: 'if-next',
				type: 'step',
				label: 'NEXT'
			})
		);
	});

	it('keeps the response connected to a bottom-output container instead of its child lane', () => {
		const { edges, nodes } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:object',
					type: 'JsonObjectStep',
					label: '"object": {...}',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					bottomOutputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:object' }
				},
				{
					id: 'Project.sq:Sequence.st:object.st:field',
					type: 'JsonFieldStep',
					label: '"field": ""',
					x: 180,
					y: 140,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:object.st:field' }
				}
			],
			links: [
				{
					id: 'object-field',
					from: { nodeId: 'Project.sq:Sequence.st:object', portIndex: 1 },
					to: { nodeId: 'Project.sq:Sequence.st:object.st:field', portIndex: 0 }
				},
				{
					id: 'field-object',
					from: { nodeId: 'Project.sq:Sequence.st:object.st:field', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:object', portIndex: 0 }
				}
			]
		});

		const response = nodes.find((node) => node.data.terminalKind === 'response');
		expect(response?.id).toBe('Project.sq:Sequence.__response');
		expect(edges).toContainEqual(
			expect.objectContaining({
				source: 'Project.sq:Sequence.st:object',
				target: 'Project.sq:Sequence.__response',
				sourceHandle: outputHandleId(0),
				targetHandle: inputHandleId(0)
			})
		);
		expect(edges).not.toContainEqual(
			expect.objectContaining({
				source: 'Project.sq:Sequence.st:object.st:field',
				target: 'Project.sq:Sequence.__response'
			})
		);
	});

	it('connects return steps to the response terminal', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:init',
					type: 'SimpleStep',
					label: 'Init',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:init' }
				},
				{
					id: 'Project.sq:Sequence.st:return',
					type: 'ReturnStep',
					label: 'return',
					x: 180,
					y: 0,
					inputs: 1,
					outputs: 1,
					data: {
						classname: 'com.twinsoft.convertigo.beans.steps.ReturnStep',
						originalId: 'Project.sq:Sequence.st:return'
					}
				}
			],
			links: [
				{
					id: 'init-return',
					from: { nodeId: 'Project.sq:Sequence.st:init', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:return', portIndex: 0 }
				}
			]
		});

		expect(edges).toContainEqual(
			expect.objectContaining({
				source: 'Project.sq:Sequence.st:return',
				target: 'Project.sq:Sequence.__response',
				sourceHandle: outputHandleId(0),
				targetHandle: inputHandleId(0)
			})
		);
	});

	it('selects structured children with equivalent step child qnames', () => {
		const { nodes } = toXyFlow(
			{
				id: 'Project.sq:Sequence',
				name: 'Sequence',
				nodes: [
					{
						id: 'Project.sq:Sequence.st:object.field',
						type: 'JsonFieldStep',
						label: '"field": ""',
						x: 0,
						y: 0,
						inputs: 1,
						outputs: 1,
						data: { originalId: 'Project.sq:Sequence.st:object.field' }
					}
				],
				links: []
			},
			{
				selectedObjectId: 'Project.sq:Sequence.st:object.st:field'
			}
		);

		const field = nodes.find((node) => node.id === 'Project.sq:Sequence.st:object.field');
		expect(field?.selected).toBe(true);
		expect(field?.data.isSelected).toBe(true);
	});
});
