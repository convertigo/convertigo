import { describe, expect, it, vi } from 'vitest';

vi.mock('$lib/utils/service', () => ({ call: vi.fn() }));

const { indexTreeDiff, projectOfNode } = await import('./treeDiff.svelte.js');

describe('treeDiff', () => {
	it('indexes the changes for the nodes of the tree', () => {
		const diff = indexTreeDiff({
			repository: true,
			commit: { shortId: 'a460515' },
			changes: [
				{ status: 'modified', id: 'App.Application.NgxApp.pg:Page.Header', properties: [] },
				{ status: 'added', id: 'App.Application.NgxApp.pg:Page.Header.ToolBar' },
				{
					status: 'removed',
					id: 'App.Application.NgxApp.pg:Page.Content~removed~p:12',
					parentId: 'App.Application.NgxApp.pg:Page.Content',
					objectParentId: 'App.Application.NgxApp.pg:Page.Content',
					name: 'List'
				}
			]
		});
		expect(diff.counts).toEqual({ added: 1, modified: 1, removed: 1 });
		expect(diff.byId['App.Application.NgxApp.pg:Page.Header.ToolBar'].status).toBe('added');
		expect(diff.ghosts['App.Application.NgxApp.pg:Page.Content'].map((c) => c.name)).toEqual([
			'List'
		]);
		// the nodes down to the changes contain some
		expect(diff.ancestors['App']).toBe(true);
		expect(diff.ancestors['App.Application.NgxApp:pg']).toBe(true);
		expect(diff.ancestors['App.Application.NgxApp.pg:Page']).toBe(true);
		expect(diff.ancestors['App.Application.NgxApp.pg:Page.Content']).toBe(true);
		// a node changed is not its own ancestor, its parent changed is one of the next
		expect(diff.ancestors['App.Application.NgxApp.pg:Page.Header.ToolBar']).toBeUndefined();
		expect(diff.ancestors['App.Application.NgxApp.pg:Page.Header']).toBe(true);
	});

	it('names the project of a node', () => {
		expect(projectOfNode('App.Application.NgxApp.pg:Page')).toBe('App');
		expect(projectOfNode('App:sq')).toBe('App');
		expect(projectOfNode('App//readme.md')).toBe('App');
		expect(projectOfNode('App')).toBe('App');
	});
});
