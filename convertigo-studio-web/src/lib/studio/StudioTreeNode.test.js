import { render } from 'svelte/server';
import { describe, expect, it } from 'vitest';
import { debugSession } from './debugSession.svelte.js';
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

describe('project YAML format', () => {
	it.each(['legacy', 'standard'])('shows %s in the project tooltip', (yamlFormat) => {
		const { body } = render(StudioTreeNode, {
			props: { node: { id: 'Demo', label: 'Demo', yamlFormat }, onTreeAction: () => {} }
		});
		expect(body).toContain(`YAML format: ${yamlFormat}`);
	});
});

describe('logical pause marker', () => {
	it('uses the frame target without changing the tree selection and clears on resume', () => {
		const node = {
			id: 'Project.sq:Run.flow.first',
			label: 'first',
			debugTarget: { url: 'flow-block:opaque#/nodes/0' }
		};
		const state = {
			attached: true,
			stopped: true,
			serial: 1,
			breakOnExceptions: false,
			sources: [],
			frames: [{ url: node.debugTarget.url, line: 3, function: 'first', kind: 'flow' }]
		};
		try {
			debugSession.state = state;
			debugSession.frameIndex = 0;
			const paused = render(StudioTreeNode, { props: { node, selectedId: 'Project.sq:Run' } }).body;
			expect(paused).toContain('data-debug-paused="true"');
			expect(paused).toContain('title="Paused block"');
			expect(paused).not.toMatch(/class="[^"]*studio-tree-node__row--selected/);
			debugSession.state = { ...state, stopped: false };
			expect(render(StudioTreeNode, { props: { node } }).body).not.toContain(
				'data-debug-paused="true"'
			);
		} finally {
			debugSession.state = null;
			debugSession.frameIndex = 0;
		}
	});
});
