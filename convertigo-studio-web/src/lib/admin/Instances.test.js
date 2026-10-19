import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('#lib/utils/service.js', () => ({
	call: vi.fn(),
	getUrl: () => '/convertigo/admin/services/'
}));

const pods = [
	{ instanceId: 'convertigo-pod-a', routingId: 'route-a' },
	{ instanceId: 'convertigo-pod-b', routingId: 'route-b' }
];
let Instances;
let call;

beforeEach(async () => {
	vi.resetModules();
	({ default: Instances } = await import('./Instances.svelte.js'));
	call = vi.mocked((await import('#lib/utils/service.js')).call);
	call.mockReset();
	call.mockResolvedValue({ storeMode: 'redis', instances: pods, localRoutingId: 'route-a' });
});

const response = (headers) => ({
	headers: new Headers(typeof headers === 'string' ? { 'X-C8O-Instance': headers } : headers)
});

describe('Administration instance routing', () => {
	it('keeps the pod names as metadata and sends only the routing address', async () => {
		await Instances.refresh();
		expect(call).toHaveBeenCalledWith('engine.ListInstances', {}, { instance: '' });
		expect(Instances.instances).toEqual(pods);
		expect(Instances.activeName).toBe('convertigo-pod-a');
		const headers = {};
		Instances.apply(headers);
		expect(headers).toEqual({ 'x-c8o-instance': 'route-a' });
	});

	it('distinguishes the chosen target from the last pod that answered', async () => {
		await Instances.refresh();
		Instances.current = 'route-b';
		expect(Instances.activeName).toBe('convertigo-pod-a');
		const revision = Instances.apply({});
		Instances.update(response('route-b'), revision);
		expect(Instances.activeName).toBe('convertigo-pod-b');
	});

	it('ignores delayed replies from requests sent before a selection change', async () => {
		await Instances.refresh();
		const previous = Instances.apply({});
		Instances.current = 'route-b';
		Instances.update(response('route-a'), previous);
		expect(Instances.current).toBe('route-b');
		Instances.update(response('route-b'));
		expect(Instances.activeName).toBe('convertigo-pod-b');
	});

	it('keeps a target whose discovery entry expired among the options instead of switching to the local pod', async () => {
		await Instances.refresh();
		Instances.current = 'route-b';
		call.mockResolvedValue({ storeMode: 'redis', instances: [pods[0]], localRoutingId: 'route-a' });
		await Instances.refresh(true);
		expect(Instances.instances).toEqual([pods[0]]);
		expect(Instances.current).toBe('route-b');
		expect(Instances.options).toEqual([
			{ value: 'route-a', text: 'convertigo-pod-a' },
			{ value: 'route-b', text: 'route-b' }
		]);
	});

	it('does not lose the selection when discovery fails', async () => {
		await Instances.refresh();
		Instances.current = 'route-b';
		call.mockResolvedValue({ error: 'Authentication failure' });
		await Instances.refresh(true);
		expect(Instances.current).toBe('route-b');
		expect(Instances.instances).toEqual(pods);
	});

	it('uses the opaque selection in the EventSource query', async () => {
		await Instances.refresh();
		Instances.current = 'route-b';
		const { adminEventsUrl } = await import('./adminEvents.js');
		const url = new URL(adminEventsUrl('*', { origin: 'http://localhost', xsrfToken: 'token' }));
		expect(url.searchParams.get('__instance')).toBe('route-b');
		expect(url.href).not.toContain('convertigo-pod');
	});

	it('keeps the chosen routing address when forwarding fails', async () => {
		await Instances.refresh();
		Instances.current = 'route-b';
		expect(
			Instances.update(
				response({
					'X-C8O-Instance': 'route-b',
					'X-C8O-Instance-Unreachable': 'route-b',
					'X-C8O-Instance-Local': 'route-a',
					'X-C8O-Instance-Error': 'SocketTimeoutException: Connect timed out'
				})
			)
		).toBe(true);
		expect(Instances.current).toBe('route-b');
		expect(Instances.activeName).toBe('convertigo-pod-a');
		expect(Instances.unreachable).toEqual({
			instance: 'route-b',
			local: 'route-a',
			cause: 'SocketTimeoutException: Connect timed out'
		});
	});

	it('clears the report when the chosen instance answers again', () => {
		Instances.current = 'route-b';
		Instances.update(response({ 'X-C8O-Instance-Unreachable': 'route-b' }));
		expect(Instances.update(response('route-b'))).toBe(false);
		expect(Instances.unreachable).toBe(null);
	});

	it('clears the report when another instance is chosen', () => {
		Instances.current = 'route-b';
		Instances.update(response({ 'X-C8O-Instance-Unreachable': 'route-b' }));
		Instances.current = 'route-a';
		expect(Instances.unreachable).toBe(null);
	});

	it('ignores unreachable reports from requests sent before a selection change', async () => {
		await Instances.refresh();
		const revision = Instances.apply({});
		Instances.current = 'route-b';
		expect(Instances.update(response({ 'X-C8O-Instance-Unreachable': 'route-a' }), revision)).toBe(
			false
		);
		expect(Instances.current).toBe('route-b');
		expect(Instances.unreachable).toBe(null);
	});
	it.each([
		{ error: 'Instance unreachable' },
		{ error: 'Server unreachable', offline: true },
		{},
		null
	])('preserves the selector state when refreshing fails: %j', async (failure) => {
		await Instances.refresh();
		Instances.current = 'route-b';
		Instances.update(
			response({ 'X-C8O-Instance-Unreachable': 'route-b', 'X-C8O-Instance-Local': 'route-a' })
		);
		call.mockResolvedValue(failure);
		await Instances.refresh(true);
		expect(Instances.storeMode).toBe('redis');
		expect(Instances.instances).toEqual(pods);
		expect(Instances.current).toBe('route-b');
		expect(Instances.unreachable?.instance).toBe('route-b');
		expect(Instances.loading).toBe(false);
	});

	it('does not mistake a local or unidentified response for recovery of the chosen target', async () => {
		await Instances.refresh();
		Instances.current = 'route-b';
		Instances.update(response({ 'X-C8O-Instance-Unreachable': 'route-b' }));
		for (const headers of [{ 'X-C8O-Instance': 'route-a' }, {}]) {
			expect(Instances.update(response(headers))).toBe(Boolean(headers['X-C8O-Instance']));
			expect(Instances.current).toBe('route-b');
			expect(Instances.unreachable?.instance).toBe('route-b');
		}
		expect(Instances.update(response('route-b'))).toBe(false);
		expect(Instances.unreachable).toBe(null);
	});

	it('ignores a late outage response after the user explicitly selects the local instance', async () => {
		await Instances.refresh();
		Instances.current = 'route-a';
		expect(Instances.update(response({ 'X-C8O-Instance-Unreachable': 'route-b' }))).toBe(true);
		expect(Instances.current).toBe('route-a');
		expect(Instances.unreachable).toBe(null);
	});
});
