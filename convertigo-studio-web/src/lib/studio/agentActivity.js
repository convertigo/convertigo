/**
 * What an agent of the Assistant does in the projects, as the web Studio following it tells it: the kind of change of
 * each object it reveals, the activity feed of its changes, the view each kind of object shows in, as a perspective
 * of the Eclipse Studio, and when following it pauses for the user working in the Studio.
 *
 * The MCP of the Assistant may tell the change it made (the studio.reveal event carries it then); else the change is
 * inferred from what the tree saw appear or vanish around the reveal: an object that just appeared was created, one
 * appearing as a sibling vanishes was renamed, elsewhere moved, a parent revealed as a child vanishes lost it.
 */

import {
	equivalentDboObjectIds,
	isDescendantObjectId,
	objectNameFromId,
	parentObjectId
} from './dnd';

/**
 * @typedef {'created' | 'changed' | 'renamed' | 'moved' | 'deleted' | 'opened'} AgentChange opened: an object the
 * agent shows without changing it, as the application whose builder it opens
 */

/** @type {AgentChange[]} */
export const AGENT_CHANGES = ['created', 'changed', 'renamed', 'moved', 'deleted', 'opened'];

/**
 * @param {unknown} value
 * @returns {AgentChange | ''} a change of the agent, as the MCP names it, '' for none or an unknown one
 */
export function agentChange(value) {
	const change = String(value ?? '')
		.trim()
		.toLowerCase();
	const aliases = /** @type {Record<string, AgentChange>} */ ({
		create: 'created',
		add: 'created',
		added: 'created',
		update: 'changed',
		updated: 'changed',
		modified: 'changed',
		rename: 'renamed',
		move: 'moved',
		delete: 'deleted',
		remove: 'deleted',
		removed: 'deleted',
		open: 'opened',
		shown: 'opened'
	});
	const normalized = aliases[change] ?? change;
	return AGENT_CHANGES.includes(/** @type {AgentChange} */ (normalized))
		? /** @type {AgentChange} */ (normalized)
		: '';
}

/** the names of the types of the objects, by the short name of their folder in their ids (FolderType) */
const TYPE_NAMES = /** @type {Record<string, string>} */ ({
	ac: 'action',
	at: 'attribute',
	ah: 'authentication',
	cn: 'connector',
	ct: 'control',
	cr: 'criteria',
	dc: 'document',
	ev: 'event',
	er: 'extraction rule',
	fn: 'function',
	id: 'index',
	ls: 'listener',
	mp: 'mapping',
	mn: 'menu',
	op: 'operation',
	pg: 'page',
	pr: 'parameter',
	pf: 'platform',
	pl: 'pool',
	rf: 'reference',
	rs: 'response',
	rt: 'route',
	sc: 'screen class',
	sq: 'sequence',
	sa: 'shared action',
	sp: 'shared component',
	sh: 'sheet',
	sr: 'source',
	st: 'step',
	sl: 'style',
	tp: 'template',
	tc: 'test case',
	tr: 'transaction',
	vl: 'validator',
	vr: 'variable'
});

/** the names of the kinds of the objects the engine reveals, for the objects whose id tells no type */
const KIND_NAMES = /** @type {Record<string, string>} */ ({
	project: 'project',
	connector: 'connector',
	transaction: 'transaction',
	sequence: 'sequence',
	'sequence.child': 'object',
	'ngx.application': 'application',
	'ngx.page': 'page',
	'ngx.shared': 'shared component',
	'ngx.component': 'component'
});

/**
 * @param {string} id the id of an object in the tree, its full QName, as Shop.sq:Checkout.st:copy
 * @param {string=} kind the kind of the object, as the engine reveals it
 * @returns {{ name: string, type: string }} its name and the name of its type
 */
export function objectOf(id, kind = '') {
	const value = String(id ?? '');
	const last = value.slice(value.lastIndexOf('.') + 1);
	const typed = last.match(/^([a-z]{2,4}):(.+)$/);
	const name = objectNameFromId(value) || last;
	if (!value.includes('.')) {
		return { name: value, type: kind && kind !== 'object' ? (KIND_NAMES[kind] ?? '') : 'project' };
	}
	return {
		name: typed?.[2] ?? name,
		type:
			(typed && TYPE_NAMES[typed[1]]) ||
			// a child of a step named without its type, as the fields of a JSON object step
			(kind === 'sequence.child' && /\.st:[^.]+\./.test(value) ? 'step' : '') ||
			KIND_NAMES[kind] ||
			'object'
	};
}

/**
 * @typedef {{ type: 'appeared' | 'vanished', id: string, parentId: string, name: string, label: string,
 *  icon: string, at: number }} TreeChangeEvent a child the tree saw appear in or vanish from a branch it had read
 */

/**
 * Remembers what the tree reads, to tell the changes of the agent its events do not name: the children that
 * appear in or vanish from a branch read again, with their names and icons.
 *
 * @param {{ now?: () => number, keep?: number }} [options] keep: how long an event is remembered
 */
export function createTreeChangeLog({ now = () => Date.now(), keep = 20_000 } = {}) {
	/** @type {TreeChangeEvent[]} */
	let events = [];
	/** @type {Map<string, { name: string, label: string, icon: string, parentId: string }>} */
	const nodes = new Map();
	/** @type {Map<string, Map<string, any>>} the children of each branch read, by their ids */
	const snapshots = new Map();
	/** @type {Set<string>} the objects that appeared, not renamed nor moved: their children are new too */
	const created = new Set();

	/** @param {any} node @param {string} parentId */
	function remember(node, parentId) {
		const id = String(node?.id ?? '');
		if (!id) return null;
		const info = {
			name: String(node.name ?? node.label ?? objectOf(id).name),
			label: String(node.label ?? node.name ?? ''),
			icon: typeof node.icon === 'string' && node.icon !== 'folder' ? node.icon : '',
			parentId
		};
		nodes.set(id, info);
		return info;
	}

	/**
	 * @param {string} parentId
	 * @param {any} previous the children the tree had, not an array for a branch it never read: the ones read before
	 *        then, as by an earlier read of the project
	 * @param {any[]} next the children read
	 * @returns {TreeChangeEvent[]} what appeared or vanished
	 */
	function children(parentId, previous, next) {
		const at = now();
		/** @type {TreeChangeEvent[]} */
		const changes = [];
		/** @param {string} parent @param {any[] | undefined} before @param {any[]} after */
		const compare = (parent, before, after) => {
			/** @type {Map<string, any> | undefined} */
			let beforeById;
			if (Array.isArray(before)) {
				beforeById = new Map();
				for (const node of before) {
					if (node?.id && !node.pending) beforeById.set(String(node.id), node);
				}
			} else {
				// a branch read first: the one read before, or none for an object created since
				beforeById = snapshots.get(parent) ?? (created.has(parent) ? new Map() : undefined);
			}
			const listed = after.filter((node) => node?.id && !node.tagGroup && !node.pending);
			const afterIds = new Set(listed.map((node) => String(node.id)));
			// the types of the children gone, as one renamed: the one appearing beside is not new, nor its children
			const goneTypes = new Set(
				[...(beforeById?.keys() ?? [])].filter((id) => !afterIds.has(id)).map(typeCode)
			);
			/** @type {Map<string, any>} */
			const afterById = new Map();
			for (const node of listed) {
				const id = String(node.id);
				afterById.set(id, { id, name: node.name, label: node.label, icon: node.icon });
				const info = remember(node, parent);
				const old = beforeById?.get(id);
				if (beforeById && !old && info) {
					changes.push({ type: 'appeared', id, ...info, parentId: parent, at });
					if (!goneTypes.has(typeCode(id))) created.add(id);
				}
				if (Array.isArray(node.children)) {
					compare(id, old?.children, node.children);
				}
			}
			for (const [id, node] of beforeById ?? []) {
				if (!afterIds.has(id)) {
					const info = nodes.get(id) ?? remember(node, parent);
					if (info) changes.push({ type: 'vanished', id, ...info, parentId: parent, at });
				}
			}
			snapshots.set(parent, afterById);
		};
		if (parentId && Array.isArray(next)) {
			compare(parentId, previous, next);
		}
		events = [...events.filter((event) => at - event.at <= keep), ...changes];
		return changes;
	}

	return {
		children,
		/** @returns {TreeChangeEvent[]} the events remembered, the oldest first */
		get events() {
			const at = now();
			return events.filter((event) => at - event.at <= keep);
		},
		/**
		 * @param {string} id an object
		 * @returns {boolean} whether it is new: a branch read before, holding it or one of its parents, did not have it
		 */
		isNew(id) {
			for (let current = String(id ?? ''); current; current = parentObjectId(current)) {
				const parent = parentObjectId(current);
				if (!parent) return false;
				// its parent, or the folder of its type in its parent
				const snapshot = snapshots.get(parent) ?? snapshots.get(`${parent}:${typeCode(current)}`);
				if (snapshot) {
					return !equivalentDboObjectIds(current).some((equivalent) => snapshot.has(equivalent));
				}
			}
			return false;
		},
		/** @param {string} id @returns {{ name: string, label: string, icon: string, parentId: string } | undefined} */
		node(id) {
			for (const equivalent of equivalentDboObjectIds(id)) {
				const node = nodes.get(equivalent);
				if (node) return node;
			}
			return undefined;
		},
		clear() {
			events = [];
		}
	};
}

/**
 * @typedef {{ change: AgentChange, explicit: boolean, previousId?: string, previousName?: string,
 *  removed?: { id: string, name: string, parentId: string, icon: string } }} AgentClassification
 */

/**
 * @param {string} left
 * @param {string} right
 * @returns {boolean}
 */
function sameObject(left, right) {
	const ids = equivalentDboObjectIds(right);
	return equivalentDboObjectIds(left).some((id) => ids.includes(id));
}

/**
 * @param {string} id
 * @returns {string} the short name of the type of an object, as st, '' for none
 */
function typeCode(id) {
	return String(id ?? '').match(/\.([a-z]{2,4}):[^.]+$/)?.[1] ?? '';
}

/**
 * The change of an object an agent reveals: the one its event tells, else the one the tree saw around it.
 *
 * @param {{ id: string, kind?: string, change?: string, previous?: string }} target
 * @param {TreeChangeEvent[]} events what the tree saw appear or vanish
 * @param {{ at: number, window?: number, isNew?: (id: string) => boolean }} options at: when the agent revealed it;
 *        window: how far the changes the tree saw go with it; isNew: whether a branch read before did not have it
 * @returns {AgentClassification}
 */
export function classifyChange(target, events, { at, window = 8_000, isNew }) {
	const near = events.filter((event) => Math.abs(event.at - at) <= window);
	const explicit = agentChange(target.change);
	const appeared = near.findLast(
		(event) => event.type === 'appeared' && sameObject(event.id, target.id)
	);
	const vanished = near.filter(
		(event) => event.type === 'vanished' && !sameObject(event.id, target.id)
	);
	/** @type {AgentClassification} */
	const result = { change: explicit || 'changed', explicit: Boolean(explicit) };
	const previous = String(target.previous ?? '').trim();
	if (previous) {
		result.previousId = previous;
		result.previousName = objectOf(previous).name;
	}
	if (appeared) {
		const name = objectOf(target.id).name;
		const sibling = vanished.findLast(
			(event) => event.parentId === appeared.parentId && typeCode(event.id) === typeCode(target.id)
		);
		const elsewhere = vanished.findLast(
			(event) => event.parentId !== appeared.parentId && objectOf(event.id).name === name
		);
		const from = sibling ?? elsewhere;
		if (!explicit) {
			result.change = sibling ? 'renamed' : elsewhere ? 'moved' : 'created';
		}
		if (from && !result.previousId && (result.change === 'renamed' || result.change === 'moved')) {
			result.previousId = from.id;
			result.previousName = from.name;
		}
		return result;
	}
	// a parent the agent reveals once it removed a child, as the MCP does after a deletion
	const removed = vanished.findLast(
		(event) =>
			isDescendantObjectId(event.id, target.id) &&
			(!previous || sameObject(event.id, previous) || event.name === result.previousName)
	);
	if (!explicit && !removed && target.kind === 'ngx.application') {
		// the application of a project, as the agent opens its builder to work on it
		result.change = 'opened';
		return result;
	}
	if (removed && (!explicit || explicit === 'deleted')) {
		result.change = 'deleted';
		result.removed = {
			id: removed.id,
			name: removed.name,
			parentId: removed.parentId,
			icon: removed.icon
		};
	} else if (explicit === 'deleted' && previous) {
		result.removed = {
			id: previous,
			name: objectOf(previous).name,
			parentId: '',
			icon: ''
		};
	} else if (!explicit && isNew?.(target.id)) {
		// not seen appearing, as the tree does not read it again while following pauses, but new to what it read
		result.change = 'created';
	}
	return result;
}

/**
 * @typedef {Object} AgentEntry an action of the agent in its activity feed
 * @property {number} key
 * @property {number} at when the agent made it first
 * @property {number} updatedAt when the agent made it last
 * @property {string} id the object revealed
 * @property {string} projectName
 * @property {string} kind
 * @property {AgentChange} change
 * @property {boolean} explicit whether the MCP told the change, which the tree does not change then
 * @property {string} name
 * @property {string} type
 * @property {string} previousName
 * @property {string} removedName
 * @property {string} removedType
 * @property {string} tool the tool of the MCP that made it, when told
 * @property {string} summary what the MCP told of it
 * @property {number} count how many times the agent changed it in a row
 * @property {import('./studioReveal.js').StudioRevealTarget} target
 */

/**
 * The activity feed of the agent, the last action first: an object the agent changes again and again in a while makes
 * one entry, first again at each change.
 *
 * @param {{ now?: () => number, max?: number, mergeWindow?: number }} [options]
 */
export function createAgentFeed({ now = () => Date.now(), max = 60, mergeWindow = 90_000 } = {}) {
	/** @type {AgentEntry[]} */
	let entries = [];
	let serial = 0;

	/**
	 * @param {AgentEntry} entry
	 * @param {AgentClassification} classification
	 */
	function describe(entry, classification) {
		entry.change = classification.change;
		entry.explicit = classification.explicit;
		entry.previousName = classification.previousName ?? '';
		entry.removedName = classification.removed?.name ?? '';
		entry.removedType = classification.removed ? objectOf(classification.removed.id).type : '';
	}

	return {
		/**
		 * @param {import('./studioReveal.js').StudioRevealTarget} target
		 * @param {AgentClassification} classification
		 * @returns {AgentEntry} the new entry, or the one of its object the agent changed lately, first again
		 */
		add(target, classification) {
			const at = now();
			const index = entries.findIndex(
				(item) =>
					at - item.updatedAt <= mergeWindow &&
					sameObject(item.id, target.id) &&
					classification.change !== 'deleted' &&
					item.change !== 'deleted' &&
					(classification.change === 'changed' || classification.change === item.change)
			);
			if (index >= 0) {
				const entry = entries[index];
				entry.updatedAt = at;
				entry.count += 1;
				entry.target = target;
				entry.summary = target.summary || entry.summary;
				entry.tool = target.tool || entry.tool;
				// an application opened again, as the agent checks its build, stays where it is
				if (entry.change !== 'opened') {
					entries = [entry, ...entries.filter((_, position) => position !== index)];
				} else {
					entries = [...entries];
				}
				return entry;
			}
			const object = objectOf(target.id, target.kind);
			/** @type {AgentEntry} */
			const entry = {
				key: ++serial,
				at,
				updatedAt: at,
				id: target.id,
				projectName: target.projectName,
				kind: target.kind,
				change: 'changed',
				explicit: false,
				name: object.name,
				type: object.type,
				previousName: '',
				removedName: '',
				removedType: '',
				tool: target.tool ?? '',
				summary: target.summary ?? '',
				count: 1,
				target
			};
			describe(entry, classification);
			entries = [entry, ...entries].slice(0, max);
			return entry;
		},
		/**
		 * The change of an entry the tree tells better, as an object seen appearing after its reveal.
		 * @param {number} key
		 * @param {AgentClassification} classification
		 * @returns {boolean} whether it changed
		 */
		update(key, classification) {
			const entry = entries.find((item) => item.key === key);
			if (
				!entry ||
				entry.explicit ||
				(entry.change === classification.change &&
					entry.previousName === (classification.previousName ?? '') &&
					entry.removedName === (classification.removed?.name ?? ''))
			) {
				return false;
			}
			describe(entry, classification);
			entries = [...entries];
			return true;
		},
		/** @returns {AgentEntry[]} the last action first */
		get entries() {
			return entries;
		},
		clear() {
			entries = [];
		}
	};
}

const VERBS = /** @type {Record<AgentChange, string>} */ ({
	created: 'Created',
	changed: 'Changed',
	renamed: 'Renamed',
	moved: 'Moved',
	deleted: 'Deleted',
	opened: 'Opened'
});

/**
 * @param {AgentEntry} entry
 * @returns {{ verb: string, type: string, name: string, detail: string }} what the feed tells of an action
 */
export function describeEntry(entry) {
	if (entry.change === 'deleted') {
		return {
			verb: VERBS.deleted,
			type: entry.removedType || (entry.removedName ? 'object' : ''),
			name: entry.removedName || '',
			detail: entry.removedName ? `from ${entry.name}` : `in ${entry.type} ${entry.name}`
		};
	}
	return {
		verb: VERBS[entry.change] ?? VERBS.changed,
		type: entry.type,
		name: entry.name,
		detail:
			entry.change === 'renamed' && entry.previousName
				? `was ${entry.previousName}`
				: entry.change === 'moved' && entry.previousName && entry.previousName !== entry.name
					? `was ${entry.previousName}`
					: entry.count > 1
						? `${entry.count} changes`
						: ''
	};
}

/**
 * @param {number} at
 * @param {number} time now
 * @returns {string} how long ago, as a feed tells it
 */
export function elapsedLabel(at, time) {
	const seconds = Math.max(0, Math.round((time - at) / 1000));
	if (seconds < 5) return 'now';
	if (seconds < 60) return `${seconds} s`;
	const minutes = Math.round(seconds / 60);
	if (minutes < 60) return `${minutes} min`;
	return `${Math.round(minutes / 60)} h`;
}

/**
 * The view an object of the agent shows in, as a perspective of the Eclipse Studio: its application for an NGX
 * object, the Flow of its sequence, the Execution view of a transaction; '' for the tree only.
 *
 * @param {{ kind: string, sequenceId?: string }} target
 * @returns {'frontend' | 'flow' | 'execution' | ''}
 */
export function followView(target) {
	if (String(target.kind).startsWith('ngx.')) return 'frontend';
	if (target.sequenceId) return 'flow';
	if (target.kind === 'transaction') return 'execution';
	return '';
}

/**
 * When the Studio follows the agent: it pauses when the user works in the Studio while the agent works, or as the
 * user asks, and offers to resume. A pause of the user's work ends once the agent stopped working for a while, as
 * its next request follows again; one asked stays until the user resumes.
 *
 * @param {{ now?: () => number, activeWindow?: number }} [options] activeWindow: how long after its last change the
 *        agent is still taken for working, as it may think for a minute or more between two changes
 */
export function createFollowPolicy({ now = () => Date.now(), activeWindow = 180_000 } = {}) {
	let lastAgent = -Infinity;
	/** @type {'' | 'user' | 'manual'} */
	let reason = '';

	return {
		/** the agent changed something */
		agent() {
			lastAgent = now();
		},
		/** @returns {boolean} whether the agent changed something lately */
		get working() {
			return now() - lastAgent <= activeWindow;
		},
		/** @returns {number} when the agent is no longer taken for working */
		get idleAt() {
			return lastAgent + activeWindow;
		},
		/**
		 * The user works in the Studio.
		 * @returns {boolean} whether following pauses now
		 */
		userActed() {
			if (reason || now() - lastAgent > activeWindow) return false;
			reason = 'user';
			return true;
		},
		/** @param {'user' | 'manual'} why */
		pause(why) {
			if (reason !== 'manual') reason = why;
		},
		resume() {
			reason = '';
		},
		/**
		 * @returns {boolean} whether a pause of the user's work ended, the agent having stopped working
		 */
		expire() {
			if (reason === 'user' && now() - lastAgent > activeWindow) {
				reason = '';
				return true;
			}
			return false;
		},
		get paused() {
			return Boolean(reason);
		},
		get reason() {
			return reason;
		}
	};
}
