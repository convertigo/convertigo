<script>
	import PagesRailToggle from './PagesRailToggle.svelte';
	import Time from '$lib/common/Time.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';
	import { page } from '$app/state';

	let { showLeft = $bindable(), showDrawer = $bindable() } = $props();
</script>

<header
	class="bg-surface-200-800 sticky top-0 z-20 flex justify-between border-b-[0.5px] border-color"
>
	<section class="layout-x pl-5">
		<PagesRailToggle class="show-md" bind:state={showDrawer} />
		<PagesRailToggle class="hide-md" bind:state={showLeft} />
		{#if Time.isSameTime}
			<span class="monitor-time">{Time.browserTime}</span>
		{:else}
			<span class="monitor-time hide-md">you {Time.browserTime}</span>
			<span class="monitor-time">server {Time.serverTime} {Time.serverTimezone}</span>
		{/if}
	</section>

	<section class="layout-x">
		<Ico icon="logo.png" alt="logo convertigo" size={7} />
		<h1 class="hide-md font-bold">
			Convertigo {page.route.id?.includes('dashboard') ? 'Dashboard' : 'Admin Console'}
		</h1>
	</section>

	<section class="layout-x-p py-low!">
		<a href="https://github.com/convertigo/convertigo" target="_blank" class="layout-x-low hide-md">
			<p class="font-extrabold">Star us on</p>
			<Ico icon="mdi:github" size={8} />
		</a>
		<LightSwitch />
	</section>
</header>

<!-- <style lang="postcss">
	.monitor-time {
		@apply p-1 border rounded font-mono text-xs text-nowrap;
	}
</style> -->
