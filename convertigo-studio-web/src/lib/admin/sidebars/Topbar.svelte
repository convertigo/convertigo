<script>
	import { AppBar, LightSwitch } from '@skeletonlabs/skeleton';
	import PagesRailToggle from '../components/PagesRailToggle.svelte';
	import { browser } from '$app/environment';
	import Time from '$lib/common/Time.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	let { showLeft = $bindable() } = $props();
</script>

<AppBar
	background="dark:bg-surface-700 bg-surface-100"
	padding="p-0"
	gridColumns="grid-cols-3"
	slotDefault="place-self-center"
	slotTrail="place-content-end"
>
	<svelte:fragment slot="lead">
		<div class="layout-x pl-1">
			<PagesRailToggle />
			<button class="hide-md" onclick={() => (showLeft = !showLeft)}
				><Ico icon="iconamoon:menu-burger-horizontal-thin" size={8} /></button
			>
			{#if Time.isSameTime}
				<span class="monitor-time">{Time.browserTime}</span>
			{:else}
				<span class="monitor-time hide-md">you {Time.browserTime}</span>
				<span class="monitor-time">server {Time.serverTime} {Time.serverTimezone}</span>
			{/if}
		</div>
	</svelte:fragment>

	<div class="layout-x">
		<Ico icon="logo.png" alt="logo convertigo" size={10} />
		<h1 class="hide-md">Convertigo Admin Console</h1>
	</div>

	<svelte:fragment slot="trail">
		<div class="layout-x-p">
			<a
				href="https://github.com/convertigo/convertigo"
				target="_blank"
				class="layout-x-low hide-md"
			>
				<p class="font-extrabold">Star us on</p>
				<Ico icon="mdi:github" size={8} />
			</a>
			{#if browser}
				<LightSwitch />
			{/if}
		</div>
	</svelte:fragment>
</AppBar>

<style lang="postcss">
	.monitor-time {
		@apply p-1 border rounded font-mono text-xs;
	}
</style>
