<script>
	import PagesRail from '$lib/dashboard/components/PagesRail.svelte';
	import Topbar from '$lib/dashboard/components/Topbar.svelte';
	import { AppShell } from '@skeletonlabs/skeleton';
	import { fade } from 'svelte/transition';
</script>

<AppShell>
	<svelte:fragment slot="header">
		<Topbar />
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft">
		<div class="hidden md:block bg-surface-800 h-full">
			<PagesRail />
		</div>
	</svelte:fragment>

	<div class="p-10 gap-5 flex flex-col h-full" in:fade>
		<slot />
	</div>
</AppShell>
