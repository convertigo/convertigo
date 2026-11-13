/**
 * The objects an agent of the Assistant creates or changes, which the engine reveals (the studio.reveal Admin
 * events) and the web Studio follows, as the reveal mode of the Assistant shows them in the Project Explorer of the
 * Eclipse Studio: the object selected in the tree, the Flow of its sequence, the preview of its NGX page.
 */

export const STUDIO_REVEAL_TOPIC = 'studio.reveal';

/**
 * @typedef {Object} StudioRevealTarget
 * @property {string} projectName
 * @property {string} id the object, by the id of the tree (its full QName)
 * @property {string} kind project, connector, transaction, sequence, sequence.child, ngx.application, ngx.page,
 *           ngx.shared, ngx.component or object
 * @property {string} sequenceId the sequence holding the object, empty for none
 * @property {string} pageId the NGX page holding the object, empty for none
 * @property {string} applicationId the NGX application holding the object, empty for none
 * @property {string=} change the change the agent made, when its MCP tells it: created, changed, renamed, moved or
 *           deleted (then the object is the parent of the one removed)
 * @property {string=} previous the QName the object had before a rename or a move, or the one removed
 * @property {string=} tool the tool of the MCP that made the change
 * @property {string=} summary what the MCP tells of the change, in a few words
 * @property {string[]=} touched the other objects the same tool call changed, by their full QNames
 */

/**
 * @param {ReturnType<typeof import('#lib/admin/adminEvents.js').parseAdminEvent>} event
 * @returns {StudioRevealTarget | null}
 */
export function studioRevealTarget(event) {
	if (event?.topic !== STUDIO_REVEAL_TOPIC) return null;
	const payload = event.payload ?? {};
	const projectName = String(payload.project ?? '').trim();
	const id = String(payload.id ?? '').trim();
	// the id of an object starts with the name of its project
	if (!projectName || !id || id.split(/[.:/]/)[0] !== projectName) return null;
	/** @type {StudioRevealTarget} */
	const target = {
		projectName,
		id,
		kind: String(payload.kind ?? '').trim() || 'object',
		sequenceId: String(payload.sequence ?? '').trim(),
		pageId: String(payload.page ?? '').trim(),
		applicationId: String(payload.application ?? '').trim()
	};
	// what the MCP of an engine of 8.5 may tell of the change, which an older one does not
	for (const key of /** @type {const} */ (['change', 'previous', 'tool', 'summary'])) {
		const value = String(payload[key] ?? '').trim();
		if (value) target[key] = value.slice(0, key === 'summary' ? 200 : 400);
	}
	if (Array.isArray(payload.touched)) {
		const touched = payload.touched
			.map((/** @type {unknown} */ value) => String(value ?? '').trim())
			.filter((/** @type {string} */ value) => value && value.split(/[.:/]/)[0] === projectName);
		if (touched.length) target.touched = touched.slice(0, 50);
	}
	return target;
}

/**
 * @param {StudioRevealTarget} target
 * @returns {boolean} whether the object shows in the preview of its NGX application
 */
export function revealsFrontend(target) {
	return target.kind.startsWith('ngx.');
}

/**
 * @param {StudioRevealTarget} target
 * @returns {boolean} whether the object shows in the Flow of its sequence
 */
export function revealsFlow(target) {
	return Boolean(target.sequenceId);
}

/**
 * @param {string | undefined} preference vibe (the default), always or never
 * @param {string} profile the profile of the Studio
 * @returns {boolean} whether the Studio follows the objects the agent reveals
 */
export function followsAgent(preference, profile) {
	if (preference === 'always') return true;
	if (preference === 'never') return false;
	return profile === 'vibe';
}

const NOT_TYPED_INPUTS = new Set([
	'button',
	'checkbox',
	'color',
	'file',
	'hidden',
	'image',
	'radio',
	'range',
	'reset',
	'submit'
]);

/**
 * @param {Element | null | undefined} element
 * @returns {boolean} whether the user types in the element, as a field of the properties or a code editor
 */
export function isEditableElement(element) {
	if (!element || element.nodeType !== 1) return false;
	const html = /** @type {HTMLElement} */ (element);
	if (html.isContentEditable) return true;
	const tag = html.tagName;
	if (tag === 'TEXTAREA' || tag === 'SELECT') return true;
	if (tag === 'INPUT') {
		return !NOT_TYPED_INPUTS.has(String(/** @type {HTMLInputElement} */ (html).type).toLowerCase());
	}
	return false;
}

/**
 * Follows the reveals of an agent at a pace the user can see: an agent makes dozens of changes a minute, in bursts.
 * Only the last reveal waiting is shown, one at most every interval, and none while the user works in the Studio:
 * it waits until the user has stopped for idleDelay and isBusy tells the Studio is free, as no field being typed in.
 *
 * @param {{
 *  apply: (target: StudioRevealTarget) => void | Promise<void>,
 *  isBusy?: () => boolean,
 *  settleDelay?: number,
 *  interval?: number,
 *  idleDelay?: number,
 *  busyRetry?: number,
 *  now?: () => number,
 *  setTimer?: (callback: () => void, delay: number) => any,
 *  clearTimer?: (timer: any) => void
 * }} options settleDelay: the time a first reveal waits, as the refresh of the tree its change asks
 */
export function createRevealFollower({
	apply,
	isBusy = () => false,
	settleDelay = 300,
	interval = 1500,
	idleDelay = 4000,
	busyRetry = 500,
	now = () => Date.now(),
	setTimer = (callback, delay) => setTimeout(callback, delay),
	clearTimer = (timer) => clearTimeout(timer)
}) {
	/** @type {StudioRevealTarget | null} */
	let pending = null;
	/** @type {any} */
	let timer;
	let lastApplied = -Infinity;
	let lastActivity = -Infinity;
	let running = false;
	let disposed = false;

	/** @param {number} delay */
	function schedule(delay) {
		if (disposed) return;
		clearTimer(timer);
		timer = setTimer(flush, Math.max(0, delay));
	}

	async function flush() {
		timer = undefined;
		if (!pending || running || disposed) return;
		const time = now();
		const wait = Math.max(lastActivity + idleDelay - time, lastApplied + interval - time);
		if (wait > 0) {
			schedule(wait);
			return;
		}
		if (isBusy()) {
			schedule(busyRetry);
			return;
		}
		const target = pending;
		pending = null;
		lastApplied = time;
		running = true;
		try {
			await apply(target);
		} catch (error) {
			console.warn('Unable to show the object the agent revealed', error);
		} finally {
			running = false;
			if (pending) schedule(0);
		}
	}

	return {
		/** @param {StudioRevealTarget | null} target the last reveal, which replaces the one waiting */
		push(target) {
			if (!target || disposed) return;
			pending = target;
			if (timer === undefined && !running) schedule(settleDelay);
		},
		/** the user works in the Studio: the reveal waiting waits for the user to stop */
		activity() {
			lastActivity = now();
		},
		/** forgets the reveal waiting, as when the Studio stops following the agent */
		clear() {
			pending = null;
			clearTimer(timer);
			timer = undefined;
		},
		dispose() {
			disposed = true;
			pending = null;
			clearTimer(timer);
			timer = undefined;
		},
		get pending() {
			return pending;
		}
	};
}
