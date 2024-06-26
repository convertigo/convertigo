<script>
	import { AppBar, LightSwitch } from '@skeletonlabs/skeleton';
	import Icon from '@iconify/svelte';
	import { assets, base } from '$app/paths';
	import { monitorData } from '$lib/admin/stores/monitorStore';
	import Ico from '$lib/utils/Ico.svelte';
</script>

<AppBar
	class="app-bar border-b-[0.5px] dark:border-surface-500 border-surface-200 py-4"
	background="dark:bg-surface-700 bg-surface-100"
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

	<svelte:fragment slot="trail">
<LightSwitch/>
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
