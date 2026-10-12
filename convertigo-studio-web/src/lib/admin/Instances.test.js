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

const response = (routingId) => ({ headers: new Headers({ 'X-C8O-Instance': routingId }) });

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

	it('refreshes independently of a vanished target and explicitly selects the responding pod', async () => {
		await Instances.refresh();
		Instances.current = 'route-b';
		call.mockResolvedValue({ storeMode: 'redis', instances: [pods[0]], localRoutingId: 'route-a' });
		await Instances.refresh(true);
		expect(Instances.current).toBe('route-a');
		expect(Instances.activeName).toBe('convertigo-pod-a');
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
});
