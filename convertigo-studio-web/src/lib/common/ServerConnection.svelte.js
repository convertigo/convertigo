const SERVER_UNAVAILABLE_STATUSES = new Set([502, 503, 504]);

export function isServerUnavailableStatus(status) {
	return SERVER_UNAVAILABLE_STATUSES.has(Number(status));
}

export function createServerConnectionTracker() {
	let state = $state({
		unavailable: false,
		status: undefined,
		statusText: ''
	});

	return {
		get unavailable() {
			return state.unavailable;
		},
		get status() {
			return state.status;
		},
		get statusText() {
			return state.statusText;
		},
		markUnavailable(status, statusText = '') {
			const changed = !state.unavailable;
			state.unavailable = true;
			state.status = status;
			state.statusText = statusText;
			return changed;
		},
		markReachable() {
			if (!state.unavailable) return false;
			state.unavailable = false;
			state.status = undefined;
			state.statusText = '';
			return true;
		}
	};
}

export default createServerConnectionTracker();
