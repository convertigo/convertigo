import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export const connectionsStore = writable([]);
export const sessionsStore = writable([]);

export const connections = writable('');
export const contextsInUse = writable('');
export const contextsNumber = writable('');
export const httpTimeout = writable('');
export const sessions = writable('');
export const sessionsInUse = writable('');
export const sessionsIsOverFlow = writable('');
export const sessionsNumber = writable('');
export const threadsInUse = writable('');
export const threadsNumber = writable('');

const all = {
	connections,
	contextsInUse,
	contextsNumber,
	httpTimeout,
	sessions,
	sessionsInUse,
	sessionsIsOverFlow,
	sessionsNumber,
	threadsInUse,
	threadsNumber
};

let init = false;

export async function connectionsCheck() {
	if (!init) {
		init = true;
		const response = await call('connections.List');
		console.log('connection list response :', response);
		if (response?.admin) {
			for (let k in all) {
				if (k in response.admin) {
					all[k].set(response.admin[k]);
				}
			}
		}

		if (response?.admin?.connections) {
			if (!Array.isArray(response.admin.connections.connection)) {
				response.admin.connections.connection = [response.admin.connections.connection];
			}
			connectionsStore.set(response.admin.connections.connection);
		}
		if (response?.admin?.sessions) {
			if (!Array.isArray(response.admin.sessions.session)) {
				response.admin.sessions.session = [response.admin.sessions.session];
			}
			sessionsStore.set(response.admin.sessions.session);
		}
	}
}
