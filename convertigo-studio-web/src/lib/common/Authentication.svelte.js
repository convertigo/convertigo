import { call, checkArray } from '$lib/utils/service';
import Time from './Time.svelte';

/** @type {any} */
let result = $state({});

const parseRole = (role) =>
	typeof role == 'string' ? role : (role?.name ?? role?.['@_name'] ?? role?.['#text'] ?? '');

export default {
	get authenticated() {
		return result.authenticated ?? false;
	},
	get roles() {
		return checkArray(result?.roles?.role).map(parseRole).filter(Boolean);
	},
	hasRole(role) {
		return this.roles.includes(role);
	},
	get canAccessAdmin() {
		return this.hasRole('WEB_ADMIN');
	},
	get canAccessDashboard() {
		return this.canAccessAdmin || this.roles.some((role) => role.startsWith('TEST_PLATFORM'));
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
		result = (await call('engine.Authenticate', event.target ? new FormData(event.target) : event))
			.admin ?? {
			error: 'Error authenticating'
		};
	},
	logout: async () => {
		result = (await call('engine.Authenticate', { authType: 'logout' })).admin ?? {
			error: 'Error logging out'
		};
	}
};
