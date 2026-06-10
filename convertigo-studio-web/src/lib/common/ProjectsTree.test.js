import { describe, expect, it } from 'vitest';
import { normalizeProjectTreeChildren } from './ProjectsTree.svelte.js';

describe('ProjectsTree', () => {
	it('reuses existing nodes when qname aliases are equivalent', () => {
		const previousField = {
			id: 'Project.sq:Sequence.st:object.field',
			name: '"field"',
			children: [{ id: 'Project.sq:Sequence.st:object.field.child', name: 'child' }]
		};

		const children = normalizeProjectTreeChildren(
			[
				{
					id: 'Project.sq:Sequence.st:object.st:field',
					name: '"field"',
					children: true
				}
			],
			[previousField],
			{
				equivalentIds: (id) =>
					id === 'Project.sq:Sequence.st:object.st:field'
						? [id, 'Project.sq:Sequence.st:object.field']
						: id
							? [id]
							: []
			}
		);

		expect(children[0]).toBe(previousField);
		expect(children[0].id).toBe('Project.sq:Sequence.st:object.st:field');
		expect(children[0].children).toEqual([
			{ id: 'Project.sq:Sequence.st:object.field.child', name: 'child' }
		]);
	});

	it('normalizes labels and names for new nodes', () => {
		const children = normalizeProjectTreeChildren([{ id: 'Project', label: 'Project' }]);

		expect(children[0].name).toBe('Project');
		expect(children[0].label).toBe('Project');
	});
});
