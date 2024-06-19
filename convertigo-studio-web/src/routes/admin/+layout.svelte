<script>
	import { AppShell, Drawer } from '@skeletonlabs/skeleton';
	import Topbar from '$lib/admin/sidebars/Topbar.svelte';
	import { Toast } from '@skeletonlabs/skeleton';
	import PagesRail from '$lib/admin/components/PagesRail.svelte';
	import PagesRailToggle from '$lib/admin/components/PagesRailToggle.svelte';
	import { blur, fly, fade } from 'svelte/transition';
	import { onMount } from 'svelte';
	import { page } from '$app/stores';

	let mounted = false;
	onMount(() => {
		mounted = true;
	});
</script>

<Drawer>
	<PagesRailToggle open={false} />
	<PagesRail />
</Drawer>
<Toast />

<AppShell>
	<svelte:fragment slot="header">
		<Topbar />
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft">
		<div class="hidden md:block bg-surface-800 h-full">
			<PagesRail />
		</div>
	</svelte:fragment>
	<svelte:fragment slot="sidebarRight"></svelte:fragment>

	{#key $page.url.pathname}
		<div class="p-5 flex flex-col h-full" in:fade>
			<slot />
		</div>
	{/key}
</AppShell>
