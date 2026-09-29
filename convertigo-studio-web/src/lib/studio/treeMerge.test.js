import { describe, expect, it, vi } from 'vitest';

vi.mock('$lib/utils/service', () => ({ call: vi.fn() }));

const { describeOperation, indexTreeMerge } = await import('./treeMerge.svelte.js');

describe('treeMerge', () => {
	it('indexes the conflicts and the changes of a merge for the tree', () => {
		const merge = indexTreeMerge({
			merging: true,
			theirs: 'theirs',
			unresolved: 2,
			conflicts: [
				{
					id: 'property:p:1:comment',
					kind: 'property',
					name: 'Button',
					objectId: 'App.Application.NgxApp.pg:Page.Button',
					choices: ['mine', 'theirs']
				},
				{
					id: 'removed:p:2',
					kind: 'removed-by-me',
					name: 'Toast',
					parentId: 'App.Application.NgxApp.pg:Page',
					choices: ['mine', 'theirs']
				}
			],
			changes: [
				{
					status: 'modified',
					origin: 'theirs',
					name: 'Title',
					objectId: 'App.Application.NgxApp.pg:Page.Title'
				},
				{
					status: 'added',
					origin: 'theirs',
					name: 'Menu',
					objectId: 'App.Application.NgxApp.mn:Menu',
					parentId: 'App.Application.NgxApp:mn'
				}
			]
		});
		expect(merge.merging).toBe(true);
		expect(merge.conflictsById['App.Application.NgxApp.pg:Page.Button']).toHaveLength(1);
		expect(merge.changesById['App.Application.NgxApp.pg:Page.Title'].status).toBe('modified');
		// the objects the tree does not show yet are shown where they go
		expect(merge.ghosts['App.Application.NgxApp.pg:Page'].map((g) => g.name)).toEqual(['Toast']);
		expect(merge.ghosts['App.Application.NgxApp:mn'].map((g) => g.name)).toEqual(['Menu']);
		expect(merge.ancestors['App']).toBe(true);
		expect(merge.ancestors['App.Application.NgxApp.pg:Page']).toBe(true);
	});

	it('keeps the operation the conflicts come from', () => {
		const merge = indexTreeMerge({
			merging: true,
			kind: 'rebase',
			ours: 'origin/main',
			theirs: 'abc1234 Change the title',
			operation: { kind: 'rebase', branch: 'main', onto: 'origin/main', step: 2, steps: 3 },
			unresolved: 0
		});
		expect(merge.kind).toBe('rebase');
		expect(merge.ours).toBe('origin/main');
		expect(merge.operation?.step).toBe(2);
	});

	it('names each operation stopped', () => {
		const rebase = describeOperation({
			kind: 'rebase',
			branch: 'main',
			onto: 'origin/main',
			step: 2,
			steps: 3,
			commit: { id: 'abc1234', subject: 'Change the title', author: 'me' }
		});
		expect(rebase.title).toBe('Rebasing main onto origin/main');
		expect(rebase.detail).toBe('commit 2/3: abc1234 Change the title');
		expect(rebase.complete).toBe('Continue the rebase');
		expect(describeOperation({ kind: 'merge', ours: 'main', theirs: 'feature' }).title).toBe(
			'Merging feature into main'
		);
		expect(
			describeOperation({ kind: 'cherry-pick', commit: { id: 'def5678', subject: 'Fix', author: 'me' } })
				.title
		).toBe('Cherry-picking def5678');
		expect(describeOperation({ kind: 'revert' }).complete).toBe('Complete the revert');
		expect(describeOperation({ kind: 'conflicts' }).complete).toBe('Mark as resolved');
		expect(describeOperation(null).icon).toBe('mdi:source-merge');
	});
});
