<script>
	import { Modal } from '@skeletonlabs/skeleton-svelte';
	import Topbar from '$lib/admin/sidebars/Topbar.svelte';
	import PagesRail from '$lib/common/components/PagesRail.svelte';
	import partsAdmin from '$lib/admin/PagesRail.json';
	import partsDashboard from '$lib/dashboard/PagesRail.svelte.js';
	import PagesRailToggle from '$lib/admin/components/PagesRailToggle.svelte';
	import { fade, slide } from 'svelte/transition';
	import { page } from '$app/stores';
	import RightPart from './admin/RightPart.svelte';
	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();

	let path = $derived(
		$page.route?.id?.startsWith('/(app)/dashboard') ? '/(app)/dashboard' : '/(app)/admin'
	);
	let parts = $derived(path == '/(app)/admin' ? partsAdmin : partsDashboard.parts);
	let showLeft = $state(true);
	let showDrawer = $state(false);

	let doFade = $derived($page.route.id?.replace(/\/\[project\]$/, ''));
	$inspect('doFade', doFade);
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
		<PagesRail {path} {parts} />
	{/snippet}
</Modal>

<div class="flex flex-col min-h-screen">
	<Topbar bind:showLeft bind:showDrawer />

	<div class="layout-y md:layout-x !gap-0 !items-stretch grow">
		{#if showLeft}
			<aside class="hide-md" transition:slide={{ axis: 'x' }}>
				<PagesRail {path} {parts} />
			</aside>
		{/if}
		{#key doFade}
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
