<script>
	import { AppBar, LightSwitch } from '@skeletonlabs/skeleton';
	import { monitorData } from '$lib/admin/stores/monitorStore';
	import { page } from '$app/stores';
	import Ico from '$lib/utils/Ico.svelte';
	import { fly, slide } from 'svelte/transition';

	$: isBackend = $page.url.pathname.includes('backend');
	$: isFrontend = $page.url.pathname.includes('frontend');
</script>

<AppBar
	class="app-bar border-b-[0.5px] dark:border-surface-500 border-surface-200 py-2 px-10"
	background="dark:bg-surface-700 bg-surface-100"
	gridColumns="grid-cols-3"
	slotDefault="place-self-center"
	slotTrail="place-content-end"
	padding="p-0"
>
	<svelte:fragment slot="lead">
		<!-- <PagesRailToggle /> -->
		<!-- <img src="{assets}/logo.png" alt="logo convertigo" class="logo-desktop mr-4 ml-4" />
		<h1 class="app-title">Convertigo Admin Console</h1> -->
		{#if $monitorData.time > 0}
			<span class="monitor-time">{new Date($monitorData.time).toTimeString().split(' ')[0]}</span>
		{/if}
	</svelte:fragment>

	{#if isBackend || isFrontend}
		<div class="flex" transition:slide={{ axis: 'y' }}>
			<a
				href="../backend/"
				class:variant-filled-secondary={isBackend}
				class:variant-ghost-secondary={isFrontend}
				class="btn rounded-r-none"
				><span><Ico icon="ph:gear-six-thin" size="nav" /></span><span>Backend</span></a
			>
			<a
				href="../frontend/"
				class:variant-filled-secondary={isFrontend}
				class:variant-ghost-secondary={isBackend}
				class="btn rounded-l-none"
				><span>Frontend</span><span><Ico icon="ph:video-thin" size="nav" /></span></a
			>
		</div>
	{/if}
	<svelte:fragment slot="trail">
		<LightSwitch />
	</svelte:fragment>
</AppBar>

<style lang="postcss">
	.app-bar {
		@apply flex w-full justify-between dark:bg-secondary-800 bg-white;
	}
	.logo-mobile {
		@apply w-10 h-full md:hidden;
	}
	.logo-desktop {
		@apply w-10 h-full hidden md:block;
	}
	.app-title {
		@apply font-normal dark:text-surface-200 text-surface-800 hidden md:block;
	}
	.monitor-time {
		@apply m-2 p-1 border rounded font-mono text-xs hidden md:block;
	}
	.github-link {
		@apply flex items-center btn font-normal hidden md:inline-flex;
	}
	.trail-links {
		@apply pr-4 p-1 flex items-center gap-5;
	}
	.studio-link {
		@apply items-center p-1 pl-5 pr-5 hidden md:inline-flex rounded-token;
	}

	.icon-size {
		@apply w-7 h-7;
	}
	.light-switch {
		@apply mr-5;
	}
</style>
