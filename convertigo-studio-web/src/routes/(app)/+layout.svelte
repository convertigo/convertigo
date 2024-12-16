<script>
	import { Modal } from '@skeletonlabs/skeleton-svelte';
	import Topbar from '$lib/admin/components/Topbar.svelte';
	import PagesRail from '$lib/common/components/PagesRail.svelte';
	import partsAdmin from '$lib/admin/PagesRail.json';
	import partsDashboard from '$lib/dashboard/PagesRail.svelte.js';
	import PagesRailToggle from '$lib/admin/components/PagesRailToggle.svelte';
	import { fade, slide } from 'svelte/transition';
	import { page } from '$app/state';
	import RightPart from './admin/RightPart.svelte';
	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();
	let parts = $derived(
		page.route?.id?.startsWith('/(app)/admin') ? partsAdmin : partsDashboard.parts
	);
	let showLeft = $state(true);
	let showDrawer = $state(false);
</script>

<Modal
	bind:open={showDrawer}
	contentBase="shadow-xl w-fit h-screen"
	triggerBase="hidden"
	positionerJustify="justify-start"
	positionerAlign=""
	positionerPadding=""
	transitionsPositionerIn={{ x: -480, duration: 200 }}
	transitionsPositionerOut={{ x: -480, duration: 200 }}
>
	{#snippet content()}
		<PagesRailToggle bind:state={showDrawer} />
		<PagesRail {parts} />
	{/snippet}
</Modal>

<div class="flex flex-col min-h-screen">
	<Topbar bind:showLeft bind:showDrawer />

	<div class="layout-y-stretch md:layout-x-stretch !gap-0 grow">
		{#if showLeft}
			<aside class="hide-md" transition:slide={{ axis: 'x' }}>
				<PagesRail {parts} />
			</aside>
		{/if}
		{#key page.route.id}
			<main class="px py w-full min-h-full grow" in:fade>
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
