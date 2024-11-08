<script>
	import { Modal } from '@skeletonlabs/skeleton-svelte';
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
		<PagesRail {path} {parts} />
	{/snippet}
</Modal>

{#if path == '/(app)/admin'}
	<TopbarAdmin bind:showLeft bind:showDrawer />
{:else}
	<TopbarDashBoard />
{/if}

<div class="layout-x-low !items-stretch">
	{#if showLeft}
		<aside class="hide-md" transition:slide={{ axis: 'x' }}>
			<PagesRail {path} {parts} />
		</aside>
	{/if}
	{#key $page.url.pathname}
		<main class="px py h-full w-full" in:fade>
			{@render children?.()}
		</main>
	{/key}
</div>
