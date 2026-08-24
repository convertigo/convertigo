import { acceptDbo, addDbo, moveDbo } from '$lib/utils/service';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
	affectedDboParentIds,
	areEquivalentDboObjectIds,
	canDropDbo,
	canUseDboDropFallback,
	dboTreeFolderIds,
	equivalentDboObjectIds,
	expandableDboAncestorIds,
	getDboDropAction,
	inferMovedObjectId,
	isDescendantObjectId,
	isNoopSiblingMove,
	mutationDboContextIds,
	mutationDboRefreshIds,
	objectNameFromId,
	performDboDrop,
	renameObjectId,
	shouldStartInlineRename
} from './dnd';

vi.mock('$lib/utils/service', () => ({
	acceptDbo: vi.fn(),
	addDbo: vi.fn(),
	moveDbo: vi.fn()
}));

beforeEach(() => {
	vi.clearAllMocks();
});

describe('Studio DBO drag and drop qnames', () => {
	it('extracts local object names from regular and structured qnames', () => {
		expect(objectNameFromId('Project.sq:Sequence.st:step')).toBe('step');
		expect(objectNameFromId('Project.sq:Sequence.st:object.field')).toBe('field');
		expect(objectNameFromId('Project.sq:Sequence.st:object.st:field')).toBe('field');
		expect(objectNameFromId('Project.sq:Sequence.st:"field1" : ""')).toBe('"field1" : ""');
		expect(objectNameFromId('Project.sq:Sequence.st:object."field1" : ""')).toBe('"field1" : ""');
		expect(objectNameFromId('Project')).toBe('Project');
		expect(objectNameFromId(undefined)).toBe('');
	});

	it('falls back to move when the drag event keeps a tree drop effect to none', () => {
		let writtenDropEffect = 'none';
		const event = {
			ctrlKey: false,
			dataTransfer: {
				effectAllowed: 'move',
				get dropEffect() {
					return 'none';
				},
				set dropEffect(value) {
					writtenDropEffect = value;
				}
			}
		};

		expect(
			getDboDropAction(/** @type {DragEvent} */ (event), {
				type: 'treeData',
				data: { id: 'Project.sq:Sequence.st:field' },
				options: {}
			})
		).toBe('move');
		expect(writtenDropEffect).toBe('move');
	});

	it('does not rename source-backed Flow widgets after a palette insert', () => {
		expect(
			shouldStartInlineRename({
				done: true,
				selectedId: 'Project.Engine.frontends.svelte.routes.home.structure.text',
				selectionSourcePath: 'model/Project/src/routes/+page.flow.svelte',
				projectedSourcePath: 'model/Project/src/routes/+page.flow.svelte',
				payload: { type: 'paletteData', data: {} }
			})
		).toBe(false);
		expect(
			shouldStartInlineRename({
				done: true,
				selectedId: 'Project.sq:Sequence.st:SimpleStep1',
				payload: { type: 'paletteData', data: {} }
			})
		).toBe(true);
	});

	it('rebuilds renamed qnames without leaking the parent path into the new name', () => {
		expect(renameObjectId('Project.sq:Sequence.st:step', 'renamed')).toBe(
			'Project.sq:Sequence.st:renamed'
		);
		expect(renameObjectId('Project.sq:Sequence.st:object.field', 'renamed')).toBe(
			'Project.sq:Sequence.st:object.renamed'
		);
		expect(renameObjectId('Project.sq:Sequence.st:object.st:field', 'renamed')).toBe(
			'Project.sq:Sequence.st:object.st:renamed'
		);
		expect(renameObjectId('Project.sq:Sequence.st:"field1" : ""', 'renamed')).toBe(
			'Project.sq:Sequence.st:renamed'
		);
		expect(renameObjectId('Project.sq:Sequence.st:object."field1" : ""', 'renamed')).toBe(
			'Project.sq:Sequence.st:object.renamed'
		);
		expect(renameObjectId('', 'renamed')).toBe('renamed');
	});

	it('keeps the step folder segment when moving a step inside another step', () => {
		const sourceId = 'Project.sq:Sequence.st:field2';
		const targetId = 'Project.sq:Sequence.st:object';

		expect(
			inferMovedObjectId({
				payload: { type: 'treeData', data: { id: sourceId } },
				target: targetId,
				position: 'inside'
			})
		).toBe('Project.sq:Sequence.st:object.st:field2');
	});

	it('uses source-container qnames for structured JSON/XML children', () => {
		const sourceId = 'Project.sq:Sequence.st:field2';
		const targetId = 'Project.sq:Sequence.st:object';

		expect(
			inferMovedObjectId({
				payload: {
					type: 'treeData',
					data: {
						id: sourceId,
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				},
				target: targetId,
				position: 'inside'
			})
		).toBe('Project.sq:Sequence.st:object.field2');
	});

	it('keeps the step folder segment when moving a step inside a sequence', () => {
		const sourceId = 'Project.sq:Sequence.st:field2';
		const targetId = 'Project.sq:Sequence';

		expect(
			inferMovedObjectId({
				payload: { type: 'treeData', data: { id: sourceId } },
				target: targetId,
				position: 'inside'
			})
		).toBe('Project.sq:Sequence.st:field2');
	});

	it('keeps sibling qname shape for before or after moves', () => {
		const sourceId = 'Project.sq:Sequence.st:field2';
		const targetId = 'Project.sq:Sequence.st:object.st:field1';

		expect(
			inferMovedObjectId({
				payload: { type: 'treeData', data: { id: sourceId } },
				target: targetId,
				position: 'after'
			})
		).toBe('Project.sq:Sequence.st:object.st:field2');
		expect(
			inferMovedObjectId({
				payload: { type: 'treeData', data: { id: sourceId } },
				target: targetId,
				position: 'before'
			})
		).toBe('Project.sq:Sequence.st:object.st:field2');
	});

	it('keeps structured child qname shape for first before or after moves under source containers', () => {
		const sourceId = 'Project.sq:Sequence.st:object.field2';
		const targetId = 'Project.sq:Sequence.st:object.field1';

		expect(
			inferMovedObjectId({
				payload: {
					type: 'treeData',
					data: {
						id: sourceId,
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				},
				target: targetId,
				position: 'first'
			})
		).toBe('Project.sq:Sequence.st:object.field2');
		expect(
			inferMovedObjectId({
				payload: {
					type: 'treeData',
					data: {
						id: sourceId,
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				},
				target: targetId,
				position: 'before'
			})
		).toBe('Project.sq:Sequence.st:object.field2');
		expect(
			inferMovedObjectId({
				payload: {
					type: 'treeData',
					data: {
						id: sourceId,
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				},
				target: targetId,
				position: 'after'
			})
		).toBe('Project.sq:Sequence.st:object.field2');
	});

	it('keeps structured child qname shape when the sibling target is a regular step', () => {
		const sourceId = 'Project.sq:Sequence.st:object.field2';
		const targetId = 'Project.sq:Sequence.st:object.st:if';

		expect(
			inferMovedObjectId({
				payload: {
					type: 'treeData',
					data: {
						id: sourceId,
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				},
				target: targetId,
				position: 'after'
			})
		).toBe('Project.sq:Sequence.st:object.field2');
	});

	it('keeps JSON field labels with colons when moving structured children', () => {
		const sourceId = 'Project.sq:Sequence.st:"field2" : ""';
		const targetId = 'Project.sq:Sequence.st:object."field1" : ""';

		expect(
			inferMovedObjectId({
				payload: {
					type: 'treeData',
					data: {
						id: sourceId,
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				},
				target: 'Project.sq:Sequence.st:object',
				position: 'inside'
			})
		).toBe('Project.sq:Sequence.st:object."field2" : ""');
		expect(
			inferMovedObjectId({
				payload: {
					type: 'treeData',
					data: {
						id: sourceId,
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				},
				target: targetId,
				position: 'after'
			})
		).toBe('Project.sq:Sequence.st:object."field2" : ""');
	});

	it('maps equivalent source-container and regular step child qnames', () => {
		expect(equivalentDboObjectIds('Project.sq:Sequence.st:object.field2')).toEqual([
			'Project.sq:Sequence.st:object.field2',
			'Project.sq:Sequence.st:object.st:field2'
		]);
		expect(equivalentDboObjectIds('Project.sq:Sequence.st:object.st:field2')).toEqual([
			'Project.sq:Sequence.st:object.st:field2',
			'Project.sq:Sequence.st:object.field2'
		]);
		expect(
			areEquivalentDboObjectIds(
				'Project.sq:Sequence.st:object.field2',
				'Project.sq:Sequence.st:object.st:field2'
			)
		).toBe(true);
	});

	it('maps equivalent structured child qnames when field labels contain colons', () => {
		expect(equivalentDboObjectIds('Project.sq:Sequence.st:object."field2" : ""')).toEqual([
			'Project.sq:Sequence.st:object."field2" : ""',
			'Project.sq:Sequence.st:object.st:"field2" : ""'
		]);
		expect(equivalentDboObjectIds('Project.sq:Sequence.st:object.st:"field2" : ""')).toEqual([
			'Project.sq:Sequence.st:object.st:"field2" : ""',
			'Project.sq:Sequence.st:object."field2" : ""'
		]);
		expect(
			areEquivalentDboObjectIds(
				'Project.sq:Sequence.st:object."field2" : ""',
				'Project.sq:Sequence.st:object.st:"field2" : ""'
			)
		).toBe(true);
	});

	it('detects descendants across equivalent structured qname shapes', () => {
		expect(
			isDescendantObjectId(
				'Project.sq:Sequence.st:object.field2.st:child',
				'Project.sq:Sequence.st:object.st:field2'
			)
		).toBe(true);
		expect(
			isDescendantObjectId(
				'Project.sq:Sequence.st:object.st:field2.st:child',
				'Project.sq:Sequence.st:object.field2'
			)
		).toBe(true);
		expect(
			isDescendantObjectId(
				'Project.sq:Sequence.st:object.field2',
				'Project.sq:Sequence.st:object.st:field2'
			)
		).toBe(false);
	});

	it('refuses before inside or after drops on the same structured child object', async () => {
		const accepted = await canDropDbo({
			payload: {
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.st:field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				}
			},
			target: 'Project.sq:Sequence.st:object.field2',
			position: 'after',
			dropAction: 'move'
		});

		expect(accepted).toBe(false);
		expect(acceptDbo).not.toHaveBeenCalled();
	});

	it('tracks both old and new parents when a step moves into a nested lane', () => {
		expect(
			affectedDboParentIds({
				done: true,
				selectedId: 'Project.sq:Sequence.st:object.st:field2',
				target: 'Project.sq:Sequence.st:object.st:field1',
				parentId: 'Project.sq:Sequence.st:object',
				position: 'after',
				payload: {
					type: 'treeData',
					data: { id: 'Project.sq:Sequence.st:field2' }
				}
			})
		).toEqual([
			'Project.sq:Sequence.st:object',
			'Project:sq',
			'Project.sq:Sequence:st',
			'Project.sq:Sequence.st:object:st',
			'Project.sq:Sequence'
		]);
	});

	it('keeps nested parents expanded for a moved selected step', () => {
		expect(expandableDboAncestorIds('Project.sq:Sequence.st:object.field2')).toEqual([
			'Project',
			'Project:sq',
			'Project.sq:Sequence:st',
			'Project.sq:Sequence.st:object',
			'Project.sq:Sequence'
		]);
	});

	it('builds a shared mutation context for cross-parent flow to tree refreshes', () => {
		expect(
			mutationDboContextIds({
				done: true,
				selectedId: 'Project.sq:Sequence.st:object.field2',
				target: 'Project.sq:Sequence.st:object.field1',
				parentId: 'Project.sq:Sequence.st:object',
				position: 'after',
				payload: {
					type: 'treeData',
					data: { id: 'Project.sq:Sequence.st:field2' }
				}
			})
		).toEqual([
			'Project.sq:Sequence.st:object',
			'Project:sq',
			'Project.sq:Sequence:st',
			'Project.sq:Sequence',
			'Project.sq:Sequence.st:object:st',
			'Project'
		]);
	});

	it('keeps explicit previous parent context returned by the move service', () => {
		expect(
			mutationDboContextIds({
				done: true,
				selectedId: 'Project.sq:Sequence.st:object.field2',
				target: 'Project.sq:Sequence.st:object.field1',
				parentId: 'Project.sq:Sequence.st:object',
				previousParentId: 'Project.sq:Sequence',
				position: 'after',
				payload: {
					type: 'treeData',
					data: { id: 'Project.sq:Sequence.st:field2' }
				}
			})
		).toEqual(
			expect.arrayContaining([
				'Project.sq:Sequence.st:object',
				'Project.sq:Sequence',
				'Project:sq',
				'Project.sq:Sequence:st',
				'Project'
			])
		);
	});

	it('refreshes only exact parents after a projected frontend insertion', () => {
		expect(
			mutationDboRefreshIds({
				done: true,
				selectedId: 'Project.FlowEngine.frontends.svelte.routes.home.structure.card.spinner',
				target: 'Project.FlowEngine.frontends.svelte.routes.home.structure.card.title',
				parentId: 'Project.FlowEngine.frontends.svelte.routes.home.structure.card',
				position: 'before',
				selectionSourcePath: '/workspace/projects/Project/model/src/routes/+page.flow.svelte',
				projectedSourcePath: '/workspace/projects/Project/model/src/routes/+page.flow.svelte',
				payload: { type: 'paletteData', data: { type: 'FrontendBlock' } }
			})
		).toEqual(['Project.FlowEngine.frontends.svelte.routes.home.structure.card']);
	});

	it('refreshes both exact parents after a projected frontend cross-parent move', () => {
		expect(
			mutationDboRefreshIds({
				done: true,
				selectedId: 'Project.FlowEngine.frontends.svelte.routes.home.structure.right.spinner',
				target: 'Project.FlowEngine.frontends.svelte.routes.home.structure.right.title',
				parentId: 'Project.FlowEngine.frontends.svelte.routes.home.structure.right',
				previousParentId: 'Project.FlowEngine.frontends.svelte.routes.home.structure.left',
				position: 'before',
				selectionSourcePath: '/workspace/projects/Project/model/src/routes/+page.flow.svelte',
				payload: { type: 'treeData', data: { id: 'spinner' } }
			})
		).toEqual([
			'Project.FlowEngine.frontends.svelte.routes.home.structure.right',
			'Project.FlowEngine.frontends.svelte.routes.home.structure.left'
		]);
	});

	it('keeps the source container context for structured child reorders', () => {
		expect(
			mutationDboContextIds({
				done: true,
				selectedId: 'Project.sq:Sequence.st:object.field2',
				target: 'Project.sq:Sequence.st:object.field1',
				parentId: 'Project.sq:Sequence.st:object',
				position: 'after',
				payload: {
					type: 'treeData',
					data: {
						id: 'Project.sq:Sequence.st:object.field2',
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				}
			})
		).toEqual(
			expect.arrayContaining([
				'Project.sq:Sequence.st:object',
				'Project:sq',
				'Project.sq:Sequence:st',
				'Project.sq:Sequence',
				'Project'
			])
		);
	});

	it('keeps rename/delete parent context even when the changed object no longer exists', () => {
		expect(
			mutationDboContextIds({
				done: true,
				id: 'Project.sq:Sequence.st:renamed',
				selectedId: 'Project.sq:Sequence.st:renamed',
				target: 'Project.sq:Sequence.st:field',
				position: 'inside',
				payload: {
					type: 'renameData',
					data: { id: 'Project.sq:Sequence.st:field' }
				}
			})
		).toEqual(['Project:sq', 'Project.sq:Sequence:st', 'Project.sq:Sequence', 'Project']);
	});

	it('maps qname typed segments to virtual tree folders', () => {
		expect(dboTreeFolderIds('Project.sq:Sequence.st:object.field2')).toEqual([
			'Project:sq',
			'Project.sq:Sequence:st'
		]);
	});

	it('keeps the engine parent id when an add result provides it', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(addDbo).mockResolvedValue({
			done: true,
			id: 'Project.sq:CreatedSequence',
			parentId: 'Project'
		});

		const payload = {
			type: 'paletteData',
			data: {
				id: 'com.twinsoft.convertigo.beans.sequences.GenericSequence',
				classname: 'com.twinsoft.convertigo.beans.sequences.GenericSequence'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project:sq',
			position: 'inside',
			dropAction: 'copy'
		});

		expect(result).toMatchObject({
			done: true,
			selectedId: 'Project.sq:CreatedSequence',
			parentId: 'Project'
		});
		expect(result.affectedParentIds).toContain('Project');
		expect(result.affectedParentIds).not.toContain('Project:sq:st');
	});

	it('does not append an exact after move when the fallback parent refuses it', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: false });

		const result = await performDboDrop({
			payload: {
				type: 'treeData',
				data: { id: 'Project.sq:Sequence.st:field2' }
			},
			target: 'Project.sq:Sequence.st:object.st:field1',
			position: 'after',
			dropAction: 'move',
			fallbackTarget: 'Project.sq:Sequence.st:object',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: false,
			refused: true,
			target: 'Project.sq:Sequence.st:object.st:field1',
			position: 'after'
		});
		expect(acceptDbo).toHaveBeenCalledTimes(1);
		expect(acceptDbo).toHaveBeenCalledWith('move', 'Project.sq:Sequence.st:object', 'inside', {
			type: 'treeData',
			data: { id: 'Project.sq:Sequence.st:field2' }
		});
		expect(moveDbo).not.toHaveBeenCalled();
	});

	it('allows structured child sibling reorders without a redundant accept call', async () => {
		const accepted = await canDropDbo({
			payload: {
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				}
			},
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'first',
			dropAction: 'move'
		});

		expect(accepted).toBe(true);
		expect(acceptDbo).not.toHaveBeenCalled();
	});

	it('keeps selected structured child id after a first move without engine id echo', async () => {
		vi.mocked(moveDbo).mockResolvedValue({
			done: true
		});

		const result = await performDboDrop({
			payload: {
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				}
			},
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'first',
			dropAction: 'move'
		});

		expect(result).toMatchObject({
			done: true,
			selectedId: 'Project.sq:Sequence.st:object.field2',
			parentId: 'Project.sq:Sequence.st:object',
			previousParentId: 'Project.sq:Sequence.st:object',
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'first'
		});
		expect(acceptDbo).not.toHaveBeenCalled();
		expect(moveDbo).toHaveBeenCalledWith(
			'Project.sq:Sequence.st:object.field1',
			'first',
			{
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				}
			},
			expect.objectContaining({ silentError: expect.any(Function) })
		);
	});

	it('retries direct structured sibling reorders with equivalent target qnames', async () => {
		vi.mocked(moveDbo).mockResolvedValueOnce({ done: false }).mockResolvedValueOnce({
			done: true,
			id: 'Project.sq:Sequence.st:object.field2'
		});

		const payload = {
			type: 'treeData',
			data: {
				id: 'Project.sq:Sequence.st:object.field2',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'after',
			dropAction: 'move'
		});

		expect(result).toMatchObject({
			done: true,
			selectedId: 'Project.sq:Sequence.st:object.field2',
			target: 'Project.sq:Sequence.st:object.field1',
			parentId: 'Project.sq:Sequence.st:object',
			position: 'after'
		});
		expect(acceptDbo).not.toHaveBeenCalled();
		expect(moveDbo).toHaveBeenNthCalledWith(
			1,
			'Project.sq:Sequence.st:object.field1',
			'after',
			payload,
			expect.objectContaining({ silentError: expect.any(Function) })
		);
		expect(moveDbo).toHaveBeenNthCalledWith(
			2,
			'Project.sq:Sequence.st:object.st:field1',
			'after',
			payload,
			{}
		);
	});

	it('keeps failed direct equivalent precise drops anchored to the requested target', async () => {
		vi.mocked(moveDbo).mockResolvedValue({ done: false });

		const payload = {
			type: 'treeData',
			data: {
				id: 'Project.sq:Sequence.st:object.field2',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'after',
			dropAction: 'move'
		});

		expect(result).toMatchObject({
			done: false,
			target: 'Project.sq:Sequence.st:object.field1',
			parentId: 'Project.sq:Sequence.st:object',
			position: 'after'
		});
		expect(moveDbo).toHaveBeenCalledTimes(2);
	});

	it('retries an invalid inside move as an after move when a fallback is provided', async () => {
		vi.mocked(acceptDbo).mockResolvedValueOnce({ accept: false });
		vi.mocked(moveDbo).mockResolvedValue({
			done: true,
			id: 'Project.sq:Sequence.st:field2'
		});

		const result = await performDboDrop({
			payload: {
				type: 'treeData',
				data: { id: 'Project.sq:Sequence.st:field2' }
			},
			target: 'Project.sq:Sequence.st:if',
			position: 'inside',
			dropAction: 'move',
			fallbackTarget: 'Project.sq:Sequence.st:if',
			fallbackPosition: 'after'
		});

		expect(result).toMatchObject({
			done: true,
			retried: true,
			target: 'Project.sq:Sequence.st:if',
			position: 'after'
		});
		expect(moveDbo).toHaveBeenCalledWith(
			'Project.sq:Sequence.st:if',
			'after',
			{
				type: 'treeData',
				data: { id: 'Project.sq:Sequence.st:field2' }
			},
			{}
		);
	});

	it('adds through the parent then moves to the precise sibling position', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(addDbo).mockResolvedValue({
			done: true,
			id: 'Project.sq:Sequence.st:object.field2'
		});
		vi.mocked(moveDbo).mockResolvedValue({ done: true });

		const payload = {
			type: 'paletteData',
			data: {
				id: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'after',
			dropAction: 'copy',
			fallbackTarget: 'Project.sq:Sequence.st:object',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: true,
			retried: true,
			selectedId: 'Project.sq:Sequence.st:object.field2',
			target: 'Project.sq:Sequence.st:object.field1',
			payload,
			position: 'after'
		});
		expect(acceptDbo).toHaveBeenCalledTimes(1);
		expect(acceptDbo).toHaveBeenCalledWith(
			'copy',
			'Project.sq:Sequence.st:object',
			'inside',
			payload
		);
		expect(addDbo).toHaveBeenCalledWith('Project.sq:Sequence.st:object', 'inside', payload, {});
		expect(moveDbo).toHaveBeenCalledWith(
			'Project.sq:Sequence.st:object.field1',
			'after',
			{
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				},
				options: {}
			},
			expect.objectContaining({ silentError: expect.any(Function) })
		);
	});

	it('inserts Flow frontend palette blocks at the precise sibling in one mutation', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(addDbo).mockResolvedValue({
			done: true,
			id: 'Project.Engine.frontends.svelte.routes.home.structure.spinner'
		});

		const payload = {
			type: 'paletteData',
			data: {
				type: 'FrontendBlock',
				id: 'frontend:spinner',
				classname: 'standard.spinner'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.Engine.frontends.svelte.routes.home.structure.title',
			position: 'before',
			dropAction: 'copy',
			fallbackTarget: 'Project.Engine.frontends.svelte.routes.home.structure',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: true,
			selectedId: 'Project.Engine.frontends.svelte.routes.home.structure.spinner',
			target: 'Project.Engine.frontends.svelte.routes.home.structure.title',
			position: 'before'
		});
		expect(acceptDbo).toHaveBeenCalledTimes(1);
		expect(acceptDbo).toHaveBeenCalledWith(
			'copy',
			'Project.Engine.frontends.svelte.routes.home.structure.title',
			'before',
			payload
		);
		expect(addDbo).toHaveBeenCalledTimes(1);
		expect(addDbo).toHaveBeenCalledWith(
			'Project.Engine.frontends.svelte.routes.home.structure.title',
			'before',
			payload,
			{}
		);
		expect(moveDbo).not.toHaveBeenCalled();
	});

	it('moves Flow frontend nodes to a precise sibling in one mutation', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(moveDbo).mockResolvedValue({
			done: true,
			id: 'Project.Engine.frontends.svelte.routes.home.structure.spinner'
		});

		const payload = {
			type: 'treeData',
			data: {
				id: 'Project.Engine.frontends.svelte.routes.other.structure.spinner',
				classname: 'com.twinsoft.convertigo.beans.flow.FlowVirtualObject'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.Engine.frontends.svelte.routes.home.structure.title',
			position: 'after',
			dropAction: 'move',
			fallbackTarget: 'Project.Engine.frontends.svelte.routes.home.structure',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: true,
			selectedId: 'Project.Engine.frontends.svelte.routes.home.structure.spinner',
			target: 'Project.Engine.frontends.svelte.routes.home.structure.title',
			position: 'after'
		});
		expect(acceptDbo).toHaveBeenCalledTimes(1);
		expect(moveDbo).toHaveBeenCalledTimes(1);
		expect(moveDbo).toHaveBeenCalledWith(
			'Project.Engine.frontends.svelte.routes.home.structure.title',
			'after',
			payload,
			{}
		);
	});

	it('preserves the engine parent id when a composed add uses a virtual folder target', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(addDbo).mockResolvedValue({
			done: true,
			id: 'Project.sq:CreatedSequence',
			parentId: 'Project'
		});
		vi.mocked(moveDbo).mockResolvedValue({
			done: true,
			id: 'Project.sq:CreatedSequence',
			parentId: 'Project'
		});

		const payload = {
			type: 'paletteData',
			data: {
				id: 'com.twinsoft.convertigo.beans.sequences.GenericSequence',
				classname: 'com.twinsoft.convertigo.beans.sequences.GenericSequence'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.sq:ExistingSequence',
			position: 'after',
			dropAction: 'copy',
			fallbackTarget: 'Project:sq',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: true,
			retried: true,
			selectedId: 'Project.sq:CreatedSequence',
			target: 'Project.sq:ExistingSequence',
			parentId: 'Project',
			position: 'after'
		});
		expect(result.affectedParentIds).toContain('Project');
		expect(result.affectedParentIds).not.toContain('Project:sq:st');
	});

	it('moves through the parent then reorders a precise cross-parent move', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(moveDbo).mockResolvedValueOnce({ done: true }).mockResolvedValueOnce({ done: true });

		const payload = {
			type: 'treeData',
			data: {
				id: 'Project.sq:Sequence.st:field2',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'first',
			dropAction: 'move',
			fallbackTarget: 'Project.sq:Sequence.st:object',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: true,
			retried: true,
			selectedId: 'Project.sq:Sequence.st:object.field2',
			target: 'Project.sq:Sequence.st:object.field1',
			previousParentId: 'Project.sq:Sequence',
			position: 'first'
		});
		expect(acceptDbo).toHaveBeenCalledTimes(1);
		expect(acceptDbo).toHaveBeenCalledWith(
			'move',
			'Project.sq:Sequence.st:object',
			'inside',
			payload
		);
		expect(result.affectedParentIds).toEqual([
			'Project.sq:Sequence.st:object',
			'Project:sq',
			'Project.sq:Sequence:st',
			'Project.sq:Sequence',
			'Project.sq:Sequence.st:object:st'
		]);
		expect(moveDbo).toHaveBeenNthCalledWith(
			1,
			'Project.sq:Sequence.st:object',
			'inside',
			payload,
			{}
		);
		expect(moveDbo).toHaveBeenNthCalledWith(
			2,
			'Project.sq:Sequence.st:object.field1',
			'first',
			{
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				},
				options: {}
			},
			expect.objectContaining({ silentError: expect.any(Function) })
		);
	});

	it('composes a precise cross-parent move instead of trusting exact acceptance', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(moveDbo).mockResolvedValueOnce({ done: true }).mockResolvedValueOnce({ done: true });

		const payload = {
			type: 'treeData',
			data: {
				id: 'Project.sq:Sequence.st:field2',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'after',
			dropAction: 'move',
			fallbackTarget: 'Project.sq:Sequence.st:object',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: true,
			retried: true,
			selectedId: 'Project.sq:Sequence.st:object.field2',
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'after'
		});
		expect(acceptDbo).toHaveBeenCalledTimes(1);
		expect(acceptDbo).toHaveBeenCalledWith(
			'move',
			'Project.sq:Sequence.st:object',
			'inside',
			payload
		);
		expect(moveDbo).toHaveBeenNthCalledWith(
			1,
			'Project.sq:Sequence.st:object',
			'inside',
			payload,
			{}
		);
		expect(moveDbo).toHaveBeenNthCalledWith(
			2,
			'Project.sq:Sequence.st:object.field1',
			'after',
			{
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				},
				options: {}
			},
			expect.objectContaining({ silentError: expect.any(Function) })
		);
	});

	it('moves through the parent then reorders a precise cross-parent before move with the engine id echo', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(moveDbo)
			.mockResolvedValueOnce({
				done: true,
				id: 'Project.sq:Sequence.st:object.field2',
				parentId: 'Project.sq:Sequence.st:object',
				previousParentId: 'Project.sq:Sequence'
			})
			.mockResolvedValueOnce({
				done: true,
				id: 'Project.sq:Sequence.st:object.field2',
				parentId: 'Project.sq:Sequence.st:object',
				previousParentId: 'Project.sq:Sequence.st:object'
			});

		const payload = {
			type: 'treeData',
			data: {
				id: 'Project.sq:Sequence.st:field2',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'before',
			dropAction: 'move',
			fallbackTarget: 'Project.sq:Sequence.st:object',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: true,
			retried: true,
			selectedId: 'Project.sq:Sequence.st:object.field2',
			target: 'Project.sq:Sequence.st:object.field1',
			parentId: 'Project.sq:Sequence.st:object',
			previousParentId: 'Project.sq:Sequence',
			position: 'before'
		});
		expect(acceptDbo).toHaveBeenCalledTimes(1);
		expect(acceptDbo).toHaveBeenCalledWith(
			'move',
			'Project.sq:Sequence.st:object',
			'inside',
			payload
		);
		expect(moveDbo).toHaveBeenNthCalledWith(
			1,
			'Project.sq:Sequence.st:object',
			'inside',
			payload,
			{}
		);
		expect(moveDbo).toHaveBeenNthCalledWith(
			2,
			'Project.sq:Sequence.st:object.field1',
			'before',
			{
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				},
				options: {}
			},
			expect.objectContaining({ silentError: expect.any(Function) })
		);
	});

	it('retries composed precise moves with equivalent structured sibling targets', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(moveDbo)
			.mockResolvedValueOnce({
				done: true,
				id: 'Project.sq:Sequence.st:object.field2'
			})
			.mockResolvedValueOnce({ done: false })
			.mockResolvedValueOnce({ done: true });

		const payload = {
			type: 'treeData',
			data: {
				id: 'Project.sq:Sequence.st:field2',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'after',
			dropAction: 'move',
			fallbackTarget: 'Project.sq:Sequence.st:object',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: true,
			retried: true,
			selectedId: 'Project.sq:Sequence.st:object.field2',
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'after'
		});
		expect(moveDbo).toHaveBeenNthCalledWith(
			2,
			'Project.sq:Sequence.st:object.field1',
			'after',
			expect.any(Object),
			expect.objectContaining({ silentError: expect.any(Function) })
		);
		expect(moveDbo).toHaveBeenNthCalledWith(
			3,
			'Project.sq:Sequence.st:object.st:field1',
			'after',
			{
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				},
				options: {}
			},
			{}
		);
	});

	it('normalizes structured child ids echoed with a step folder during composed moves', async () => {
		vi.mocked(acceptDbo).mockResolvedValue({ accept: true });
		vi.mocked(moveDbo)
			.mockResolvedValueOnce({
				done: true,
				id: 'Project.sq:Sequence.st:object.st:field2'
			})
			.mockResolvedValueOnce({ done: true });

		const payload = {
			type: 'treeData',
			data: {
				id: 'Project.sq:Sequence.st:field2',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
			},
			options: {}
		};
		const result = await performDboDrop({
			payload,
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'after',
			dropAction: 'move',
			fallbackTarget: 'Project.sq:Sequence.st:object',
			fallbackPosition: 'inside'
		});

		expect(result).toMatchObject({
			done: true,
			retried: true,
			selectedId: 'Project.sq:Sequence.st:object.field2',
			target: 'Project.sq:Sequence.st:object.field1',
			position: 'after'
		});
		expect(moveDbo).toHaveBeenNthCalledWith(
			2,
			'Project.sq:Sequence.st:object.field1',
			'after',
			{
				type: 'treeData',
				data: {
					id: 'Project.sq:Sequence.st:object.field2',
					classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
				},
				options: {}
			},
			expect.objectContaining({ silentError: expect.any(Function) })
		);
	});

	it('detects a no-op move after the current previous sibling', () => {
		expect(
			isNoopSiblingMove({
				payload: {
					type: 'treeData',
					data: { id: 'Project.sq:Sequence.st:field2' }
				},
				target: 'Project.sq:Sequence.st:field1',
				position: 'after',
				sourceSiblingIds: [
					'Project.sq:Sequence.st:field1',
					'Project.sq:Sequence.st:field2',
					'Project.sq:Sequence.st:field3'
				]
			})
		).toBe(true);
	});

	it('detects no-op sibling moves when source ids use equivalent structured shapes', () => {
		expect(
			isNoopSiblingMove({
				payload: {
					type: 'treeData',
					data: {
						id: 'Project.sq:Sequence.st:object.st:field2',
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				},
				target: 'Project.sq:Sequence.st:object.field1',
				position: 'after',
				sourceSiblingIds: [
					'Project.sq:Sequence.st:object.field1',
					'Project.sq:Sequence.st:object.field2'
				]
			})
		).toBe(true);
	});

	it('detects a no-op move after itself', () => {
		expect(
			isNoopSiblingMove({
				payload: {
					type: 'treeData',
					data: { id: 'Project.sq:Sequence.st:field2' }
				},
				target: 'Project.sq:Sequence.st:field2',
				position: 'after',
				sourceSiblingIds: [
					'Project.sq:Sequence.st:field1',
					'Project.sq:Sequence.st:field2',
					'Project.sq:Sequence.st:field3'
				]
			})
		).toBe(true);
	});

	it('detects a no-op move before the next sibling', () => {
		expect(
			isNoopSiblingMove({
				payload: {
					type: 'treeData',
					data: { id: 'Project.sq:Sequence.st:field2' }
				},
				target: 'Project.sq:Sequence.st:field3',
				position: 'before',
				sourceSiblingIds: [
					'Project.sq:Sequence.st:field1',
					'Project.sq:Sequence.st:field2',
					'Project.sq:Sequence.st:field3'
				]
			})
		).toBe(true);
	});

	it('detects a no-op move before the first sibling', () => {
		expect(
			isNoopSiblingMove({
				payload: {
					type: 'treeData',
					data: { id: 'Project.sq:Sequence.st:field1' }
				},
				target: 'Project.sq:Sequence.st:field1',
				position: 'first',
				sourceSiblingIds: ['Project.sq:Sequence.st:field1', 'Project.sq:Sequence.st:field2']
			})
		).toBe(true);
	});

	it('keeps real sibling moves enabled', () => {
		expect(
			isNoopSiblingMove({
				payload: {
					type: 'treeData',
					data: { id: 'Project.sq:Sequence.st:field3' }
				},
				target: 'Project.sq:Sequence.st:field1',
				position: 'before',
				sourceSiblingIds: [
					'Project.sq:Sequence.st:field1',
					'Project.sq:Sequence.st:field2',
					'Project.sq:Sequence.st:field3'
				]
			})
		).toBe(false);
	});

	it('uses fallback drops only when the execution path can actually use them', () => {
		const sourcePayload = {
			type: 'treeData',
			data: {
				id: 'Project.sq:Sequence.st:field2',
				classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
			}
		};
		expect(
			canUseDboDropFallback({
				payload: sourcePayload,
				target: 'Project.sq:Sequence.st:if',
				position: 'inside',
				dropAction: 'move',
				fallbackTarget: 'Project.sq:Sequence.st:if',
				fallbackPosition: 'after'
			})
		).toBe(true);
		expect(
			canUseDboDropFallback({
				payload: sourcePayload,
				target: 'Project.sq:Sequence.st:object.field1',
				position: 'after',
				dropAction: 'move',
				fallbackTarget: 'Project.sq:Sequence.st:object',
				fallbackPosition: 'inside'
			})
		).toBe(true);
		expect(
			canUseDboDropFallback({
				payload: {
					type: 'treeData',
					data: {
						id: 'Project.sq:Sequence.st:object.field2',
						classname: 'com.twinsoft.convertigo.beans.steps.JsonFieldStep'
					}
				},
				target: 'Project.sq:Sequence.st:object.field1',
				position: 'after',
				dropAction: 'move',
				fallbackTarget: 'Project.sq:Sequence.st:object',
				fallbackPosition: 'inside'
			})
		).toBe(false);
	});
});
