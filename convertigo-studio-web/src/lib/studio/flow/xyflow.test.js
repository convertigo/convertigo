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

	it('routes visible split branches with separated vertical leads', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:if',
					type: 'IfStep',
					label: 'if(??)',
					x: 0,
					y: 120,
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
					y: 0,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:next' }
				},
				{
					id: 'Project.sq:Sequence.st:then',
					type: 'SimpleStep',
					label: 'Then step',
					x: 260,
					y: 260,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:then' }
				}
			],
			links: [
				{
					id: 'if-then',
					from: { nodeId: 'Project.sq:Sequence.st:if', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:then', portIndex: 0 },
					routing: 'orthogonal'
				},
				{
					id: 'if-next',
					from: { nodeId: 'Project.sq:Sequence.st:if', portIndex: 1 },
					to: { nodeId: 'Project.sq:Sequence.st:next', portIndex: 0 },
					routing: 'orthogonal'
				}
			]
		});

		const thenEdge = edges.find((edge) => edge.id === 'if-then');
		const nextEdge = edges.find((edge) => edge.id === 'if-next');
		expect(thenEdge).toEqual(
			expect.objectContaining({
				type: 'branch',
				label: 'THEN',
				data: expect.objectContaining({
					routeKind: 'branch',
					sourceLeadOffset: expect.any(Number)
				})
			})
		);
		expect(nextEdge).toEqual(
			expect.objectContaining({
				type: 'branch',
				label: 'NEXT',
				data: expect.objectContaining({
					routeKind: 'branch',
					sourceLeadOffset: expect.any(Number)
				})
			})
		);
		expect(thenEdge?.data?.sourceLeadOffset).not.toBe(nextEdge?.data?.sourceLeadOffset);
	});

	it('compacts visible node positions when substeps are collapsed', () => {
		const { nodes, edges } = toXyFlow(
			{
				id: 'Project.sq:Sequence',
				name: 'Sequence',
				nodes: [
					{
						id: 'Project.sq:Sequence.st:before',
						type: 'SimpleStep',
						label: 'before',
						x: 0,
						y: 0,
						inputs: 1,
						outputs: 1,
						data: { originalId: 'Project.sq:Sequence.st:before' }
					},
					{
						id: 'Project.sq:Sequence.st:hidden1',
						type: 'SimpleStep',
						label: 'hidden1',
						x: 260,
						y: 0,
						inputs: 1,
						outputs: 1,
						data: { originalId: 'Project.sq:Sequence.st:hidden1' }
					},
					{
						id: 'Project.sq:Sequence.st:hidden2',
						type: 'SimpleStep',
						label: 'hidden2',
						x: 520,
						y: 0,
						inputs: 1,
						outputs: 1,
						data: { originalId: 'Project.sq:Sequence.st:hidden2' }
					},
					{
						id: 'Project.sq:Sequence.st:after',
						type: 'SimpleStep',
						label: 'after',
						x: 2000,
						y: 200,
						inputs: 1,
						outputs: 1,
						data: { originalId: 'Project.sq:Sequence.st:after' }
					}
				],
				links: [
					{
						id: 'before-hidden1',
						from: { nodeId: 'Project.sq:Sequence.st:before', portIndex: 0 },
						to: { nodeId: 'Project.sq:Sequence.st:hidden1', portIndex: 0 }
					},
					{
						id: 'hidden1-hidden2',
						from: { nodeId: 'Project.sq:Sequence.st:hidden1', portIndex: 0 },
						to: { nodeId: 'Project.sq:Sequence.st:hidden2', portIndex: 0 }
					},
					{
						id: 'hidden2-after',
						from: { nodeId: 'Project.sq:Sequence.st:hidden2', portIndex: 0 },
						to: { nodeId: 'Project.sq:Sequence.st:after', portIndex: 0 }
					}
				]
			},
			{
				hiddenNodeIds: new Set(['Project.sq:Sequence.st:hidden1', 'Project.sq:Sequence.st:hidden2'])
			}
		);

		const before = nodes.find((node) => node.id === 'Project.sq:Sequence.st:before');
		const after = nodes.find((node) => node.id === 'Project.sq:Sequence.st:after');
		expect(before?.position.x).toBe(40);
		expect(after?.position.x).toBe(300);
		expect(edges).toContainEqual(
			expect.objectContaining({
				id: 'collapsed_before-hidden1_hidden2-after',
				source: 'Project.sq:Sequence.st:before',
				target: 'Project.sq:Sequence.st:after'
			})
		);
	});

	it('uses the dedicated edge type for loop return links', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:Iterator',
					type: 'IteratorStep',
					label: 'Iterator',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: { originalId: 'Project.sq:Sequence.st:Iterator', isLoop: true }
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:body',
					type: 'SimpleStep',
					label: 'body',
					x: 260,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:Iterator.st:body' }
				}
			],
			links: [
				{
					id: 'body-loop',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator.st:body', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					routing: 'loop-return'
				}
			]
		});

		expect(edges).toContainEqual(
			expect.objectContaining({
				id: 'body-loop',
				type: 'loop-return',
				targetHandle: inputHandleId(1),
				data: expect.objectContaining({
					connectToLoop: true,
					joinX: expect.any(Number),
					laneY: expect.any(Number)
				})
			})
		);
	});

	it('adds a left input port to loop nodes for loop return links', () => {
		const { nodes } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:Iterator',
					type: 'IteratorStep',
					label: 'Iterator',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: { originalId: 'Project.sq:Sequence.st:Iterator', isLoop: true }
				}
			],
			links: []
		});

		const iterator = nodes.find((node) => node.id === 'Project.sq:Sequence.st:Iterator');
		expect(iterator?.data).toEqual(
			expect.objectContaining({
				inputs: 2,
				bottomInputs: 0,
				loopReturnInputIndex: 1
			})
		);
	});

	it('uses a dedicated edge type for loop body links', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:Iterator',
					type: 'IteratorStep',
					label: 'Iterator',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator',
						isLoop: true
					}
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:body',
					type: 'SimpleStep',
					label: 'body',
					x: 260,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:body',
						parentId: 'Project.sq:Sequence.st:Iterator'
					}
				}
			],
			links: [
				{
					id: 'iterator-body',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator.st:body', portIndex: 0 }
				}
			]
		});

		expect(edges).toContainEqual(
			expect.objectContaining({
				id: 'iterator-body',
				type: 'loop-body',
				label: 'LOOP'
			})
		);
	});

	it('routes loop body links below occupied rows when another node blocks the target lane', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:Iterator',
					type: 'IteratorStep',
					label: 'Iterator',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator',
						isLoop: true
					}
				},
				{
					id: 'Project.sq:Sequence.st:blocker',
					type: 'SimpleStep',
					label: 'blocker',
					x: 260,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:blocker' }
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:body',
					type: 'SimpleStep',
					label: 'body',
					x: 520,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:body',
						parentId: 'Project.sq:Sequence.st:Iterator'
					}
				}
			],
			links: [
				{
					id: 'iterator-body',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator.st:body', portIndex: 0 }
				}
			]
		});

		const body = edges.find((edge) => edge.id === 'iterator-body');
		expect(body?.data).toEqual(
			expect.objectContaining({
				laneY: expect.any(Number)
			})
		);
		expect(Number(body?.data?.laneY)).toBeGreaterThan(200 + 40 + 72);
	});

	it('keeps loop body lanes away from loop return buses', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:Iterator',
					type: 'IteratorStep',
					label: 'Iterator',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: { originalId: 'Project.sq:Sequence.st:Iterator', isLoop: true }
				},
				{
					id: 'Project.sq:Sequence.st:blocker',
					type: 'SimpleStep',
					label: 'blocker',
					x: 260,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:blocker' }
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:body',
					type: 'SimpleStep',
					label: 'body',
					x: 520,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:body',
						parentId: 'Project.sq:Sequence.st:Iterator'
					}
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:return',
					type: 'SimpleStep',
					label: 'return',
					x: 780,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:return',
						parentId: 'Project.sq:Sequence.st:Iterator'
					}
				}
			],
			links: [
				{
					id: 'iterator-body',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator.st:body', portIndex: 0 }
				},
				{
					id: 'return-loop',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator.st:return', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					routing: 'loop-return'
				}
			]
		});

		const body = edges.find((edge) => edge.id === 'iterator-body');
		const loopReturn = edges.find((edge) => edge.id === 'return-loop');
		expect(body?.data).toEqual(
			expect.objectContaining({
				laneY: expect.any(Number)
			})
		);
		expect(loopReturn?.data).toEqual(
			expect.objectContaining({
				laneY: expect.any(Number)
			})
		);
		expect(body?.data?.laneY).not.toBe(loopReturn?.data?.laneY);
		expect(Number(body?.data?.laneY)).toBeLessThan(Number(loopReturn?.data?.laneY));
	});

	it('routes multiple loop returns to the loop input with separated lanes', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:Iterator',
					type: 'IteratorStep',
					label: 'Iterator',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: { originalId: 'Project.sq:Sequence.st:Iterator', isLoop: true }
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:first',
					type: 'SimpleStep',
					label: 'first',
					x: 260,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:Iterator.st:first' }
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:second',
					type: 'SimpleStep',
					label: 'second',
					x: 520,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:Iterator.st:second' }
				}
			],
			links: [
				{
					id: 'first-loop',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator.st:first', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					routing: 'loop-return'
				},
				{
					id: 'second-loop',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator.st:second', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					routing: 'loop-return'
				}
			]
		});

		const first = edges.find((edge) => edge.id === 'first-loop');
		const second = edges.find((edge) => edge.id === 'second-loop');
		expect(first?.data).toEqual(expect.objectContaining({ connectToLoop: false }));
		expect(second?.data).toEqual(expect.objectContaining({ connectToLoop: true }));
		expect(first?.data?.joinX).toBe(second?.data?.joinX);
		expect(first?.data?.laneY).toBe(second?.data?.laneY);
	});

	it('draws the loop return bus from the rightmost returning node', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:Iterator',
					type: 'IteratorStep',
					label: 'Iterator',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: { originalId: 'Project.sq:Sequence.st:Iterator', isLoop: true }
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:right',
					type: 'SimpleStep',
					label: 'right',
					x: 780,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:Iterator.st:right' }
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:lower',
					type: 'SimpleStep',
					label: 'lower',
					x: 520,
					y: 420,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:Iterator.st:lower' }
				}
			],
			links: [
				{
					id: 'right-loop',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator.st:right', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					routing: 'loop-return'
				},
				{
					id: 'lower-loop',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator.st:lower', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					routing: 'loop-return'
				}
			]
		});

		const right = edges.find((edge) => edge.id === 'right-loop');
		const lower = edges.find((edge) => edge.id === 'lower-loop');
		expect(right?.data).toEqual(expect.objectContaining({ connectToLoop: true }));
		expect(lower?.data).toEqual(expect.objectContaining({ connectToLoop: false }));
	});

	it('routes outer loop returns below nested loop return buses', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:Iterator',
					type: 'IteratorStep',
					label: 'Iterator',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: { originalId: 'Project.sq:Sequence.st:Iterator', isLoop: true }
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:Iterator1',
					type: 'IteratorStep',
					label: 'Iterator1',
					x: 260,
					y: 200,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:Iterator1',
						parentId: 'Project.sq:Sequence.st:Iterator',
						isLoop: true
					}
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:Iterator1.st:body',
					type: 'SimpleStep',
					label: 'inner body',
					x: 520,
					y: 400,
					inputs: 1,
					outputs: 1,
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:Iterator1.st:body',
						parentId: 'Project.sq:Sequence.st:Iterator.st:Iterator1'
					}
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:outerTail',
					type: 'SimpleStep',
					label: 'outer tail',
					x: 780,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:outerTail',
						parentId: 'Project.sq:Sequence.st:Iterator'
					}
				}
			],
			links: [
				{
					id: 'inner-loop',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator.st:Iterator1.st:body', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator.st:Iterator1', portIndex: 0 },
					routing: 'loop-return'
				},
				{
					id: 'outer-loop',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator.st:outerTail', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					routing: 'loop-return'
				}
			]
		});

		const inner = edges.find((edge) => edge.id === 'inner-loop');
		const outer = edges.find((edge) => edge.id === 'outer-loop');
		expect(Number(outer?.data?.laneY)).toBeGreaterThan(Number(inner?.data?.laneY));
	});

	it('separates overlapping loop return vertical leads', () => {
		const { edges } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:Iterator',
					type: 'IteratorStep',
					label: 'Iterator',
					x: 0,
					y: 0,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: { originalId: 'Project.sq:Sequence.st:Iterator', isLoop: true }
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:Iterator1',
					type: 'IteratorStep',
					label: 'Iterator1',
					x: 260,
					y: 200,
					inputs: 1,
					outputs: 2,
					outputLabels: ['loop', 'done'],
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:Iterator1',
						parentId: 'Project.sq:Sequence.st:Iterator',
						isLoop: true
					}
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:outerTail',
					type: 'SimpleStep',
					label: 'outer tail',
					x: 520,
					y: 200,
					inputs: 1,
					outputs: 1,
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:outerTail',
						parentId: 'Project.sq:Sequence.st:Iterator'
					}
				},
				{
					id: 'Project.sq:Sequence.st:Iterator.st:Iterator1.st:innerTail',
					type: 'SimpleStep',
					label: 'inner tail',
					x: 520,
					y: 400,
					inputs: 1,
					outputs: 1,
					data: {
						originalId: 'Project.sq:Sequence.st:Iterator.st:Iterator1.st:innerTail',
						parentId: 'Project.sq:Sequence.st:Iterator.st:Iterator1'
					}
				}
			],
			links: [
				{
					id: 'inner-loop',
					from: {
						nodeId: 'Project.sq:Sequence.st:Iterator.st:Iterator1.st:innerTail',
						portIndex: 0
					},
					to: { nodeId: 'Project.sq:Sequence.st:Iterator.st:Iterator1', portIndex: 0 },
					routing: 'loop-return'
				},
				{
					id: 'outer-loop',
					from: { nodeId: 'Project.sq:Sequence.st:Iterator.st:outerTail', portIndex: 0 },
					to: { nodeId: 'Project.sq:Sequence.st:Iterator', portIndex: 0 },
					routing: 'loop-return'
				}
			]
		});

		const inner = edges.find((edge) => edge.id === 'inner-loop');
		const outer = edges.find((edge) => edge.id === 'outer-loop');
		expect(inner?.data).toEqual(
			expect.objectContaining({
				sourceLeadOffset: expect.any(Number)
			})
		);
		expect(outer?.data).toEqual(
			expect.objectContaining({
				sourceLeadOffset: expect.any(Number)
			})
		);
		expect(inner?.data?.sourceLeadOffset).not.toBe(outer?.data?.sourceLeadOffset);
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
		const { edges, nodes } = toXyFlow({
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
					outputs: 0,
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

		const returnNode = nodes.find((node) => node.id === 'Project.sq:Sequence.st:return');
		expect(returnNode?.data.outputs).toBe(1);
		expect(edges).toContainEqual(
			expect.objectContaining({
				source: 'Project.sq:Sequence.st:return',
				target: 'Project.sq:Sequence.__response',
				sourceHandle: outputHandleId(0),
				targetHandle: inputHandleId(0),
				class: 'flow-terminal-edge--return',
				type: 'terminal-return',
				zIndex: 2,
				data: expect.objectContaining({ laneY: expect.any(Number) })
			})
		);
	});

	it('groups return paths into a shared response input', () => {
		const { edges, nodes } = toXyFlow({
			id: 'Project.sq:Sequence',
			name: 'Sequence',
			nodes: [
				{
					id: 'Project.sq:Sequence.st:tail',
					type: 'SimpleStep',
					label: 'Tail',
					x: 180,
					y: 0,
					inputs: 1,
					outputs: 1,
					data: { originalId: 'Project.sq:Sequence.st:tail' }
				},
				{
					id: 'Project.sq:Sequence.st:return',
					type: 'ReturnStep',
					label: 'return',
					x: 180,
					y: 200,
					inputs: 1,
					outputs: 0,
					data: {
						classname: 'com.twinsoft.convertigo.beans.steps.ReturnStep',
						originalId: 'Project.sq:Sequence.st:return'
					}
				},
				{
					id: 'Project.sq:Sequence.st:return2',
					type: 'ReturnStep',
					label: 'return',
					x: 440,
					y: 200,
					inputs: 1,
					outputs: 0,
					data: {
						classname: 'com.twinsoft.convertigo.beans.steps.ReturnStep',
						originalId: 'Project.sq:Sequence.st:return2'
					}
				}
			],
			links: []
		});

		const response = nodes.find((node) => node.data.terminalKind === 'response');
		const firstReturn = edges.find((edge) => edge.source === 'Project.sq:Sequence.st:return');
		const secondReturn = edges.find((edge) => edge.source === 'Project.sq:Sequence.st:return2');
		expect(response?.data.inputs).toBe(2);
		expect(edges).toContainEqual(
			expect.objectContaining({
				source: 'Project.sq:Sequence.st:tail',
				target: 'Project.sq:Sequence.__response',
				targetHandle: inputHandleId(0),
				class: 'flow-terminal-edge--response'
			})
		);
		expect(edges).toContainEqual(
			expect.objectContaining({
				source: 'Project.sq:Sequence.st:return',
				target: 'Project.sq:Sequence.__response',
				targetHandle: inputHandleId(1),
				class: 'flow-terminal-edge--return',
				type: 'terminal-return',
				data: expect.objectContaining({ connectToResponse: false })
			})
		);
		expect(edges).toContainEqual(
			expect.objectContaining({
				source: 'Project.sq:Sequence.st:return2',
				target: 'Project.sq:Sequence.__response',
				targetHandle: inputHandleId(1),
				class: 'flow-terminal-edge--return',
				type: 'terminal-return',
				data: expect.objectContaining({ connectToResponse: true })
			})
		);
		expect(firstReturn?.data?.busStartX).toBe(secondReturn?.data?.busStartX);
		expect(Number(firstReturn?.data?.busStartX)).toBeLessThan(Number(firstReturn?.data?.joinX));
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
