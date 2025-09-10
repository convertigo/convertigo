import Status from '$lib/common/Status.svelte';
import { getUrl } from '$lib/utils/service';

const parts = $derived([
	[
		{
			title: 'Home',
			icon: 'mdi:home-outline',
			page: '/(app)/admin'
		},
		{
			title: 'Config',
			icon: 'mdi:cog-outline',
			id: '/(app)/admin/config/[category]',
			page: '/(app)/admin/config'
		},
		{
			title: 'Projects',
			icon: 'mdi:folder-outline',
			page: '/(app)/admin/projects'
		},
		{
			title: 'Symbols',
			icon: 'mdi:star-outline',
			page: '/(app)/admin/symbols'
		},
		{
			title: 'Connections',
			icon: 'mdi:brain',
			page: '/(app)/admin/connections'
		},
		{
			title: 'Logs',
			icon: 'mdi:magnify',
			id: '/(app)/admin/logs/[tab]',
			page: '/(app)/admin/logs'
		},
		{
			title: 'Full Sync',
			icon: 'mdi:cloud-sync-outline',
			page: '/(app)/admin/fullsync'
		},
		{
			title: 'Cache',
			icon: 'mdi:cached',
			page: '/(app)/admin/cache'
		},
		{
			title: 'Scheduler',
			icon: 'mdi:calendar-clock',
			page: '/(app)/admin/scheduler'
		},
		{
			title: 'Roles',
			icon: 'mdi:account-supervisor-circle-outline',
			page: '/(app)/admin/roles'
		},
		{
			title: 'Certificates',
			icon: 'mdi:bookmark-check-outline',
			page: '/(app)/admin/certificates'
		},
		{
			title: 'Keys',
			icon: 'mdi:key-outline',
			page: '/(app)/admin/keys',
			loading: Status.cloud == null
		}
	].filter(({ title }) => title != 'Keys' || Status.cloud != true),
	[
		{
			title: 'Dashboard',
			icon: 'mdi:view-dashboard-outline',
			page: '/(app)/dashboard'
		},
		{
			title: 'Swagger',
			icon: 'mdi:code-braces',
			url: getUrl(`/swagger/dist/index.html?url=${encodeURIComponent(getUrl('/openapi?YAML'))}`),
			external: true
		},
		{
			title: 'Test Platform',
			icon: 'mdi:view-dashboard-outline',
			url: getUrl('/testplatform.html'),
			external: true
		},
		{
			title: 'Old Admin',
			icon: 'mdi:cog-outline',
			url: getUrl('/admin_/'),
			external: true
		},
		{
			title: 'Convertigo',
			icon: 'mdi:cloud-outline',
			url: 'https://www.convertigo.com'
		},
		{
			title: 'Dev Network',
			icon: 'mdi:hub',
			url: 'https://convertigo.atlassian.net/wiki/spaces/CK/'
		},
		{
			title: 'Documentation',
			icon: 'mdi:file-question-outline',
			url: 'https://doc.convertigo.com'
		}
	]
]);

export default {
	get parts() {
		return parts;
	}
};
