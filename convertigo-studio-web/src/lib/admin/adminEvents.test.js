import { describe, expect, it, vi } from 'vitest';
import { adminEventsUrl, parseAdminEvent, subscribeAdminEvents } from './adminEvents';

describe('Admin events', () => {
	it('builds an EventSource-compatible XSRF and sticky instance URL', () => {
		const url = new URL(
			adminEventsUrl(['projects.changed', 'admin.*'], {
				baseUrl: '/convertigo/admin/services/events.Subscribe',
				origin: 'https://convertigo.example',
				xsrfToken: 'xsrf-value',
				instance: 'pod-a',
				clientId: 'studio-tab-a'
			})
		);

		expect(url.origin).toBe('https://convertigo.example');
		expect(url.pathname).toBe('/convertigo/admin/services/events.Subscribe');
		expect(url.searchParams.get('topics')).toBe('projects.changed,admin.*');
		expect(url.searchParams.get('__xsrfToken')).toBe('xsrf-value');
		expect(url.searchParams.get('__instance')).toBe('pod-a');
		expect(url.searchParams.get('client')).toBe('studio-tab-a');
	});

	it('normalizes a valid envelope and ignores invalid data', () => {
		expect(
			parseAdminEvent(
				JSON.stringify({
					id: 12,
					topic: 'projects.changed',
					timestamp: 42,
					instance: 'pod-a',
					payload: { project: 'sample' }
				})
			)
		).toEqual({
			id: '12',
			topic: 'projects.changed',
			timestamp: 42,
			instance: 'pod-a',
			payload: { project: 'sample' }
		});
		expect(parseAdminEvent('{invalid')).toBeNull();
		expect(parseAdminEvent(JSON.stringify({ payload: {} }))).toBeNull();
	});

	it('delivers parsed events and closes its transport', () => {
		const close = vi.fn();
		const source = /** @type {EventSource} */ (
			/** @type {unknown} */ ({ close, onmessage: null, onerror: null })
		);
		const onEvent = vi.fn();
		let requestedUrl = '';
		const eventSourceFactory = vi.fn((url) => {
			requestedUrl = String(url);
			return source;
		});
		const dispose = subscribeAdminEvents('projects.changed', onEvent, {
			eventSourceFactory,
			urlOptions: {
				baseUrl: '/events',
				origin: 'https://convertigo.example',
				xsrfToken: 'token',
				clientId: 'test-client'
			}
		});
		expect(new URL(requestedUrl).searchParams.get('client')).toBe('test-client');

		source.onmessage?.(
			/** @type {MessageEvent} */ ({
				data: JSON.stringify({ topic: 'projects.changed', payload: {} })
			})
		);
		expect(onEvent).toHaveBeenCalledOnce();
		dispose();
		expect(close).toHaveBeenCalledOnce();
	});
});
