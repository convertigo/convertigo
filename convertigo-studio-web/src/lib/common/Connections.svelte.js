import { browser } from '$app/environment';
import { call } from '$lib/utils/service';

const all = $state({
	delay: 2000,
	sessions: Array(5).fill({ '@_sessionID': null }),
	connections: Array(10).fill({ '@_contextName': null }),
	contextsInUse: null,
	contextsNumber: null,
	httpTimeout: null,
	sessionsInUse: null,
	sessionsIsOverFlow: null,
	sessionsNumber: null,
	threadsInUse: null,
	threadsNumber: null
});

let selectedSession = $state('');

let interval = null;

function connectionsCheck() {
	if (browser && interval == null && all.delay > 0) {
		interval = window.setInterval(async () => {
			const response = await call('connections.List', { session: selectedSession });

			if (response?.admin?.connections) {
				response.admin.connections = Array.isArray(response.admin.connections.connection)
					? response.admin.connections.connection
					: [response.admin.connections.connection];
			}
			if (response?.admin?.sessions) {
				response.admin.sessions = Array.isArray(response.admin.sessions.session)
					? response.admin.sessions.session
					: [response.admin.sessions.session];
			}
			if (response?.admin) {
				for (let k in all) {
					if (k in response.admin) {
						all[k] = response.admin[k];
					}
				}
			}
		}, all.delay);
	} else if (all.delay <= 0) {
		if (browser) {
			window.clearInterval(interval);
		}
		interval = null;
	}
}

function stop() {
	if (browser) {
		window.clearInterval(interval);
	}
	interval = null;
}

function refresh() {
	stop();
	connectionsCheck();
}

async function callDelete(params) {
	await call('connections.Delete', params);
	refresh();
}

export default {
	get connections() {
		connectionsCheck();
		return all.connections;
	},
	get contextsInUse() {
		connectionsCheck();
		return all.contextsInUse;
	},
	get contextsNumber() {
		connectionsCheck();
		return all.contextsNumber;
	},
	get httpTimeout() {
		connectionsCheck();
		return all.httpTimeout;
	},
	get sessions() {
		connectionsCheck();
		return all.sessions;
	},
	get sessionsInUse() {
		connectionsCheck();
		return all.sessionsInUse;
	},
	get sessionsIsOverFlow() {
		connectionsCheck();
		return all.sessionsIsOverFlow;
	},
	get sessionsNumber() {
		connectionsCheck();
		return all.sessionsNumber;
	},
	get threadsInUse() {
		connectionsCheck();
		return all.threadsInUse;
	},
	get threadsNumber() {
		connectionsCheck();
		return all.threadsNumber;
	},
	get selectedSession() {
		return selectedSession;
	},
	set selectedSession(value) {
		selectedSession = value;
		refresh();
	},
	refresh,
	stop,
	async deleteSession(sessionId) {
		await callDelete({ sessionId });
	},
	async deleteContext(contextName) {
		await callDelete({ contextName });
	},
	async deleteAll() {
		await callDelete({ removeAll: true });
	}
};
