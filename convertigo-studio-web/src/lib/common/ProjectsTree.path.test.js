import { call } from '#lib/utils/service.js';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createProjectTree, readProjectTreeChildren } from './ProjectsTree.svelte.js';

vi.mock('#lib/utils/service.js', () => ({ call: vi.fn() }));

const ids = ['P', 'P.FlowEngine', 'P.FlowEngine.catalog'];

beforeEach(() => {
	vi.clearAllMocks();
});

describe('checkPath', () => {
	it('opens the branches toward a descendant with one request', async () => {
		vi.mocked(call).mockResolvedValue({
			P: [{ id: 'P.FlowEngine', name: 'FlowEngine', children: true }],
			'P.FlowEngine': [{ id: 'P.FlowEngine.catalog', name: 'Catalog', children: true }],
			'P.FlowEngine.catalog': [
				{ id: 'P.FlowEngine.catalog.blocks', name: 'Blocks', children: true }
			]
		});
		const { checkPath } = createProjectTree();
		/** @type {any} */
		const project = { id: 'P', name: 'P', children: true };

		await checkPath(project, ids);

		expect(call).toHaveBeenCalledTimes(1);
		expect(call).toHaveBeenCalledWith('studio.treeview.Get', { ids: JSON.stringify(ids) });
		const catalog = project.children[0].children[0];
		expect(catalog.id).toBe('P.FlowEngine.catalog');
		expect(catalog.children.map((child) => child.id)).toEqual(['P.FlowEngine.catalog.blocks']);
	});

	it('loads the node alone when the answer misses it', async () => {
		vi.mocked(call)
			.mockResolvedValueOnce({ isError: true, error: { message: 'Unknown object' } })
			.mockResolvedValueOnce({ children: [{ id: 'P.FlowEngine', name: 'FlowEngine' }] });
		const { checkPath } = createProjectTree();
		/** @type {any} */
		const project = { id: 'P', name: 'P', children: true };

		await checkPath(project, ids);

		expect(call).toHaveBeenLastCalledWith('studio.treeview.Get', { id: 'P' });
		expect(project.children.map((child) => child.id)).toEqual(['P.FlowEngine']);
	});
});

describe('readProjectTreeChildren', () => {
	it('gives the children a tree reads to another view, unless grouped by tags', async () => {
		vi.mocked(call).mockResolvedValue({ children: [{ id: 'Q.FlowEngine', name: 'FlowEngine' }] });
		await createProjectTree().checkChildren({ id: 'Q', name: 'Q', children: true });
		expect(await readProjectTreeChildren('Q')).toEqual([
			{ id: 'Q.FlowEngine', name: 'FlowEngine' }
		]);

		await createProjectTree({ parameters: () => ({ tagsGrouped: true }) }).checkChildren({
			id: 'R',
			name: 'R',
			children: true
		});
		expect(readProjectTreeChildren('R')).toBeUndefined();
	});
});
