/**
 * The Studio following an agent of the Assistant, live: its activity feed, the objects it touches lit in the tree and
 * the Flow as they appear, change or go, the views following it at a pace the user can read, and the pause when the
 * user works in the Studio, which offers to resume.
 */

import {
	classifyChange,
	createAgentFeed,
	createFollowPolicy,
	createTreeChangeLog
} from './agentActivity.js';
import { equivalentDboObjectIds, isDescendantObjectId } from './dnd';
import { createRevealFollower } from './studioReveal.js';

/** how long an object the agent touched stays lit */
const HIGHLIGHT_MS = 4_000;
/** how long an object the agent removed shows where it was */
const GHOST_MS = 2_600;
/** how far around a reveal the changes the tree sees go with it */
const CHANGE_WINDOW_MS = 8_000;

/** @typedef {import('./agentActivity.js').AgentChange} AgentChange */
/** @typedef {import('./agentActivity.js').AgentEntry} AgentEntry */
/** @typedef {import('./studioReveal.js').StudioRevealTarget} StudioRevealTarget */
/** @typedef {{ id: string, name: string, icon: string, serial: number }} AgentGhost */

export const agentFollow = $state({
	/** whether the Studio follows the agent, as its preference asks */
	enabled: false,
	/** whether following pauses, the views no longer going to what the agent does */
	paused: false,
	/** @type {'' | 'user' | 'manual'} the pause of the user's work in the Studio, or asked */
	pauseReason: /** @type {'' | 'user' | 'manual'} */ (''),
	/** whether the agent works: a turn the Assistant tells running, or a change lately */
	working: false,
	/** whether the Assistant tells a turn of its agent is running */
	running: false,
	/** when the turn running started, 0 for none told */
	runStart: 0,
	/** the actions of the agent since following paused */
	missed: 0,
	/** @type {AgentEntry[]} the activity feed, the last action first */
	entries: /** @type {AgentEntry[]} */ ([]),
	/** the entry the views show */
	currentKey: 0,
	/** @type {Record<string, { change: AgentChange, serial: number }>} the objects lit, by their ids */
	highlights: /** @type {Record<string, { change: AgentChange, serial: number }>} */ ({}),
	/** @type {Record<string, AgentGhost[]>} the objects removed, by the id of the branch they were in */
	ghosts: /** @type {Record<string, AgentGhost[]>} */ ({}),
	/** @type {Record<string, AgentChange>} the objects the agent touched since the feed was cleared */
	touched: /** @type {Record<string, AgentChange>} */ ({})
});

const log = createTreeChangeLog();
const feed = createAgentFeed();
const policy = createFollowPolicy();
let serial = 0;
/** @type {Map<string, ReturnType<typeof setTimeout>>} */
const highlightTimers = new Map();
/** @type {ReturnType<typeof setTimeout> | undefined} */
let workingTimer;
/** the events of the tree already lit */
const litEvents = new WeakSet();
/** @type {{ id: string, at: number, pulsed: boolean } | null} the last object the views went to */
let lastShown = null;

/**
 * @typedef {(parameters: Record<string, string>) => Promise<any>} TreeReader reads branches of the tree, as the
 *          studio.treeview.Get service: one by its id, or several by the JSON array of their ids
 */

/**
 * @type {{ apply: (target: StudioRevealTarget) => void | Promise<void>, follower: ReturnType<typeof createRevealFollower>, readTree?: TreeReader } | null}
 */
let connection = null;
/** the projects whose branches were read for the agent */
const readProjects = new Set();

/** @param {AgentEntry} entry */
function copyEntry(entry) {
	return { ...entry };
}

function syncEntries() {
	agentFollow.entries = feed.entries.map(copyEntry);
}

function syncPolicy() {
	agentFollow.paused = policy.paused;
	agentFollow.pauseReason = policy.reason;
	agentFollow.working = policy.working;
	if (!policy.paused) {
		agentFollow.missed = 0;
	}
}

function scheduleWorkingEnd() {
	clearTimeout(workingTimer);
	// a turn running ends as the Assistant tells it
	if (policy.idleAt === Infinity) return;
	workingTimer = setTimeout(
		() => {
			policy.expire();
			syncPolicy();
		},
		Math.max(0, policy.idleAt - Date.now()) + 50
	);
}

/**
 * The Assistant tells a turn of its agent starts or ends: the agent works from the start of the turn, before it
 * changes anything, until its end, rather than until a while after its last change.
 * @param {boolean} running
 */
export function agentRunSignal(running) {
	if (running && !policy.running) {
		agentFollow.runStart = Date.now();
	}
	policy.run(running);
	agentFollow.running = running;
	if (!running) {
		policy.expire();
	}
	scheduleWorkingEnd();
	syncPolicy();
}

/**
 * Lights an object the agent touched, in the tree and the Flow, for a moment.
 * @param {string} id
 * @param {AgentChange} change
 */
export function flashAgentObject(id, change) {
	if (!id) return;
	const flash = { change, serial: ++serial };
	for (const equivalent of equivalentDboObjectIds(id)) {
		agentFollow.highlights[equivalent] = flash;
		clearTimeout(highlightTimers.get(equivalent));
		highlightTimers.set(
			equivalent,
			setTimeout(() => {
				highlightTimers.delete(equivalent);
				if (agentFollow.highlights[equivalent]?.serial === flash.serial) {
					delete agentFollow.highlights[equivalent];
				}
			}, HIGHLIGHT_MS)
		);
	}
	if (change !== 'deleted' && change !== 'opened') {
		agentFollow.touched[id] = change === 'changed' ? (agentFollow.touched[id] ?? change) : change;
	}
}

/**
 * @param {AgentChange} change
 * @returns {AgentChange} the light of the object revealed: the parent of an object removed is changed
 */
function lightOf(change) {
	return change === 'deleted' ? 'changed' : change;
}

/**
 * Shows for a moment an object the agent removed, where it was.
 * @param {string} parentId the branch of the tree it was in
 * @param {{ id: string, name: string, icon?: string }} object
 */
function showGhost(parentId, object) {
	if (!parentId || !object.id) return;
	const ghost = { id: object.id, name: object.name, icon: object.icon ?? '', serial: ++serial };
	agentFollow.ghosts[parentId] = [
		...(agentFollow.ghosts[parentId] ?? []).filter((item) => item.id !== object.id),
		ghost
	];
	delete agentFollow.touched[object.id];
	setTimeout(() => {
		const ghosts = (agentFollow.ghosts[parentId] ?? []).filter(
			(item) => item.serial !== ghost.serial
		);
		if (ghosts.length) agentFollow.ghosts[parentId] = ghosts;
		else delete agentFollow.ghosts[parentId];
	}, GHOST_MS);
}

/**
 * Lights what the tree saw appear or vanish while the agent works, as the steps it adds beside the one it reveals.
 * @param {import('./agentActivity.js').TreeChangeEvent[]} events
 */
function lightTreeEvents(events) {
	if (!agentFollow.enabled || !policy.working) return;
	const time = Date.now();
	for (const event of events) {
		if (litEvents.has(event) || time - event.at > CHANGE_WINDOW_MS) continue;
		litEvents.add(event);
		if (event.type === 'appeared') {
			flashAgentObject(event.id, 'created');
		} else {
			showGhost(event.parentId, event);
		}
	}
}

/**
 * The tree read the children of a branch: what appeared or vanished tells the changes of the agent.
 * @param {string} parentId
 * @param {any} previous the children it had, not an array for a branch never read
 * @param {any[]} next
 */
export function agentTreeChildren(parentId, previous, next) {
	const changes = log.children(parentId, previous, next);
	if (!changes.length || !agentFollow.enabled) return;
	lightTreeEvents(changes);
	// the reveals of a moment ago, which the tree tells better now
	const time = Date.now();
	let updated = false;
	for (const entry of feed.entries) {
		if (time - entry.updatedAt > CHANGE_WINDOW_MS) break;
		const classification = classifyChange(entry.target, log.events, {
			at: entry.updatedAt,
			isNew: log.isNew
		});
		if (feed.update(entry.key, classification)) {
			updated = true;
			flashAgentObject(entry.id, lightOf(classification.change));
			if (classification.removed?.parentId) {
				showGhost(classification.removed.parentId, classification.removed);
			}
		}
	}
	if (updated) syncEntries();
}

/**
 * Follows the reveals of the agent: the activity feed and the lights at once, the views at a pace the user can read,
 * unless following pauses.
 *
 * @param {{ apply: (target: StudioRevealTarget) => void | Promise<void>, isBusy?: () => boolean,
 *  readTree?: TreeReader }} options apply shows an object in its views; readTree reads the branches of a project the
 *  agent works on, for the changes of the branches the tree never read
 */
export function connectAgentFollow({ apply, isBusy, readTree }) {
	const follower = createRevealFollower({
		apply: async (target) => {
			if (policy.paused || !agentFollow.enabled) return;
			await show(target);
		},
		isBusy
	});
	connection = { apply, follower, readTree };

	/**
	 * The user works in the Studio: following waits, and pauses while the agent works.
	 * @param {Event} event
	 */
	const onActivity = (event) => {
		const element = /** @type {any} */ (event.target);
		const closest = (/** @type {string} */ selector) =>
			typeof element?.closest === 'function' && Boolean(element.closest(selector));
		if (closest('[data-agent-follow]')) return;
		follower.activity();
		// a dialog already holds what the agent reveals, until it closes
		if (closest('.studio-dialog, [role="dialog"]')) return;
		if (event.type === 'pointerdown') {
			userActed();
		} else if (event.type === 'keydown') {
			const key = /** @type {KeyboardEvent} */ (event).key;
			// a modifier alone, or Escape closing a menu or the feed, is no work in the Studio
			if (!['Shift', 'Control', 'Alt', 'Meta', 'CapsLock', 'Escape'].includes(key)) userActed();
		}
	};
	const detachFrames = watchFrames(onActivity);

	return {
		/** @param {StudioRevealTarget | null} target an object the agent revealed */
		receive(target) {
			if (target && agentFollow.enabled) receive(target);
		},
		/** the user works in the Studio: following waits, and pauses while the agent works */
		activity: onActivity,
		/**
		 * Reads the project the agent is about to work on, as the selected one, the Assistant's context: what it creates
		 * first is told then too.
		 * @param {string} projectName
		 */
		prepare(projectName) {
			if (projectName && agentFollow.enabled) void readProject(projectName);
		},
		/** @param {boolean} enabled whether the Studio follows the agent, as its preference asks */
		setEnabled(enabled) {
			agentFollow.enabled = enabled;
			if (!enabled) follower.clear();
		},
		dispose() {
			detachFrames();
			follower.dispose();
			if (connection?.follower === follower) connection = null;
		}
	};
}

/** the events of the user working in a frame of the Studio */
const FRAME_ACTIVITY_EVENTS = ['pointerdown', 'keydown', 'wheel'];

/**
 * Listens to the user working in the frames of the Studio, as the preview of an application, whose events do not reach
 * the Studio: each document they load, of the same origin; not the Assistant, where the user talks to the agent.
 * @param {(event: Event) => void} onActivity
 * @returns {() => void} stops listening
 */
function watchFrames(onActivity) {
	if (typeof document?.addEventListener !== 'function') return () => {};
	/** @type {Set<Window>} */
	const windows = new Set();
	/** @param {Element | null} frame */
	const attach = (frame) => {
		if (frame?.tagName !== 'IFRAME' || frame.classList.contains('studio-assistant')) return;
		try {
			const win = /** @type {HTMLIFrameElement} */ (frame).contentWindow;
			const doc = win?.document;
			// a document listened to keeps its listeners; a new one, as a reload, needs them
			if (!win || !doc || /** @type {any} */ (doc).__agentFollow) return;
			/** @type {any} */ (doc).__agentFollow = true;
			for (const type of FRAME_ACTIVITY_EVENTS) {
				win.addEventListener(type, onActivity, { capture: true, passive: true });
			}
			windows.add(win);
		} catch {
			// a frame of another origin
		}
	};
	/** @param {Event} event */
	const onLoad = (event) => attach(/** @type {Element} */ (event.target));
	document.addEventListener('load', onLoad, true);
	document.querySelectorAll('iframe').forEach(attach);
	return () => {
		document.removeEventListener('load', onLoad, true);
		for (const win of windows) {
			for (const type of FRAME_ACTIVITY_EVENTS) {
				try {
					win.removeEventListener(type, onActivity, { capture: true });
				} catch {
					// a frame gone
				}
			}
		}
		windows.clear();
	};
}

/**
 * Reads the branches of a project the agent works on, the first time, a few levels down: what appears in them later,
 * as the tree opens them, tells the objects the agent creates.
 * @param {string} projectName
 */
async function readProject(projectName) {
	const readTree = connection?.readTree;
	if (!readTree || readProjects.has(projectName)) return;
	readProjects.add(projectName);
	try {
		/** @type {string[]} */
		let ids = [projectName];
		for (let level = 0; level < 3 && ids.length; level++) {
			const read =
				ids.length === 1
					? { [ids[0]]: (await readTree({ id: ids[0] }))?.children }
					: await readTree({ ids: JSON.stringify(ids) });
			/** @type {string[]} */
			const next = [];
			/** @param {any[]} children */
			const unread = (children) => {
				for (const child of children) {
					if (child?.children === true && child.id) next.push(String(child.id));
					else if (Array.isArray(child?.children)) unread(child.children);
				}
			};
			for (const id of ids) {
				const children = read?.[id];
				if (!Array.isArray(children)) continue;
				agentTreeChildren(id, undefined, children);
				unread(children);
			}
			ids = next.slice(0, 300);
		}
	} catch (error) {
		readProjects.delete(projectName);
		console.debug('Unable to read the project the agent works on', error);
	}
}

/** @param {StudioRevealTarget} target */
function receive(target) {
	void readProject(target.projectName);
	const wasWorking = policy.working;
	policy.agent();
	scheduleWorkingEnd();
	const classification = classifyChange(target, log.events, { at: Date.now(), isNew: log.isNew });
	const entry = feed.add(target, classification);
	syncEntries();
	syncPolicy();
	if (!wasWorking) {
		// the first change of a request of the agent: what the tree saw it do before tells too
		lightTreeEvents(log.events);
	}
	flashAgentObject(target.id, lightOf(entry.change));
	for (const id of target.touched ?? []) {
		flashAgentObject(id, 'changed');
	}
	if (classification.removed?.parentId) {
		showGhost(classification.removed.parentId, classification.removed);
	}
	// the application the agent opens again, as it checks its build, while the views show what it changes in it
	const opened =
		entry.change === 'opened' &&
		lastShown !== null &&
		isDescendantObjectId(lastShown.id, target.id);
	if (policy.paused) {
		if (entry.change !== 'opened') agentFollow.missed += 1;
	} else if (!opened) {
		connection?.follower.push(target);
	}
}

/** @param {StudioRevealTarget} target */
async function show(target) {
	const entry = feed.entries.find((item) => item.target === target) ?? feed.entries[0];
	agentFollow.currentKey = entry?.key ?? 0;
	lastShown = { id: target.id, at: Date.now(), pulsed: false };
	await connection?.apply(target);
}

/** The user works in the Studio: following pauses while the agent works. */
function userActed() {
	if (agentFollow.enabled && policy.userActed()) {
		connection?.follower.clear();
		syncPolicy();
	}
}

/** Following pauses until the user resumes it. */
export function pauseFollowing() {
	policy.pause('manual');
	connection?.follower.clear();
	syncPolicy();
}

/** Following resumes, showing the last thing the agent did. */
export function resumeFollowing() {
	policy.resume();
	syncPolicy();
	const last = feed.entries[0];
	if (last && connection) {
		connection.follower.clear();
		void show(last.target);
	}
}

/**
 * Shows the object of an action of the feed; following pauses while the agent works, to leave it shown.
 * @param {AgentEntry} entry
 */
export function showAgentEntry(entry) {
	if (policy.working && !policy.paused) {
		policy.pause('user');
		syncPolicy();
	}
	connection?.follower.clear();
	flashAgentObject(entry.id, lightOf(entry.change));
	void show(entry.target);
}

/** Forgets the activity of the agent: its feed and the marks of the objects it touched. */
export function clearAgentActivity() {
	feed.clear();
	agentFollow.entries = [];
	agentFollow.currentKey = 0;
	agentFollow.missed = 0;
	agentFollow.touched = {};
}

/**
 * @param {string} id the id of a row of the tree or of a block of the Flow
 * @returns {{ change: AgentChange, serial: number } | undefined} its light, while the agent touches it
 */
export function agentHighlightOf(id) {
	return id ? agentFollow.highlights[id] : undefined;
}

/** @type {AgentGhost[]} */
const NO_GHOSTS = [];

/**
 * @param {string} id the id of a branch of the tree
 * @returns {AgentGhost[]} the objects the agent has just removed from it
 */
export function agentGhostsOf(id) {
	return (id && agentFollow.ghosts[id]) || NO_GHOSTS;
}

/**
 * @param {string} id the id of a row of the tree
 * @param {boolean} expanded whether its branch shows
 * @returns {'' | 'self' | 'inside'} whether the agent touched the object, or one inside a closed branch
 */
export function agentMarkOf(id, expanded) {
	if (!id || !agentFollow.enabled) return '';
	const touched = agentFollow.touched;
	if (equivalentDboObjectIds(id).some((equivalent) => touched[equivalent])) return 'self';
	if (expanded) return '';
	return Object.keys(touched).some((touchedId) => isDescendantObjectId(touchedId, id))
		? 'inside'
		: '';
}

/**
 * The preview showing a component the agent has just revealed, its highlight pulses, once.
 * @param {string} id the component the preview highlights
 * @returns {boolean} whether to pulse
 */
export function takeAgentPulse(id) {
	if (!lastShown || lastShown.pulsed || Date.now() - lastShown.at > 15_000) return false;
	if (!equivalentDboObjectIds(id).includes(lastShown.id)) return false;
	lastShown.pulsed = true;
	return true;
}

/** the colors of the lights of the changes, readable on a light or a dark theme */
const LIGHT_COLORS = /** @type {Record<AgentChange, [string, string]>} */ ({
	created: ['rgba(34, 197, 94, 0.30)', 'rgb(34, 197, 94)'],
	changed: ['rgba(59, 130, 246, 0.26)', 'rgb(59, 130, 246)'],
	renamed: ['rgba(139, 92, 246, 0.30)', 'rgb(139, 92, 246)'],
	moved: ['rgba(6, 182, 212, 0.28)', 'rgb(6, 182, 212)'],
	deleted: ['rgba(239, 68, 68, 0.28)', 'rgb(239, 68, 68)'],
	opened: ['rgba(168, 145, 255, 0.24)', 'rgb(168, 145, 255)']
});

/**
 * Plays the light of a change of the agent on a row of the tree or a block of the Flow: an object created slides in,
 * a change glows, then fades.
 * @param {Element | undefined | null} element
 * @param {AgentChange} change
 * @param {'row' | 'block'} shape
 */
export function playAgentLight(element, change, shape) {
	if (!element || typeof element.animate !== 'function') return;
	const [soft, solid] = LIGHT_COLORS[change] ?? LIGHT_COLORS.changed;
	const still =
		typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
	const created = change === 'created' && !still;
	/** @type {Keyframe[]} */
	const keyframes =
		shape === 'row'
			? [
					{
						backgroundColor: soft,
						boxShadow: `inset 3px 0 0 ${solid}`,
						...(created ? { transform: 'translateX(-0.5rem)', opacity: 0.2 } : {})
					},
					{
						offset: 0.18,
						backgroundColor: soft,
						boxShadow: `inset 3px 0 0 ${solid}`,
						...(created ? { transform: 'none', opacity: 1 } : {})
					},
					{ offset: 0.55, backgroundColor: soft, boxShadow: `inset 3px 0 0 ${solid}` },
					{ backgroundColor: 'transparent', boxShadow: 'inset 3px 0 0 transparent' }
				]
			: [
					{
						boxShadow: `0 0 0 0 ${solid}`,
						...(created ? { transform: 'scale(0.82)', opacity: 0 } : {})
					},
					{
						offset: 0.2,
						boxShadow: `0 0 0 5px ${soft}`,
						...(created ? { transform: 'scale(1.04)', opacity: 1 } : {})
					},
					{
						offset: 0.32,
						boxShadow: `0 0 0 5px ${soft}`,
						...(created ? { transform: 'none' } : {})
					},
					{ offset: 0.6, boxShadow: `0 0 0 3px ${soft}` },
					{ boxShadow: '0 0 0 0 transparent' }
				];
	element.animate(keyframes, { duration: still ? 1_200 : 2_600, easing: 'ease-out' });
}
