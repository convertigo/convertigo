import { describe, expect, it } from 'vitest';
import { createStudioMutationEventTracker } from './studioMutationEvents';

describe('Studio local mutation events', () => {
	it('consumes only the matching delayed Flow source event', () => {
		const tracker = createStudioMutationEventTracker();
		tracker.remember({
			selectionSourcePath: '/workspace/projects/Project/model/src/routes/+page.flow.svelte',
			projectedSourcePath: '/workspace/projects/Project/model/src/routes/+page.flow.svelte'
		});

		expect(
			tracker.consume({
				topic: 'projects.changed',
				payload: {
					scope: 'flow',
					reason: '/workspace/projects/Project/model/src/routes/other/+page.flow.svelte'
				}
			})
		).toBe(false);
		expect(
			tracker.consume({
				topic: 'projects.changed',
				payload: {
					scope: 'flow',
					reason: '/workspace/projects/Project/model/src/routes/+page.flow.svelte'
				}
			})
		).toBe(true);
		expect(
			tracker.consume({
				topic: 'projects.changed',
				payload: {
					scope: 'flow',
					reason: '/workspace/projects/Project/model/src/routes/+page.flow.svelte'
				}
			})
		).toBe(false);
	});

	it('does not hide project exports or expired source events', () => {
		let time = 100;
		const tracker = createStudioMutationEventTracker({ now: () => time, ttlMs: 50 });
		tracker.remember({ selectionSourcePath: '/workspace/project/+page.flow.svelte' });
		expect(
			tracker.consume({
				topic: 'projects.changed',
				payload: { scope: 'project', reason: 'project.exported' }
			})
		).toBe(false);
		time = 151;
		expect(
			tracker.consume({
				topic: 'projects.changed',
				payload: { scope: 'flow', reason: '/workspace/project/+page.flow.svelte' }
			})
		).toBe(false);
	});

	it('tracks repeated local mutations of the same source independently', () => {
		const tracker = createStudioMutationEventTracker();
		const mutation = { selectionSourcePath: '/workspace/project/+page.flow.svelte' };
		const event = {
			topic: 'projects.changed',
			payload: { scope: 'flow', reason: '/workspace/project/+page.flow.svelte' }
		};
		tracker.remember(mutation);
		tracker.remember(mutation);
		expect(tracker.consume(event)).toBe(true);
		expect(tracker.consume(event)).toBe(true);
		expect(tracker.consume(event)).toBe(false);
	});
});
