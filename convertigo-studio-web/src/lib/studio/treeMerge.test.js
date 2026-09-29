import { describe, expect, it, vi } from 'vitest';

vi.mock('$lib/utils/service', () => ({ call: vi.fn() }));

const { indexTreeMerge } = await import('./treeMerge.svelte.js');

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
});
