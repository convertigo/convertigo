import { getAdminPageDocHref } from '$lib/admin/AdminDocumentation.svelte';
import Authentication from '$lib/common/Authentication.svelte';
import Status from '$lib/common/Status.svelte';
import { getUrl } from '$lib/utils/service';

const cloud = $derived(Authentication.canAccessAdmin ? Status.cloud : null);

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
			icon: 'mdi:lan-connect',
			page: '/(app)/admin/connections'
		},
		{
			title: 'Logs',
			icon: 'mdi:file-document-box-outline',
			id: '/(app)/admin/logs/[tab]',
			page: '/(app)/admin/logs'
		},
		{
			title: 'Full Sync',
			icon: 'mdi:database-sync-outline',
			page: '/(app)/admin/fullsync'
		},
		{
			title: 'Cache',
			icon: 'mdi:database-clock-outline',
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
			loading: Authentication.canAccessAdmin && cloud == null
		},
		{
			title: 'Assistant',
			icon: 'mdi:robot-outline',
			page: '/(app)/admin/assistant'
		}
	].filter(({ title }) => title != 'Keys' || cloud != true),
	[
		{
			title: 'Dashboard',
			icon: 'mdi:view-dashboard-outline',
			page: '/(app)/dashboard'
		},
		{
			title: 'Swagger',
			icon: 'mdi:code-braces',
			url: getUrl(`swagger/dist/index.html?url=${encodeURIComponent(getUrl('openapi?YAML'))}`),
			external: true
		},
		{
			title: 'Test Platform',
			icon: 'mdi:home-alert-outline',
			url: getUrl('testplatform.html'),
			external: true
		},
		{
			title: 'Old Admin',
			icon: 'mdi:home-alert-outline',
			url: getUrl('admin_/'),
			external: true
		},
		{
			title: 'Convertigo',
			icon: 'convertigo:logo',
			url: 'https://www.convertigo.com',
			external: true
		},
		{
			title: 'Knowledge',
			icon: 'mdi:hub',
			url: 'https://convertigo.atlassian.net/wiki/spaces/CK/',
			external: true
		},
		{
			title: 'Documentation',
			icon: 'mdi:file-question-outline',
			url: getAdminPageDocHref('/admin'),
			external: true
		}
	]
]);

export default {
	get parts() {
		return parts;
	}
};
