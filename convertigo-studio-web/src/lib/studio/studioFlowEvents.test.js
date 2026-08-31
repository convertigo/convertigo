import { describe, expect, it } from 'vitest';
import { flowBrowserPreview, flowSourceReveal } from './studioFlowEvents';

/** @param {string} topic @param {Record<string, any>} payload */
function event(topic, payload) {
	return { id: 'event', topic, timestamp: 1, instance: 'test', payload };
}

describe('Studio Flow events', () => {
	it('carries the explicit project of a development viewer', () => {
		const browserEvent = event('flow.browser.open', {
			project: 'Demo',
			url: '/convertigo/gw/ticket/',
			kind: 'frontbuilder.svelte.dev'
		});
		expect(flowBrowserPreview(browserEvent)).toEqual({
			projectName: 'Demo',
			url: '/convertigo/gw/ticket/',
			mode: 'development'
		});
		expect(
			flowBrowserPreview(event('flow.browser.open', { project: 'Demo', url: '/built/' }))
		).toEqual({ projectName: 'Demo', url: '/built/', mode: 'production' });
	});

	it('carries the explicit project and source of managed writes', () => {
		const sourceEvent = event('flow.source.changed', {
			project: 'Demo',
			sourcePath: 'model/Demo/src/routes/+page.flow.svelte',
			reveal: true
		});
		expect(flowSourceReveal(sourceEvent)).toEqual({
			projectName: 'Demo',
			sourcePath: 'model/Demo/src/routes/+page.flow.svelte'
		});
		expect(
			flowSourceReveal({ ...sourceEvent, payload: { ...sourceEvent.payload, reveal: false } })
		).toBeNull();
	});
});
