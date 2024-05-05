<script>
	import { AppBar, LightSwitch } from '@skeletonlabs/skeleton';
	import Icon from '@iconify/svelte';
	import PagesRailToggle from '../components/PagesRailToggle.svelte';
	import { assets, base } from '$app/paths';
	import { monitorData } from '../stores/monitorStore';
	import Ico from '$lib/utils/Ico.svelte';
</script>

<AppBar
	class="app-bar border-b-[0.5px] dark:border-surface-500 border-surface-200"
	background="dark:bg-surface-700 bg-surface-100"
	padding="p-0"
>
	<svelte:fragment slot="lead">
		<PagesRailToggle />
		<img src="{assets}/logo.png" alt="logo convertigo" class="logo-desktop mr-4 ml-4" />
		<h1 class="app-title">Convertigo Admin Console</h1>
		{#if $monitorData.time > 0}
			<span class="monitor-time">{new Date($monitorData.time).toTimeString().split(' ')[0]}</span>
		{/if}
	</svelte:fragment>

	<svelte:fragment slot="trail">
		<div class="trail-links">
			<a href="https://github.com/convertigo/convertigo" target="_blank" class="github-link">
				<p class="mr-3 font-extrabold">Star us on</p>
				<Icon icon="brandico:github" class="w-7 h-7" />
			</a>

			<LightSwitch class="light-switch" />

			<a
				type="button"
				class="studio-link blue-button bg-primary-400-500-token"
				href="{base}/studio/"
			>
				<span><Ico icon="iconamoon:cloud-fill" class="w-7 h-7 mr-2" /></span>
				<span class="text-[14px]">Go to studio</span>
			</a>

			<img src="{assets}/logo.png" alt="logo convertigo" class="logo-mobile" />
		</div>
	</svelte:fragment>
</AppBar>

<style lang="postcss">
	.app-bar {
		@apply flex w-full justify-between dark:bg-surface-800 bg-white;
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
