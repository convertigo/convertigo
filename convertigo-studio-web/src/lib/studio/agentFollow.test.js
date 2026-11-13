import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
	agentFollow,
	agentGhostsOf,
	agentHighlightOf,
	agentMarkOf,
	agentRunSignal,
	agentTreeChildren,
	clearAgentActivity,
	connectAgentFollow,
	pauseFollowing,
	resumeFollowing,
	showAgentEntry,
	takeAgentPulse
} from './agentFollow.svelte.js';

/** @param {string} id @param {string} [kind] @param {Record<string, any>} [more] */
function target(id, kind = 'sequence.child', more = {}) {
	return {
		projectName: 'Shop',
		id,
		kind,
		sequenceId: kind.startsWith('sequence') ? 'Shop.sq:Checkout' : '',
		pageId: '',
		applicationId: '',
		...more
	};
}

/** @param {string} id */
function node(id) {
	const name = id.slice(id.lastIndexOf(':') + 1);
	return { id, name, label: name, icon: `icon/${name}` };
}

/** @param {string} type */
function userEvent(type, key = '') {
	return /** @type {Event} */ (/** @type {unknown} */ ({ type, key, target: null }));
}

describe('the Studio following the agent', () => {
	/** @type {ReturnType<typeof connectAgentFollow>} */
	let follow;
	/** @type {string[]} */
	let shown;

	/** the time of each test after the one before, as the Studio remembers when the last turn of the agent ended */
	let clockStart = 100_000;

	beforeEach(() => {
		clockStart += 10_000_000;
		vi.useFakeTimers({ now: clockStart });
		vi.stubGlobal('window', new EventTarget());
		vi.stubGlobal('document', { activeElement: null });
		shown = [];
		follow = connectAgentFollow({
			apply: (item) => {
				shown.push(item.id);
			}
		});
		follow.setEnabled(true);
		clearAgentActivity();
		resumeFollowing();
	});

	afterEach(async () => {
		agentRunSignal(false);
		follow.dispose();
		// the agent stops working: the next test starts idle
		await vi.advanceTimersByTimeAsync(200_000);
		vi.unstubAllGlobals();
		vi.useRealTimers();
	});

	it('lists the action, lights its object and shows it at a pace', async () => {
		follow.receive(target('Shop.sq:Checkout.st:copy'));
		expect(agentFollow.entries.map((entry) => entry.name)).toEqual(['copy']);
		expect(agentFollow.working).toBe(true);
		expect(agentHighlightOf('Shop.sq:Checkout.st:copy')?.change).toBe('changed');
		expect(agentMarkOf('Shop.sq:Checkout.st:copy', false)).toBe('self');
		expect(agentMarkOf('Shop.sq:Checkout', false)).toBe('inside');
		expect(agentMarkOf('Shop.sq:Checkout', true)).toBe('');
		expect(shown).toEqual([]);
		await vi.advanceTimersByTimeAsync(400);
		expect(shown).toEqual(['Shop.sq:Checkout.st:copy']);
		expect(agentFollow.currentKey).toBe(agentFollow.entries[0].key);
		await vi.advanceTimersByTimeAsync(5_000);
		expect(agentHighlightOf('Shop.sq:Checkout.st:copy')).toBeUndefined();
		expect(takeAgentPulse('Shop.sq:Checkout.st:copy')).toBe(true);
		expect(takeAgentPulse('Shop.sq:Checkout.st:copy')).toBe(false);
	});

	it('pauses when the user works while the agent works, and offers to resume', async () => {
		follow.receive(target('Shop.sq:Checkout.st:a'));
		await vi.advanceTimersByTimeAsync(400);
		follow.activity(userEvent('keydown', 'Shift'));
		follow.activity(userEvent('keydown', 'Escape'));
		expect(agentFollow.paused).toBe(false);
		// a dialog already holds the reveals
		follow.activity(
			/** @type {Event} */ (
				/** @type {unknown} */ ({
					type: 'pointerdown',
					target: { closest: (/** @type {string} */ selector) => selector.includes('dialog') }
				})
			)
		);
		expect(agentFollow.paused).toBe(false);
		follow.activity(userEvent('pointerdown'));
		expect(agentFollow).toMatchObject({ paused: true, pauseReason: 'user' });
		follow.receive(target('Shop.sq:Checkout.st:b'));
		follow.receive(target('Shop.sq:Checkout.st:c'));
		await vi.advanceTimersByTimeAsync(10_000);
		// the feed and the lights go on, the views stay
		expect(agentFollow.missed).toBe(2);
		expect(agentFollow.entries).toHaveLength(3);
		expect(shown).toEqual(['Shop.sq:Checkout.st:a']);
		resumeFollowing();
		expect(agentFollow).toMatchObject({ paused: false, missed: 0 });
		expect(shown.at(-1)).toBe('Shop.sq:Checkout.st:c');
	});

	it('resumes on its own once the agent stopped, not after a pause asked', async () => {
		follow.receive(target('Shop.sq:Checkout.st:a'));
		follow.activity(userEvent('pointerdown'));
		expect(agentFollow.paused).toBe(true);
		// the agent thinks a while between two changes: following stays paused
		await vi.advanceTimersByTimeAsync(90_000);
		expect(agentFollow).toMatchObject({ paused: true, working: true });
		await vi.advanceTimersByTimeAsync(91_000);
		expect(agentFollow).toMatchObject({ paused: false, working: false });

		pauseFollowing();
		await vi.advanceTimersByTimeAsync(200_000);
		expect(agentFollow).toMatchObject({ paused: true, pauseReason: 'manual' });
		follow.receive(target('Shop.sq:Checkout.st:b'));
		await vi.advanceTimersByTimeAsync(5_000);
		expect(shown).toEqual([]);
		resumeFollowing();
		expect(shown).toEqual(['Shop.sq:Checkout.st:b']);
	});

	it('tells a creation the tree sees after the reveal, and a removal with its ghost', async () => {
		const sequence = 'Shop.sq:Checkout';
		agentTreeChildren(sequence, undefined, [node(`${sequence}.st:a`)]);
		// a step the sequence read before did not have
		follow.receive(target(`${sequence}.st:b`));
		expect(agentFollow.entries[0].change).toBe('created');
		agentTreeChildren(
			sequence,
			[node(`${sequence}.st:a`)],
			[node(`${sequence}.st:a`), node(`${sequence}.st:b`)]
		);
		expect(agentFollow.entries[0].change).toBe('created');
		expect(agentHighlightOf(`${sequence}.st:b`)?.change).toBe('created');

		// the agent removes a step, then reveals its sequence: the step shows a moment where it was
		const pay = 'Shop.sq:Pay';
		agentTreeChildren(pay, undefined, [node(`${pay}.st:old`), node(`${pay}.st:kept`)]);
		agentTreeChildren(
			pay,
			[node(`${pay}.st:old`), node(`${pay}.st:kept`)],
			[node(`${pay}.st:kept`)]
		);
		expect(agentGhostsOf(pay).map((ghost) => ghost.name)).toEqual(['old']);
		follow.receive(target(pay, 'sequence'));
		expect(agentFollow.entries[0]).toMatchObject({ change: 'deleted', removedName: 'old' });
		await vi.advanceTimersByTimeAsync(3_000);
		expect(agentGhostsOf(pay)).toEqual([]);
	});

	it('shows an entry clicked and stays on it while the agent works', async () => {
		follow.receive(target('Shop.sq:Checkout.st:a'));
		follow.receive(target('Shop.sq:Pay', 'sequence'));
		const first = agentFollow.entries.at(-1);
		showAgentEntry(/** @type {any} */ (first));
		expect(shown).toEqual(['Shop.sq:Checkout.st:a']);
		expect(agentFollow.paused).toBe(true);
		await vi.advanceTimersByTimeAsync(5_000);
		expect(shown).toEqual(['Shop.sq:Checkout.st:a']);
	});

	it('reads the project the agent starts on, to tell what it creates in branches the tree never read', async () => {
		follow.dispose();
		/** @type {Record<string, string>[]} */
		const reads = [];
		follow = connectAgentFollow({
			apply: (item) => {
				shown.push(item.id);
			},
			readTree: async (parameters) => {
				reads.push(parameters);
				if (parameters.id === 'Crm') {
					return {
						children: [
							{ id: 'Crm:cn', name: 'Connectors', children: [{ id: 'Crm.cn:db', children: true }] }
						]
					};
				}
				return { children: [node('Crm.cn:db.tr:a')] };
			}
		});
		follow.setEnabled(true);
		// the project selected, before the agent works on it
		follow.prepare('Crm');
		await vi.advanceTimersByTimeAsync(10);
		expect(reads).toEqual([{ id: 'Crm' }, { id: 'Crm.cn:db' }]);
		follow.receive({ ...target('Crm.cn:db.tr:a', 'transaction'), projectName: 'Crm' });
		await vi.advanceTimersByTimeAsync(10);
		expect(reads).toHaveLength(2);
		// the agent creates a transaction, which the tree shows as it opens the connector first
		follow.receive({ ...target('Crm.cn:db.tr:b', 'transaction'), projectName: 'Crm' });
		agentTreeChildren('Crm.cn:db', true, [node('Crm.cn:db.tr:a'), node('Crm.cn:db.tr:b')]);
		expect(agentFollow.entries[0]).toMatchObject({ name: 'b', change: 'created' });
		follow.receive({ ...target('Crm.cn:db.tr:c', 'transaction'), projectName: 'Crm' });
		expect(reads).toHaveLength(2);
	});

	it('pauses when the user clicks in the preview of the application, not in the Assistant', async () => {
		follow.dispose();
		/** @param {string} className */
		const frame = (className) => {
			const win = Object.assign(new EventTarget(), { document: {} });
			return {
				tagName: 'IFRAME',
				classList: { contains: (name) => name === className },
				contentWindow: win
			};
		};
		const preview = frame('studio-preview__frame');
		const assistant = frame('studio-assistant');
		vi.stubGlobal('document', {
			activeElement: null,
			addEventListener: () => {},
			removeEventListener: () => {},
			querySelectorAll: () => [preview, assistant]
		});
		follow = connectAgentFollow({ apply: (item) => void shown.push(item.id) });
		follow.setEnabled(true);
		follow.receive(target('Shop.sq:Checkout.st:a'));
		assistant.contentWindow.dispatchEvent(new Event('pointerdown'));
		expect(agentFollow.paused).toBe(false);
		preview.contentWindow.dispatchEvent(new Event('pointerdown'));
		expect(agentFollow).toMatchObject({ paused: true, pauseReason: 'user' });
		follow.dispose();
		resumeFollowing();
		follow.receive(target('Shop.sq:Checkout.st:b'));
		preview.contentWindow.dispatchEvent(new Event('pointerdown'));
		expect(agentFollow.paused).toBe(false);
	});

	it('stays on what the agent changes in an application it opens again', async () => {
		const application = 'Shop.Application.NgxApp';
		follow.receive(target(application, 'ngx.application'));
		await vi.advanceTimersByTimeAsync(400);
		follow.receive(target(`${application}.pg:Cart`, 'ngx.page'));
		await vi.advanceTimersByTimeAsync(2_000);
		follow.receive(target(application, 'ngx.application'));
		await vi.advanceTimersByTimeAsync(5_000);
		expect(shown).toEqual([application, `${application}.pg:Cart`]);
		// the feed keeps the change first, the application opened twice below it
		expect(
			agentFollow.entries.map((entry) => `${entry.change} ${entry.name} ${entry.count}`)
		).toEqual(['changed Cart 1', 'opened NgxApp 2']);
	});

	it('shows the agent working from the start of a turn the Assistant tells, and pauses until its end', async () => {
		agentRunSignal(true);
		expect(agentFollow).toMatchObject({ running: true, working: true });
		expect(agentFollow.runStart).toBeGreaterThan(0);
		// the user works while the agent reads the project, before it changes anything
		follow.activity(userEvent('pointerdown'));
		expect(agentFollow).toMatchObject({ paused: true, pauseReason: 'user' });
		follow.receive(target('Shop.sq:Checkout.st:a'));
		// a long turn: following stays paused past the estimate of an older Assistant
		await vi.advanceTimersByTimeAsync(300_000);
		expect(agentFollow).toMatchObject({ paused: true, working: true, missed: 1 });
		agentRunSignal(false);
		expect(agentFollow).toMatchObject({ running: false, working: false, paused: false });
		expect(shown).toEqual([]);
	});

	it('follows nothing when the preference does not ask', async () => {
		follow.setEnabled(false);
		follow.receive(target('Shop.sq:Checkout.st:a'));
		await vi.advanceTimersByTimeAsync(5_000);
		expect(agentFollow.entries).toEqual([]);
		expect(shown).toEqual([]);
	});
});
