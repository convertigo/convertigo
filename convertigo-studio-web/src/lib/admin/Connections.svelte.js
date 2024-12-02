import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call } from '$lib/utils/service';

const defValues = {
	sessions: Array(5).fill({ sessionID: null }),
	connections: Array(10).fill({ contextName: null }),
	contextsInUse: null,
	contextsNumber: null,
	httpTimeout: null,
	sessionsInUse: null,
	sessionsIsOverFlow: null,
	sessionsNumber: null,
	threadsInUse: null,
	threadsNumber: null
};
let selectedSession = $state('');

let values = {
	async deleteSession(sessionId) {
		await callDelete({ sessionId });
	},
	async deleteContext(contextName) {
		await callDelete({ contextName });
	},
	async deleteAll() {
		await callDelete({ removeAll: true });
	},
	get selectedSession() {
		return selectedSession;
	},
	set selectedSession(value) {
		selectedSession = value;
		values.refresh();
	}
};

async function callDelete(params) {
	await call('connections.Delete', params);
	values.refresh();
}

export default ServiceHelper({
	defValues,
	delay: 2000,
	values,
	arrays: ['admin.connections.connection', 'admin.sessions.session'],
	mapping: {
		'': 'admin',
		connections: 'admin.connections.connection',
		sessions: 'admin.sessions.session'
	},
	service: async () => {
		return await call('connections.List', { session: selectedSession });
	}
});
