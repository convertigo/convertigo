import { abortPendingCalls, call, checkArray } from '$lib/utils/service';
import Time from './Time.svelte';

/** @type {any} */
let result = $state({});
let state = $state({
	revision: 0,
	loggingOut: false
});
let requestSequence = 0;
let lastFingerprint = '';

const toArray = (value) => {
	if (value == null) {
		return [];
	}
	return Array.isArray(value) ? value : [value];
};

const pageRules = [
	{
		page: '/(app)/admin',
		viewRoles: 'HOME_VIEW',
		configRoles: 'HOME_CONFIG',
		extraRoles: 'MONITOR_AGENT'
	},
	{
		page: '/(app)/admin/logs',
		id: '/(app)/admin/logs/[tab]',
		viewRoles: 'LOGS_VIEW',
		configRoles: 'LOGS_CONFIG'
	},
	{
		page: '/(app)/admin/cache',
		viewRoles: 'CACHE_VIEW',
		configRoles: 'CACHE_CONFIG'
	},
	{
		page: '/(app)/admin/fullsync',
		viewRoles: 'FULLSYNC_VIEW',
		configRoles: 'FULLSYNC_CONFIG'
	},
	{
		page: '/(app)/admin/connections',
		viewRoles: 'CONNECTIONS_VIEW',
		configRoles: 'CONNECTIONS_CONFIG'
	},
	{
		page: '/(app)/admin/scheduler',
		viewRoles: 'SCHEDULER_VIEW',
		configRoles: 'SCHEDULER_CONFIG'
	},
	{
		page: '/(app)/admin/symbols',
		viewRoles: 'SYMBOLS_VIEW',
		configRoles: 'SYMBOLS_CONFIG'
	},
	{
		page: '/(app)/admin/certificates',
		viewRoles: 'CERTIFICATE_VIEW',
		configRoles: 'CERTIFICATE_CONFIG'
	},
	{
		page: '/(app)/admin/keys',
		viewRoles: 'KEYS_VIEW',
		configRoles: 'KEYS_CONFIG'
	},
	{
		page: '/(app)/admin/projects',
		viewRoles: 'PROJECTS_VIEW',
		configRoles: 'PROJECTS_CONFIG'
	},
	{
		page: '/(app)/admin/projects/[project]',
		viewRoles: 'PROJECT_DBO_VIEW',
		configRoles: 'PROJECT_DBO_CONFIG',
		navigable: false
	},
	{
		page: '/(app)/admin/config',
		id: '/(app)/admin/config/[category]',
		viewRoles: ['CONFIG_VIEW', 'LOGS_VIEW', 'CACHE_VIEW', 'CERTIFICATE_VIEW'],
		configRoles: ['CONFIG_CONFIG', 'LOGS_CONFIG', 'CACHE_CONFIG', 'CERTIFICATE_CONFIG']
	},
	{
		page: '/(app)/admin/roles'
	},
	{
		page: '/(app)/admin/assistant'
	}
].map((rule) => ({
	...rule,
	viewRoles: toArray(rule.viewRoles),
	configRoles: toArray(rule.configRoles),
	extraRoles: toArray(rule.extraRoles),
	navigable: rule.navigable ?? true
}));

const dashboardRule = {
	viewRoles: ['TEST_PLATFORM', 'PROJECTS_VIEW'],
	configRoles: ['PROJECTS_CONFIG'],
	extraRoles: []
};

const parseRole = (role) =>
	typeof role == 'string' ? role : (role?.name ?? role?.['@_name'] ?? role?.['#text'] ?? '');

const roleNames = (source = result) =>
	checkArray(source?.roles?.role).map(parseRole).filter(Boolean);

const hasAnyRole = (roles = [], expectedRoles = []) =>
	expectedRoles.some((role) => roles.includes(role));

const canAccessRule = (roles = [], rule) =>
	roles.includes('WEB_ADMIN') ||
	hasAnyRole(roles, [...rule.viewRoles, ...rule.configRoles, ...rule.extraRoles]);

function matchAdminPage(routeId = '') {
	for (const page of pageRules) {
		if (routeId == page.id || routeId == page.page) {
			return page;
		}
	}
	for (const page of [...pageRules].sort((a, b) => b.page.length - a.page.length)) {
		if (routeId.startsWith(`${page.page}/`)) {
			return page;
		}
	}
	return null;
}

function setResult(next) {
	result = next ?? {};
	const fingerprint = JSON.stringify({
		authenticated: result.authenticated ?? false,
		user: result.user ?? '',
		roles: roleNames(result).slice().sort()
	});
	if (fingerprint !== lastFingerprint) {
		lastFingerprint = fingerprint;
		state.revision += 1;
	}
}

function applyResult(sequence, next, { updateTime = false } = {}) {
	if (sequence != requestSequence) {
		return;
	}
	setResult(next);
	if (updateTime && result.ts) {
		Time.serverTimestamp = 1 * result.ts;
	}
	if (updateTime && result.tz) {
		Time.serverTimezone = result.tz;
	}
}

export default {
	get authenticated() {
		return result.authenticated ?? false;
	},
	get roles() {
		return roleNames(result);
	},
	hasRole(role) {
		return this.roles.includes(role);
	},
	get canAccessAdmin() {
		return pageRules.some((page) => canAccessRule(this.roles, page));
	},
	get canAccessDashboard() {
		return canAccessRule(this.roles, dashboardRule);
	},
	canAccessAdminRoute(routeId) {
		const page = matchAdminPage(routeId);
		return page == null ? false : canAccessRule(this.roles, page);
	},
	get defaultAdminPage() {
		return (
			pageRules.find((page) => page.navigable && canAccessRule(this.roles, page))?.page ?? null
		);
	},
	get user() {
		return result.user ?? '';
	},
	get error() {
		return result.error;
	},
	get revision() {
		return state.revision;
	},
	get loggingOut() {
		return state.loggingOut;
	},
	checkAuthentication: async () => {
		const sequence = ++requestSequence;
		const next = (await call('engine.CheckAuthentication')).admin ?? {
			error: 'Error checking authentication'
		};
		applyResult(sequence, next, { updateTime: true });
	},
	authenticate: async (event) => {
		event.preventDefault?.();
		const sequence = ++requestSequence;
		applyResult(
			sequence,
			(await call('engine.Authenticate', event.target ? new FormData(event.target) : event))
				.admin ?? {
				error: 'Error authenticating'
			}
		);
	},
	logout: async () => {
		const sequence = ++requestSequence;
		state.loggingOut = true;
		abortPendingCalls();
		setResult({ authenticated: false });
		try {
			applyResult(
				sequence,
				(await call('engine.Authenticate', { authType: 'logout' })).admin ?? {
					error: 'Error logging out'
				}
			);
		} finally {
			state.loggingOut = false;
		}
	}
};
