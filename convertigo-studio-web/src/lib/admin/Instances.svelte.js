import { call } from '#lib/utils/service.js';

const INSTANCE_HEADER = 'x-c8o-instance';

/** @typedef {{ instanceId: string, routingId: string } & Record<string, any>} InstanceInfo */

let state = $state({
	storeMode: '',
	/** @type {InstanceInfo[]} */
	instances: [],
	loading: false,
	current: '',
	active: '',
	revision: 0
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
	state.revision += 1;
}

export default {
	get storeMode() {
		return state.storeMode;
	},
	get instances() {
		return state.instances;
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

	apply(headers, instance = state.current) {
		if (instance) headers[INSTANCE_HEADER] = instance;
		return state.revision;
	},
	update(response, revision = state.revision) {
		if (revision !== state.revision) return;
		const header = response?.headers?.get?.(INSTANCE_HEADER);
		const normalized = normalizeInstance(header);
		if (normalized) {
			state.active = normalized;
			setCurrent(normalized);
		}
	},
	async refresh(force = false) {
		if (state.loading) return;
		if (!force && (state.storeMode || state.instances.length)) return;
		state.loading = true;
		try {
			// Discovery must remain reachable even after the selected pod has disappeared.
			const res = await call('engine.ListInstances', {}, { instance: '' });
			if (!Array.isArray(res?.instances)) return;
			state.storeMode = res?.storeMode ?? '';
			state.instances = Array.isArray(res?.instances) ? res.instances : [];
			if (!state.current || !state.instances.some((info) => info.routingId === state.current)) {
				setCurrent(res?.localRoutingId);
				state.active = state.current;
			}
		} finally {
			state.loading = false;
		}
	}
};
