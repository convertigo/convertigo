<script>
	import { Modal } from '@skeletonlabs/skeleton-svelte';
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

<Modal
	open={showDrawer}
	contentBase="shadow-xl w-fit h-screen"
	triggerBase="hidden"
	positionerJustify="justify-start"
	positionerAlign=""
	positionerPadding=""
	transitionsPositionerIn={{ x: -480, duration: 200 }}
	transitionsPositionerOut={{ x: -480, duration: 200 }}
	onInteractOutside={() => (showDrawer = false)}
>
	{#snippet content()}
		<PagesRailToggle bind:state={showDrawer} class="h-fit! w-fit! pl-5" />
		<PagesRail {parts} />
	{/snippet}
</Modal>

<div class="flex min-h-screen flex-col" class:blur-xs={!browser}>
	<Topbar bind:showLeft bind:showDrawer />

	<div class="layout-y-stretch grow gap-0! md:layout-x-stretch">
		{#if showLeft}
			<aside class="max-md:hidden" transition:slide={{ axis: 'x' }}>
				<PagesRail {parts} />
			</aside>
		{/if}
		{#key page.route.id}
			<main class="min-h-full w-full grow py px" in:fade>
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
