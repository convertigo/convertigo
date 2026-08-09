import { browser } from '$app/environment';
import Instances from '$lib/admin/Instances.svelte.js';
import { getUrl } from '$lib/utils/service';

const DEFAULT_TOPICS = ['*'];

/**
 * Build the same-origin Admin SSE endpoint. Query parameters are required
 * because the browser EventSource API cannot add XSRF or instance headers.
 *
 * @param {string[] | string} topics
 * @param {{ baseUrl?: string, xsrfToken?: string, instance?: string, origin?: string, clientId?: string }} [options]
 */
export function adminEventsUrl(topics = DEFAULT_TOPICS, options = {}) {
	const values = Array.isArray(topics) ? topics : String(topics || '').split(',');
	const selectedTopics = values.map((value) => String(value).trim()).filter(Boolean);
	const baseUrl = options.baseUrl ?? `${getUrl()}events.Subscribe`;
	const origin = options.origin ?? (browser ? window.location.origin : 'http://localhost');
	const url = new URL(baseUrl, origin);
	url.searchParams.set(
		'topics',
		(selectedTopics.length ? selectedTopics : DEFAULT_TOPICS).join(',')
	);

	const xsrfToken =
		options.xsrfToken ?? (browser ? (localStorage.getItem('x-xsrf-token') ?? 'Fetch') : '');
	if (xsrfToken) {
		url.searchParams.set('__xsrfToken', xsrfToken);
	}

	const instance = options.instance ?? Instances.current;
	if (instance) {
		url.searchParams.set('__instance', instance);
	}
	if (options.clientId) {
		url.searchParams.set('client', options.clientId);
	}
	return url.href;
}

/**
 * @param {MessageEvent | string | null | undefined} message
 * @returns {{ id: string, topic: string, timestamp: number, instance: string, payload: Record<string, any> } | null}
 */
export function parseAdminEvent(message) {
	const data = typeof message === 'string' ? message : message?.data;
	if (typeof data !== 'string' || !data.trim()) {
		return null;
	}
	try {
		const event = JSON.parse(data);
		if (!event || typeof event !== 'object' || typeof event.topic !== 'string') {
			return null;
		}
		return {
			id: String(event.id ?? ''),
			topic: event.topic,
			timestamp: Number(event.timestamp ?? 0),
			instance: String(event.instance ?? ''),
			payload:
				event.payload && typeof event.payload === 'object' && !Array.isArray(event.payload)
					? event.payload
					: {}
		};
	} catch {
		return null;
	}
}

/**
 * Subscribe to generic Admin events. Returning a disposer keeps consumers
 * independent from the transport lifecycle and leaves room for future replay.
 *
 * @param {string[] | string} topics
 * @param {(event: ReturnType<typeof parseAdminEvent>) => void} onEvent
 * @param {{ onError?: (event: Event) => void, eventSourceFactory?: (url: string, options: EventSourceInit) => EventSource, urlOptions?: Parameters<typeof adminEventsUrl>[1] }} [options]
 */
export function subscribeAdminEvents(topics, onEvent, options = {}) {
	if (!browser && !options.eventSourceFactory) {
		return () => {};
	}
	const eventSourceFactory =
		options.eventSourceFactory ?? ((url, init) => new EventSource(url, init));
	const clientId =
		options.urlOptions?.clientId ??
		globalThis.crypto?.randomUUID?.() ??
		`admin-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
	const source = eventSourceFactory(adminEventsUrl(topics, { ...options.urlOptions, clientId }), {
		withCredentials: true
	});
	source.onmessage = (message) => {
		const event = parseAdminEvent(message);
		if (event) {
			onEvent(event);
		}
	};
	if (options.onError) {
		source.onerror = options.onError;
	}
	return () => source.close();
}
