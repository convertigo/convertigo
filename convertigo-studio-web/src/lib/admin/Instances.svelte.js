import { call } from '#lib/utils/service.js';

const INSTANCE_HEADER = 'x-c8o-instance';
const UNREACHABLE_HEADER = 'x-c8o-instance-unreachable';
const LOCAL_HEADER = 'x-c8o-instance-local';
const ERROR_HEADER = 'x-c8o-instance-error';

/** @typedef {{ instanceId: string, routingId: string } & Record<string, any>} InstanceInfo */

let state = $state({
	storeMode: '',
	/** @type {InstanceInfo[]} */
	instances: [],
	loading: false,
	current: '',
	active: '',
	revision: 0,
	/** @type {{ instance: string, local: string, cause: string } | null} */
	unreachable: null
});

function normalizeInstance(value) {
	if (typeof value !== 'string') return '';
	const trimmed = value.trim();
	if (!trimmed || trimmed.toLowerCase() === 'auto') return '';
	return trimmed;
}

function setCurrent(value) {
	const current = normalizeInstance(value);
	if (current === state.current) return;
	state.current = current;
	state.unreachable = null;
	state.revision += 1;
}

export default {
	get storeMode() {
		return state.storeMode;
	},
	get instances() {
		return state.instances;
	},
	/** The choices of the selector; a selection whose discovery entry expired stays among them. */
	get options() {
		const options = state.instances.map(({ instanceId, routingId }) => ({
			value: routingId,
			text: instanceId
		}));
		if (state.current && !options.some((option) => option.value === state.current)) {
			options.push({ value: state.current, text: state.current });
		}
		return options;
	},
	get loading() {
		return state.loading;
	},
	get current() {
		return state.current;
	},
	set current(value) {
		setCurrent(value);
	},
	get revision() {
		return state.revision;
	},
	get active() {
		return state.active;
	},
	get activeName() {
		return state.instances.find((info) => info.routingId === state.active)?.instanceId ?? '';
	},
	/** Set when the instance chosen in the console cannot be reached from the instance that answered. */
	get unreachable() {
		return state.unreachable;
	},

	apply(headers, instance = state.current) {
		if (instance) headers[INSTANCE_HEADER] = instance;
		return state.revision;
	},
	/** @returns {boolean} true when the response reports that the chosen instance is unreachable */
	update(response, revision = state.revision) {
		if (revision !== state.revision) return false;
		const headers = response?.headers;
		const unreachable = normalizeInstance(headers?.get?.(UNREACHABLE_HEADER));
		if (unreachable) {
			// An in-flight request for the previous selection must not restore its banner.
			if (state.current && unreachable !== state.current) return true;
			state.unreachable = {
				instance: unreachable,
				local: normalizeInstance(headers.get(LOCAL_HEADER)),
				cause: (headers.get(ERROR_HEADER) ?? '').trim()
			};
			return true;
		}
		const header = headers?.get?.(INSTANCE_HEADER);
		const normalized = normalizeInstance(header);
		// Only an answer from the selected target can clear its outage report.
		if (state.unreachable && normalized && normalized !== state.unreachable.instance) return true;
		if (normalized) {
			state.active = normalized;
			setCurrent(normalized);
		}
		if (state.unreachable && normalized === state.unreachable.instance) {
			state.unreachable = null;
		}
		return false;
	},
	async refresh(force = false) {
		if (state.loading) return;
		if (!force && (state.storeMode || state.instances.length)) return;
		state.loading = true;
		try {
			// Discovery must remain reachable even after the selected pod has disappeared.
			const res = await call('engine.ListInstances', {}, { instance: '' });
			// A failed discovery keeps the selector as it was.
			if (res?.error || typeof res?.storeMode !== 'string' || !Array.isArray(res?.instances))
				return;
			state.storeMode = res.storeMode;
			state.instances = res.instances;
			// A selection whose discovery entry expired is kept: requests to it report it unreachable
			// instead of silently running on the local instance.
			if (!state.current) {
				setCurrent(res.localRoutingId);
				state.active = state.current;
			}
		} finally {
			state.loading = false;
		}
	}
};
