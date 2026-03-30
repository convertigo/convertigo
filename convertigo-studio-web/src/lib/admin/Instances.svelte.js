import { call } from '$lib/utils/service';

const INSTANCE_HEADER = 'x-c8o-instance';

/** @typedef {{ instanceId: string } & Record<string, any>} InstanceInfo */

let state = $state({
	storeMode: '',
	/** @type {InstanceInfo[]} */
	instances: [],
	loading: false,
	current: '',
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

	apply(headers) {
		if (state.current) headers[INSTANCE_HEADER] = state.current;
	},
	update(response) {
		const header = response?.headers?.get?.(INSTANCE_HEADER);
		const normalized = normalizeInstance(header);
		if (normalized) setCurrent(normalized);
	},
	async refresh(force = false) {
		if (state.loading) return;
		if (!force && (state.storeMode || state.instances.length)) return;
		state.loading = true;
		try {
			const res = await call('engine.ListInstances');
			state.storeMode = res?.storeMode ?? '';
			state.instances = Array.isArray(res?.instances) ? res.instances : [];
		} finally {
			state.loading = false;
		}
	}
};
