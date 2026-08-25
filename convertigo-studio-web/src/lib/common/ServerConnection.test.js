import { describe, expect, it } from 'vitest';
import {
	createServerConnectionTracker,
	isServerUnavailableStatus
} from './ServerConnection.svelte';

describe('Server connection tracking', () => {
	it('classifies reverse proxy availability failures', () => {
		expect(isServerUnavailableStatus(502)).toBe(true);
		expect(isServerUnavailableStatus(503)).toBe(true);
		expect(isServerUnavailableStatus(504)).toBe(true);
		expect(isServerUnavailableStatus(500)).toBe(false);
		expect(isServerUnavailableStatus(401)).toBe(false);
	});

	it('reports only connectivity transitions', () => {
		const connection = createServerConnectionTracker();

		expect(connection.markUnavailable(503, 'Service Unavailable')).toBe(true);
		expect(connection.unavailable).toBe(true);
		expect(connection.status).toBe(503);
		expect(connection.statusText).toBe('Service Unavailable');

		expect(connection.markUnavailable(502, 'Bad Gateway')).toBe(false);
		expect(connection.status).toBe(502);
		expect(connection.markReachable()).toBe(true);
		expect(connection.unavailable).toBe(false);
		expect(connection.markReachable()).toBe(false);
	});
});
