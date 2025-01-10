import { call } from '$lib/utils/service';
import Time from './Time.svelte';

/** @type {any} */
let result = $state({});

export default {
	get authenticated() {
		return result.authenticated ?? false;
	},
	get user() {
		return result.user ?? '';
	},
	get error() {
		return result.error;
	},
	checkAuthentication: async () => {
		result = (await call('engine.CheckAuthentication')).admin ?? {
			error: 'Error checking authentication'
		};
		if (result.ts) {
			Time.serverTimestamp = 1 * result.ts;
		}
		if (result.tz) {
			Time.serverTimezone = result.tz;
		}
	},
	authenticate: async (event) => {
		event.preventDefault?.();
		result = (await call('engine.Authenticate', new FormData(event.target))).admin ?? {
			error: 'Error authenticating'
		};
	},
	logout: async () => {
		result = (await call('engine.Authenticate', { authType: 'logout' })).admin ?? {
			error: 'Error logging out'
		};
	}
};
