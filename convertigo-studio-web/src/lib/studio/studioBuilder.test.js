import { describe, expect, it, vi } from 'vitest';
import {
	refreshStudioBuilder,
	resolveStudioBuilder,
	runStudioBuilderCommand,
	studioBuilderOperation
} from './studioBuilder';

const builder = {
	id: 'custom',
	target: 'Project.Owner',
	commands: {
		serve: { id: 'provider.custom.serve', enabled: true, payload: { custom: 'contract' } },
		build: { id: 'provider.custom.build', enabled: false }
	}
};

describe('Studio builder discovery', () => {
	it('retains the existing NGX transport when its execute capability is offered', async () => {
		const children = vi.fn();
		const result = await resolveStudioBuilder('Project', {
			menu: vi.fn().mockResolvedValue({ menu: { items: [{ id: 'frontend.execute' }] } }),
			children
		});
		expect(result.transport).toBe('ngx');
		expect(children).not.toHaveBeenCalled();
	});
	it('discovers custom names on immediate objects without guessing an engine QName', async () => {
		const menu = vi.fn(async (id) => ({
			menu: { builders: id === 'Project.Owner' ? [builder] : [] }
		}));
		const result = await resolveStudioBuilder('Project', {
			menu,
			children: async () => ({ children: [{ id: 'Project:se' }, { id: 'Project.Owner' }] })
		});
		expect(result).toEqual({ transport: 'actions', builders: [builder] });
		expect(menu.mock.calls).toEqual([['Project'], ['Project.Owner']]);
	});
	it('accepts project-level descriptors and multiple builders', async () => {
		const builders = [builder, { ...builder, id: 'second' }];
		expect(
			await resolveStudioBuilder('Project', {
				menu: async () => ({ builders }),
				children: vi.fn()
			})
		).toEqual({ transport: 'actions', builders });
	});
	it('does not connect an unsupported project to the NGX socket', async () => {
		expect(
			await resolveStudioBuilder('Project', {
				menu: async () => ({ menu: { items: [] } }),
				children: async () => ({ children: [] })
			})
		).toEqual({ transport: 'none', builders: [] });
	});
	it('propagates discovery failures instead of silently falling back to NGX', async () => {
		await expect(
			resolveStudioBuilder('Project', {
				menu: async () => {
					throw new Error('offline');
				},
				children: vi.fn()
			})
		).rejects.toThrow('offline');
	});
});

describe('Opaque builder commands', () => {
	it('attachment is read-only and never starts a stopped server', () => {
		expect(studioBuilderOperation({ state: { serving: false } }, 'attach')).toBeNull();
		expect(studioBuilderOperation({ state: { serving: true } }, 'attach')).toBe('open');
		expect(studioBuilderOperation({ state: { serving: true } }, 'serve')).toBe('open');
		expect(studioBuilderOperation({ state: { serving: false } }, 'serve')).toBe('serve');
	});
	it('forwards the advertised target and action unchanged', async () => {
		const run = vi.fn().mockResolvedValue({ ok: true, openUrl: '/viewer/' });
		expect(await runStudioBuilderCommand(builder, 'serve', run)).toEqual({
			ok: true,
			openUrl: '/viewer/'
		});
		expect(run).toHaveBeenCalledWith(builder.target, builder.commands.serve);
	});
	it.each(['build', 'watch'])('never invents an unavailable %s command', async (command) => {
		const run = vi.fn();
		await expect(runStudioBuilderCommand(builder, command, run)).rejects.toThrow('unavailable');
		expect(run).not.toHaveBeenCalled();
	});
	it.each([
		{ ok: false, error: { message: 'compile error' } },
		{ isError: true, message: 'compile error' }
	])('does not report a failed result as a completed build', async (result) => {
		await expect(runStudioBuilderCommand(builder, 'serve', async () => result)).rejects.toThrow(
			'compile error'
		);
	});
	it('refreshes state by both provider id and owner', async () => {
		const updated = { ...builder, state: { serving: true } };
		expect(
			await refreshStudioBuilder(builder, async () => ({ menu: { builders: [updated] } }))
		).toEqual(updated);
		expect(
			await refreshStudioBuilder(builder, async () => ({
				builders: [{ ...updated, target: 'Other' }]
			}))
		).toBeNull();
	});
});
