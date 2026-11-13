import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
	createRevealFollower,
	followsAgent,
	isEditableElement,
	revealsFlow,
	revealsFrontend,
	studioRevealTarget
} from './studioReveal';

/** @param {string} topic @param {Record<string, any>} payload */
function event(topic, payload) {
	return { id: 'event', topic, timestamp: 1, instance: 'test', payload };
}

/** @param {string} id @param {string} [kind] */
function target(id, kind = 'sequence.child') {
	return {
		projectName: 'Shop',
		id,
		kind,
		sequenceId: kind.startsWith('sequence') ? 'Shop.sq:Checkout' : '',
		pageId: '',
		applicationId: ''
	};
}

describe('Studio reveal events', () => {
	it('carries the change the MCP tells, which an older MCP does not', () => {
		const reveal = studioRevealTarget(
			event('studio.reveal', {
				project: 'Shop',
				id: 'Shop.sq:Checkout',
				kind: 'sequence',
				sequence: 'Shop.sq:Checkout',
				change: 'renamed',
				previous: 'Shop.Payment',
				tool: 'databaseobject-rename',
				summary: 'Renamed Payment to Checkout',
				touched: ['Shop.sq:Checkout.st:copy', 'Other.sq:x', '']
			})
		);
		expect(reveal).toMatchObject({
			change: 'renamed',
			previous: 'Shop.Payment',
			tool: 'databaseobject-rename',
			summary: 'Renamed Payment to Checkout',
			touched: ['Shop.sq:Checkout.st:copy']
		});
		const older = studioRevealTarget(
			event('studio.reveal', { project: 'Shop', id: 'Shop.sq:Checkout', kind: 'sequence' })
		);
		expect(older && Object.keys(older)).toEqual([
			'projectName',
			'id',
			'kind',
			'sequenceId',
			'pageId',
			'applicationId'
		]);
	});

	it('carries the object, its kind and what holds it', () => {
		const reveal = studioRevealTarget(
			event('studio.reveal', {
				project: 'Shop',
				id: 'Shop.sq:Checkout.st:copy',
				kind: 'sequence.child',
				sequence: 'Shop.sq:Checkout',
				page: '',
				application: ''
			})
		);
		expect(reveal).toEqual({
			projectName: 'Shop',
			id: 'Shop.sq:Checkout.st:copy',
			kind: 'sequence.child',
			sequenceId: 'Shop.sq:Checkout',
			pageId: '',
			applicationId: ''
		});
		expect(revealsFlow(/** @type {any} */ (reveal))).toBe(true);
		expect(revealsFrontend(/** @type {any} */ (reveal))).toBe(false);

		const page = studioRevealTarget(
			event('studio.reveal', {
				project: 'Shop',
				id: 'Shop.mb:MobileApplication.ap:Application.pg:Cart',
				kind: 'ngx.page',
				page: 'Shop.mb:MobileApplication.ap:Application.pg:Cart'
			})
		);
		expect(page?.kind).toBe('ngx.page');
		expect(revealsFrontend(/** @type {any} */ (page))).toBe(true);
		expect(revealsFlow(/** @type {any} */ (page))).toBe(false);
	});

	it('ignores other topics and objects outside their project', () => {
		expect(studioRevealTarget(event('projects.changed', { project: 'Shop', id: 'Shop' }))).toBe(
			null
		);
		expect(studioRevealTarget(event('studio.reveal', { project: 'Shop' }))).toBe(null);
		expect(
			studioRevealTarget(event('studio.reveal', { project: 'Shop', id: 'Other.sq:Checkout' }))
		).toBe(null);
		expect(studioRevealTarget(event('studio.reveal', { project: 'Shop', id: 'Shop' }))?.kind).toBe(
			'object'
		);
		expect(studioRevealTarget(null)).toBe(null);
	});

	it('follows the agent in the Vibe profile, or as the preference tells', () => {
		expect(followsAgent(undefined, 'vibe')).toBe(true);
		expect(followsAgent('vibe', 'backend')).toBe(false);
		expect(followsAgent('always', 'backend')).toBe(true);
		expect(followsAgent('never', 'vibe')).toBe(false);
	});

	it('tells the fields the user types in', () => {
		/** @param {Record<string, any>} element */
		const element = (element) => ({ nodeType: 1, isContentEditable: false, ...element });
		expect(isEditableElement(/** @type {any} */ (element({ tagName: 'TEXTAREA' })))).toBe(true);
		expect(
			isEditableElement(/** @type {any} */ (element({ tagName: 'INPUT', type: 'text' })))
		).toBe(true);
		expect(
			isEditableElement(/** @type {any} */ (element({ tagName: 'INPUT', type: 'checkbox' })))
		).toBe(false);
		expect(
			isEditableElement(/** @type {any} */ (element({ tagName: 'DIV', isContentEditable: true })))
		).toBe(true);
		// the chat of the Assistant is in its own frame
		expect(isEditableElement(/** @type {any} */ (element({ tagName: 'IFRAME' })))).toBe(false);
		expect(isEditableElement(null)).toBe(false);
	});
});

describe('Studio reveal follower', () => {
	beforeEach(() => {
		vi.useFakeTimers();
		vi.setSystemTime(0);
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it('shows only the last reveal of a burst, at most one per interval', async () => {
		const applied = /** @type {string[]} */ ([]);
		const follower = createRevealFollower({
			apply: (reveal) => void applied.push(reveal.id),
			settleDelay: 300,
			interval: 1500
		});
		for (let index = 0; index < 20; index++) {
			follower.push(target(`Shop.sq:Checkout.st:step${index}`));
			await vi.advanceTimersByTimeAsync(10);
		}
		// the first reveal waits for its burst to settle, then the last one shows
		await vi.advanceTimersByTimeAsync(300);
		expect(applied).toEqual(['Shop.sq:Checkout.st:step19']);

		follower.push(target('Shop.sq:Checkout.st:next'));
		follower.push(target('Shop.sq:Checkout.st:last'));
		await vi.advanceTimersByTimeAsync(1000);
		expect(applied).toHaveLength(1);
		await vi.advanceTimersByTimeAsync(600);
		expect(applied).toEqual(['Shop.sq:Checkout.st:step19', 'Shop.sq:Checkout.st:last']);
		await vi.advanceTimersByTimeAsync(5000);
		expect(applied).toHaveLength(2);
	});

	it('waits for the user to stop working in the Studio', async () => {
		const applied = /** @type {string[]} */ ([]);
		let busy = false;
		const follower = createRevealFollower({
			apply: (reveal) => void applied.push(reveal.id),
			isBusy: () => busy,
			settleDelay: 300,
			idleDelay: 4000
		});
		follower.activity();
		follower.push(target('Shop.sq:Checkout'));
		await vi.advanceTimersByTimeAsync(2000);
		follower.activity();
		await vi.advanceTimersByTimeAsync(3900);
		expect(applied).toEqual([]);
		// a field still typed in keeps the reveal waiting
		busy = true;
		await vi.advanceTimersByTimeAsync(3000);
		expect(applied).toEqual([]);
		busy = false;
		await vi.advanceTimersByTimeAsync(500);
		expect(applied).toEqual(['Shop.sq:Checkout']);
	});

	it('forgets the reveal waiting once the Studio stops following', async () => {
		const apply = vi.fn();
		const follower = createRevealFollower({ apply });
		follower.push(target('Shop.sq:Checkout'));
		follower.clear();
		await vi.advanceTimersByTimeAsync(5000);
		expect(apply).not.toHaveBeenCalled();
		follower.push(target('Shop.sq:Checkout'));
		follower.dispose();
		await vi.advanceTimersByTimeAsync(5000);
		expect(apply).not.toHaveBeenCalled();
	});

	it('keeps following after a reveal that fails', async () => {
		const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
		const applied = /** @type {string[]} */ ([]);
		const follower = createRevealFollower({
			apply: (reveal) => {
				if (reveal.id.endsWith('broken')) throw new Error('gone');
				applied.push(reveal.id);
			}
		});
		follower.push(target('Shop.sq:Checkout.st:broken'));
		await vi.advanceTimersByTimeAsync(300);
		follower.push(target('Shop.sq:Checkout.st:copy'));
		await vi.advanceTimersByTimeAsync(1500);
		expect(applied).toEqual(['Shop.sq:Checkout.st:copy']);
		expect(warn).toHaveBeenCalled();
		warn.mockRestore();
	});
});
