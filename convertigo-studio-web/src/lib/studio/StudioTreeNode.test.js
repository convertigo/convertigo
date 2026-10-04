import { render } from 'svelte/server';
import { describe, expect, it } from 'vitest';
import StudioTreeNode from './StudioTreeNode.svelte';

describe('Studio tree rename capability', () => {
	it.each([true, false, undefined])(
		'honors capability %s when an inline rename is requested',
		(canRename) => {
			const id = 'Project.engine.provider_projection';
			const { body } = render(StudioTreeNode, {
				props: {
					node: { id, label: 'Provider object', canRename },
					depth: 1,
					selectedId: id,
					renameTargetId: id
				}
			});
			expect(body.includes('aria-label="Rename object"')).toBe(canRename !== false);
		}
	);
});

describe('Studio tree tag presentation', () => {
	it('colors the complete tag group label from its presentation metadata', () => {
		const { body } = render(StudioTreeNode, {
			props: {
				node: {
					id: 'tag:project:test',
					label: 'Test environment',
					tagGroup: true,
					presentation: { color: '#16a34a' },
					children: []
				}
			}
		});
		expect(body).toMatch(/style="color: #16a34a;"[^>]*>Test environment<\/span>/);
	});

	it.each([false, undefined])('does not color an object as a tag group (%s)', (tagGroup) => {
		const { body } = render(StudioTreeNode, {
			props: {
				node: {
					id: 'Project.object',
					label: 'Object',
					tagGroup,
					presentation: { color: '#16a34a' }
				}
			}
		});
		expect(body).not.toContain('color: #16a34a');
	});
});
