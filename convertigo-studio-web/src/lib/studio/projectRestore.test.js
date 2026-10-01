import { readFileSync } from 'node:fs';
import { describe, expect, it, vi } from 'vitest';

const source = readFileSync(
	new URL('../../routes/studio/[[qname]]/+page.svelte', import.meta.url),
	'utf8'
);

// Execute the actual page handlers: regression checks must cover their wiring,
// not a second implementation that can keep passing when Reload forgets it.
function handler(name, env) {
	const start = source.indexOf(`async function ${name}(`);
	const end = source.indexOf('\n\t}', start) + '\n\t}'.length;
	expect(start).toBeGreaterThan(-1);
	return Function('env', `with (env) { return (${source.slice(start, end)}); }`)(env);
}

function environment() {
	const env = {
		selectedProjectName: 'Project',
		projectActionBusy: '',
		projectHistoryBusy: false,
		projectSnapshotTimers: new Map(),
		propertiesRefreshSerial: 4,
		historyReload: { projectName: 'Other', serial: 7 },
		call: vi.fn().mockResolvedValue({}),
		Projects: { refresh: vi.fn().mockResolvedValue() },
		refreshStudioProject: vi.fn().mockResolvedValue(),
		clearProjectDirty: vi.fn(),
		markProjectDirty: vi.fn(),
		callProjectHistory: vi.fn().mockResolvedValue({ done: true }),
		refreshFrontendAuthoringReference: vi.fn(),
		refreshStudioViews: vi.fn()
	};
	env.refreshRestoredProject = handler('refreshRestoredProject', env);
	return env;
}

function expectReconciled(env) {
	expect(env.refreshStudioProject).toHaveBeenCalledWith('Project');
	expect(env.propertiesRefreshSerial).toBe(5);
	expect(env.refreshFrontendAuthoringReference).toHaveBeenCalledOnce();
	expect(env.historyReload).toEqual({ projectName: 'Project', serial: 8 });
	expect(env.refreshStudioViews).toHaveBeenCalledWith({ tree: false, flow: true });
}

describe('Project restoration reconciliation', () => {
	it('Reload reuses the tree reconciliation even if selection changes while loading', async () => {
		const env = environment();
		env.call.mockImplementation(async () => {
			env.selectedProjectName = 'Other';
			return {};
		});
		await handler('reloadSelectedProject', env)();
		expect(env.call).toHaveBeenCalledWith('projects.Reload', { projectName: 'Project' });
		expect(env.callProjectHistory).toHaveBeenCalledWith('Project', 'state');
		expect(env.clearProjectDirty).toHaveBeenCalledWith('Project');
		expectReconciled(env);
		expect(env.projectActionBusy).toBe('');
	});

	it.each(['undo', 'redo'])('%s uses the same selection and property refresh', async (action) => {
		const env = environment();
		await handler('undoProjectChange', env)(action);
		expect(env.callProjectHistory).toHaveBeenCalledWith('Project', action);
		expect(env.markProjectDirty).toHaveBeenCalledWith('Project', { snapshot: false });
		expectReconciled(env);
		expect(env.projectHistoryBusy).toBe(false);
	});

	it('a failed Reload does not signal a restored project', async () => {
		const env = environment();
		env.call.mockRejectedValue(new Error('reload failed'));
		await expect(handler('reloadSelectedProject', env)()).rejects.toThrow('reload failed');
		expect(env.clearProjectDirty).not.toHaveBeenCalled();
		expect(env.historyReload.serial).toBe(7);
		expect(env.projectActionBusy).toBe('');
	});
});
