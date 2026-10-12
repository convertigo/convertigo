import { describe, expect, it } from 'vitest';
import { flowImplementationRoot } from './flowRoot';

const catalog = 'Lib.FlowEngine.catalog.blocks.provider_Lib';
const trail = [
	{ id: 'Lib', label: 'Lib' },
	{ id: `${catalog}.namespace_codex`, label: 'codex' },
	{ id: `${catalog}.namespace_codex.block_codex_setup`, label: 'setup', flowType: 'codex.setup' },
	{
		id: `${catalog}.namespace_codex.block_codex_setup.implementation`,
		label: 'Implementation',
		flowType: 'flow'
	},
	{
		id: `${catalog}.namespace_codex.block_codex_setup.implementation.mcpUrl`,
		label: 'local.mcpUrl = ...',
		flowType: 'set'
	}
];

describe('flowImplementationRoot', () => {
	it('finds the Flow implementation holding the selection, named as a Flow calls its block', () => {
		expect(flowImplementationRoot(trail)).toEqual({
			id: `${catalog}.namespace_codex.block_codex_setup.implementation`,
			name: 'codex.setup'
		});
		expect(flowImplementationRoot(trail.slice(0, 4))?.name).toBe('codex.setup');
	});

	it('ignores a code implementation and a node only named implementation', () => {
		expect(
			flowImplementationRoot([
				...trail.slice(0, 3),
				{ id: `${trail[2].id}.implementation`, label: 'Implementation', flowType: 'javascript' }
			])
		).toBeNull();
		expect(
			flowImplementationRoot([
				{ id: 'Project.sq:Flow', label: 'Flow' },
				{ id: 'Project.sq:Flow.implementation', label: 'implementation', flowType: 'set' }
			])
		).toBeNull();
		expect(flowImplementationRoot([])).toBeNull();
	});
});
