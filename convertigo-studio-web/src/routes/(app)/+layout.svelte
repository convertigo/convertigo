<script>
	import { Dialog, Portal } from '@skeletonlabs/skeleton-svelte';
	import { browser } from '$app/environment';
	import { page } from '$app/state';
	import PagesRailToggle from '$lib/admin/components/PagesRailToggle.svelte';
	import Topbar from '$lib/admin/components/Topbar.svelte';
	import partsAdmin from '$lib/admin/PagesRail.svelte';
	import PagesRail from '$lib/common/components/PagesRail.svelte';
	import partsDashboard from '$lib/dashboard/PagesRail.svelte';
	import { fade, slide } from 'svelte/transition';
	import RightPart from './admin/RightPart.svelte';

	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();

	let parts = $derived(
		page.route?.id?.startsWith('/(app)/admin') ? partsAdmin.parts : partsDashboard.parts
	);
	let showLeft = $state(true);
	let showDrawer = $state(false);
</script>

<Dialog open={showDrawer} onOpenChange={(e) => (showDrawer = e.open)}>
	<Dialog.Trigger class="hidden" />
	<Portal>
		<Dialog.Backdrop class="fixed inset-0 bg-surface-50-950/30 backdrop-blur-sm" />
		<Dialog.Positioner class="fixed inset-0 flex justify-start">
			<Dialog.Content class="h-full max-h-screen w-80 max-w-full -translate-x-full overflow-y-auto bg-surface-50-950 p-low transition-transform duration-200 data-[state=open]:translate-x-0 dark:bg-surface-900">
				<PagesRailToggle bind:state={showDrawer} class="h-fit! w-fit! pl-5" />
				<PagesRail {parts} />
			</Dialog.Content>
		</Dialog.Positioner>
	</Portal>
</Dialog>

<div class="flex min-h-screen flex-col" class:blur-xs={!browser}>
	<Topbar bind:showLeft bind:showDrawer />

	<div class="layout-y-stretch grow gap-0! md:layout-x-stretch">
		{#if showLeft}
			<aside class="max-md:hidden" transition:slide={{ axis: 'x' }}>
				<PagesRail {parts} />
			</aside>
		{/if}
		{#key page.route.id}
			<main class="min-h-full w-full min-w-0 grow py px" in:fade>
				{@render children?.()}
			</main>
		{/key}
		{#if RightPart.snippet}
			<aside
				class="max-md:order-first"
				transition:slide={{ axis: window?.matchMedia('(min-width: 768px)').matches ? 'x' : 'y' }}
			>
				{@render RightPart.snippet()}
			</aside>
		{/if}
	</div>
</div>
