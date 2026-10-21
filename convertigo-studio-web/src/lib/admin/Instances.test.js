import { call } from '$lib/utils/service';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('$lib/utils/service', () => ({ call: vi.fn() }));

const { default: Instances } = await import('./Instances.svelte');

const response = (headers) => ({ headers: new Headers(headers) });

describe('Instance selection', () => {
	beforeEach(async () => {
		vi.mocked(call).mockReset();
		Instances.current = 'pod-a';
		vi.mocked(call).mockResolvedValueOnce({
			storeMode: 'redis',
			instances: [{ instanceId: 'pod-a' }, { instanceId: 'pod-b' }]
		});
		await Instances.refresh(true);
		Instances.current = 'pod-b';
		Instances.update(
			response({ 'X-C8O-Instance-Unreachable': 'pod-b', 'X-C8O-Instance-Local': 'pod-a' })
		);
	});

	it('keeps the chosen instance and reports it when the forward fails', () => {
		Instances.current = 'pod-b';
		const unreachable = Instances.update(
			response({
				'X-C8O-Instance': 'pod-b',
				'X-C8O-Instance-Unreachable': 'pod-b',
				'X-C8O-Instance-Local': 'pod-a',
				'X-C8O-Instance-Error': 'SocketTimeoutException: Connect timed out'
			})
		);
		expect(unreachable).toBe(true);
		expect(Instances.current).toBe('pod-b');
		expect(Instances.unreachable).toEqual({
			instance: 'pod-b',
			local: 'pod-a',
			cause: 'SocketTimeoutException: Connect timed out'
		});
	});

	it('clears the report once the chosen instance answers again', () => {
		expect(Instances.update(response({ 'X-C8O-Instance': 'pod-b' }))).toBe(false);
		expect(Instances.unreachable).toBe(null);
	});

	it('clears the report when another instance is chosen', () => {
		Instances.update(
			response({ 'X-C8O-Instance-Unreachable': 'pod-b', 'X-C8O-Instance-Local': 'pod-a' })
		);
		Instances.current = 'pod-a';
		expect(Instances.unreachable).toBe(null);
	});

	it.each([
		{ error: 'Instance unreachable', instanceUnreachable: true },
		{ error: 'Server unreachable', offline: true },
		{},
		null
	])('preserves the selector state when refreshing fails: %j', async (failure) => {
		vi.mocked(call).mockResolvedValueOnce({
			storeMode: 'redis',
			instances: [{ instanceId: 'pod-a' }, { instanceId: 'pod-b' }]
		});
		await Instances.refresh(true);
		Instances.current = 'pod-b';
		Instances.update(
			response({ 'X-C8O-Instance-Unreachable': 'pod-b', 'X-C8O-Instance-Local': 'pod-a' })
		);
		vi.mocked(call).mockResolvedValueOnce(failure);
		await Instances.refresh(true);
		expect(Instances.storeMode).toBe('redis');
		expect(Instances.options).toEqual(['pod-a', 'pod-b']);
		expect(Instances.current).toBe('pod-b');
		expect(Instances.unreachable?.instance).toBe('pod-b');
		expect(Instances.loading).toBe(false);
	});

	it('keeps the expired target among the options after a successful discovery refresh', async () => {
		vi.mocked(call).mockResolvedValueOnce({
			storeMode: 'redis',
			instances: [{ instanceId: 'pod-a' }]
		});
		await Instances.refresh(true);
		expect(Instances.instances).toEqual([{ instanceId: 'pod-a' }]);
		expect(Instances.options).toEqual(['pod-a', 'pod-b']);
		expect(Instances.current).toBe('pod-b');
	});

	it('does not mistake a local or unidentified response for recovery of the chosen target', () => {
		for (const headers of [{ 'X-C8O-Instance': 'pod-a' }, {}]) {
			expect(Instances.update(response(headers))).toBe(Boolean(headers['X-C8O-Instance']));
			expect(Instances.current).toBe('pod-b');
			expect(Instances.unreachable?.instance).toBe('pod-b');
		}
		expect(Instances.update(response({ 'X-C8O-Instance': 'pod-b' }))).toBe(false);
		expect(Instances.unreachable).toBe(null);
		Instances.current = 'pod-a';
	});

	it('ignores a late outage response after the user explicitly selects the local instance', () => {
		Instances.current = 'pod-a';
		expect(Instances.update(response({ 'X-C8O-Instance-Unreachable': 'pod-b' }))).toBe(true);
		expect(Instances.current).toBe('pod-a');
		expect(Instances.unreachable).toBe(null);
	});
});
