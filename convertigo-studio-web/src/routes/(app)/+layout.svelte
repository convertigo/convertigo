<script>
	import { AppShell, Drawer } from '@skeletonlabs/skeleton';
	import TopbarAdmin from '$lib/admin/sidebars/Topbar.svelte';
	import TopbarDashBoard from '$lib/dashboard/components/Topbar.svelte';
	import PagesRail from '$lib/common/components/PagesRail.svelte';
	import partsAdmin from '$lib/admin/PagesRail.json';
	import partsDashboard from '$lib/dashboard/PagesRail.json';
	import PagesRailToggle from '$lib/admin/components/PagesRailToggle.svelte';
	import { fade } from 'svelte/transition';
	import { page } from '$app/stores';

	$: path = $page.route?.id?.startsWith('/(app)/dashboard') ? '/(app)/dashboard' : '/(app)/admin';
	$: parts = path == '/(app)/admin' ? partsAdmin : partsDashboard;
</script>

<Drawer>
	<PagesRailToggle open={false} />
	<PagesRail {path} {parts} />
</Drawer>

<AppShell>
	<svelte:fragment slot="header">
		{#if path == '/(app)/admin'}
			<TopbarAdmin />
		{:else}
			<TopbarDashBoard />
		{/if}
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft">
		<div class="hidden md:block bg-surface-800 h-full">
			<PagesRail {path} {parts} />
		</div>
	</svelte:fragment>
	{#key $page.url.pathname}
		<div class="p-5 flex flex-col h-full" in:fade>
			<slot />
		</div>
	{/key}
</AppShell>
