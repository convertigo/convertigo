<script>
	import { AppShell, Drawer } from '@skeletonlabs/skeleton';
	import TopbarAdmin from '$lib/admin/sidebars/Topbar.svelte';
	import TopbarDashBoard from '$lib/dashboard/components/Topbar.svelte';
	import PagesRail from '$lib/common/components/PagesRail.svelte';
	import partsAdmin from '$lib/admin/PagesRail.json';
	import partsDashboard from '$lib/dashboard/PagesRail.json';
	import PagesRailToggle from '$lib/admin/components/PagesRailToggle.svelte';
	import { fade, slide } from 'svelte/transition';
	import { page } from '$app/stores';
	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();

	let path = $derived(
		$page.route?.id?.startsWith('/(app)/dashboard') ? '/(app)/dashboard' : '/(app)/admin'
	);
	let parts = $derived(path == '/(app)/admin' ? partsAdmin : partsDashboard);
	let showLeft = $state(true);
</script>

<Drawer>
	<PagesRailToggle open={false} />
	<PagesRail {path} {parts} />
</Drawer>

<AppShell>
	<svelte:fragment slot="header">
		{#if path == '/(app)/admin'}
			<TopbarAdmin bind:showLeft />
		{:else}
			<TopbarDashBoard />
		{/if}
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft">
		{#if showLeft}
			<div class="hide-md h-full" transition:slide={{ axis: 'x' }}>
				<PagesRail {path} {parts} />
			</div>
		{/if}
	</svelte:fragment>
	{#key $page.url.pathname}
		<div class="px py h-full" in:fade>
			{@render children?.()}
		</div>
	{/key}
</AppShell>
