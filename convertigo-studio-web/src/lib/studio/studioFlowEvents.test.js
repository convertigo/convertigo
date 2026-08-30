import { describe, expect, it } from 'vitest';
import { flowBrowserPreview, flowSourceReveal } from './studioFlowEvents';

/** @param {string} topic @param {Record<string, any>} payload */
function event(topic, payload) {
	return { id: 'event', topic, timestamp: 1, instance: 'test', payload };
}

describe('Studio Flow events', () => {
	it('opens only the selected project development viewer', () => {
		const browserEvent = event('flow.browser.open', {
			project: 'Demo',
			url: '/convertigo/gw/ticket/',
			kind: 'frontbuilder.svelte.dev'
		});
		expect(flowBrowserPreview(browserEvent, 'Demo')).toEqual({
			projectName: 'Demo',
			url: '/convertigo/gw/ticket/',
			mode: 'development'
		});
		expect(flowBrowserPreview(browserEvent, 'Other')).toBeNull();
		expect(
			flowBrowserPreview(event('flow.browser.open', { project: 'Demo', url: '/built/' }), 'Demo')
		).toEqual({ projectName: 'Demo', url: '/built/', mode: 'production' });
	});

	it('reveals only explicit managed writes for the selected project', () => {
		const sourceEvent = event('flow.source.changed', {
			project: 'Demo',
			sourcePath: 'model/Demo/src/routes/+page.flow.svelte',
			reveal: true
		});
		expect(flowSourceReveal(sourceEvent, 'Demo')).toBe('model/Demo/src/routes/+page.flow.svelte');
		expect(flowSourceReveal(sourceEvent, 'Other')).toBe('');
		expect(
			flowSourceReveal(
				{ ...sourceEvent, payload: { ...sourceEvent.payload, reveal: false } },
				'Demo'
			)
		).toBe('');
	});
});
