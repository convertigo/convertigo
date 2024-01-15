// tilesStore.js
import { writable } from 'svelte/store';

export const tilesStore = writable([
	{ title: 'Home', icon: 'iconoir:home', url: '/admin-console', value: 1 },
	{ title: 'Config', icon: 'ph:gear-thin', url: '/admin-console/config', value: 2 },
	{
		title: 'Connections',
		icon: 'ph:plugs-connected-thin',
		url: '/admin-console/connections',
		value: 3
	},
	{ title: 'Projects', icon: 'ph:folder-thin', url: '/admin-console/projects', value: 4 },
	{
		title: 'Certificate',
		icon: 'ph:certificate-thin',
		url: '/admin-console/certificate',
		value: 5
	},
	{ title: 'Logs', icon: 'lets-icons:search-light', url: '/admin-console/logs', value: 6 },
	{ title: 'Trace Player', icon: 'ph:video-thin', url: '/admin-console/trace-player', value: 7 },
	{
		title: 'Cache',
		icon: 'material-symbols-light:update',
		url: '/admin-console/cache',
		value: 8
	},
	{
		title: 'Scheduler',
		icon: 'material-symbols-light:schedule-outline',
		url: '/admin-console/scheduler',
		value: 9
	},
	{
		title: 'Keys',
		icon: 'material-symbols-light:key-outline',
		url: '/admin-console/keys',
		value: 10
	},
	{ title: 'Roles', icon: 'lets-icons:user-alt-light', url: '/admin-console/roles', value: 11 },
	{ title: 'Symbols', icon: 'et:tools-2', url: '/admin-console/symbols', value: 12 },
	{
		title: 'Store',
		icon: 'material-symbols-light:cloud-outline',
		url: '/admin-console/store',
		value: 13
	},
	{
		title: 'Full Sync',
		icon: 'material-symbols-light:sync-outline',
		url: '/admin-console/full-sync',
		value: 14
	}
]);

export const tilesStoreMiscleanous = writable([
	{
		title: 'Test Platform',
		icon: 'file-icons:test-ruby',
		url: '/admin-console/test-platform',
		value: 15
	},
	{ title: 'Swagger', icon: 'codicon:json', url: '/admin-console/swagger', value: 16 },
	{
		title: 'Convertigo',
		icon: 'material-symbols-light:cloud-outline',
		url: '/admin-console/convertigo',
		value: 17
	},
	{
		title: 'Dev Network',
		icon: 'material-symbols-light:sync-outline',
		url: '/admin-console/dev-network',
		value: 18
	},
	{
		title: 'Documentation',
		icon: 'material-symbols-light:cloud-outline',
		url: '/admin-console/documentation',
		value: 19
	}
]);
