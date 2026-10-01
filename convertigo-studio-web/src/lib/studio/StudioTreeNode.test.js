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
