import Status from '$lib/common/Status.svelte';
import { getUrl } from '$lib/utils/service';

const parts = $derived([
	[
		{
			title: 'Home',
			icon: 'material-symbols:home-outline-rounded',
			page: '/(app)/admin'
		},
		{
			title: 'Config',
			icon: 'material-symbols:settings-outline-rounded',
			id: '/(app)/admin/config/[category]',
			page: '/(app)/admin/config'
		},
		{
			title: 'Projects',
			icon: 'material-symbols:folder-outline',
			page: '/(app)/admin/projects'
		},
		{
			title: 'Symbols',
			icon: 'material-symbols:hotel-class-outline',
			page: '/(app)/admin/symbols'
		},
		{
			title: 'Connections',
			icon: 'material-symbols:online-prediction-rounded',
			page: '/(app)/admin/connections'
		},
		{
			title: 'Logs',
			icon: 'material-symbols:search-rounded',
			id: '/(app)/admin/logs/[tab]',
			page: '/(app)/admin/logs'
		},
		{
			title: 'Full Sync',
			icon: 'material-symbols:sync-arrow-up-rounded',
			page: '/(app)/admin/fullsync'
		},
		{
			title: 'Cache',
			icon: 'material-symbols:cached-rounded',
			page: '/(app)/admin/cache'
		},
		{
			title: 'Scheduler',
			icon: 'material-symbols:schedule-outline-rounded',
			page: '/(app)/admin/scheduler'
		},
		{
			title: 'Roles',
			icon: 'material-symbols:supervised-user-circle-outline',
			page: '/(app)/admin/roles'
		},
		{
			title: 'Certificates',
			icon: 'material-symbols:bookmark-added-outline-sharp',
			page: '/(app)/admin/certificates'
		},
		{
			title: 'Keys',
			icon: 'material-symbols:key-outline-rounded',
			page: '/(app)/admin/keys',
			loading: Status.cloud == null
		}
	].filter(({ title }) => title != 'Keys' || Status.cloud != true),
	[
		{
			title: 'Dashboard',
			icon: 'material-symbols:dashboard-outline-rounded',
			page: '/(app)/dashboard'
		},
		{
			title: 'Swagger',
			icon: 'material-symbols:data-object',
			url: getUrl(`swagger/dist/index.html?url=${encodeURIComponent(getUrl('/openapi?YAML'))}`),
			external: true
		},
		{
			title: 'Test Platform',
			icon: 'material-symbols:dashboard-outline-rounded',
			url: getUrl('testplatform.html'),
			external: true
		},
		{
			title: 'Old Admin',
			icon: 'material-symbols:settings-outline-rounded',
			url: getUrl('admin_/'),
			external: true
		},
		{
			title: 'Convertigo',
			icon: 'material-symbols:cloud-outline',
			url: 'https://www.convertigo.com'
		},
		{
			title: 'Dev Network',
			icon: 'material-symbols:device-hub',
			url: 'https://convertigo.atlassian.net/wiki/spaces/CK/'
		},
		{
			title: 'Documentation',
			icon: 'material-symbols:unknown-document-outline',
			url: 'https://doc.convertigo.com'
		}
	]
]);

export default {
	get parts() {
		return parts;
	}
};
