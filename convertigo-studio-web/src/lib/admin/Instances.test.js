import { describe, expect, it, vi } from 'vitest';

vi.mock('$lib/utils/service', () => ({ call: vi.fn() }));

const { default: Instances } = await import('./Instances.svelte');

const response = (headers) => ({ headers: new Headers(headers) });

describe('Instance selection', () => {
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
});
