<script>
	import { AppBar, LightSwitch } from '@skeletonlabs/skeleton';
	import Icon from '@iconify/svelte';
	import PagesRailToggle from '../components/PagesRailToggle.svelte';
	import { assets, base } from '$app/paths';
	import { monitorData } from '../stores/monitorStore';
	import Ico from '$lib/utils/Ico.svelte';
</script>

<AppBar
	background="dark:bg-surface-800 bg-white"
	class="flex w-full justify-between border-b-[0.5px] dark:border-surface-500 border-surface-50"
	padding="p-3"
>
	<svelte:fragment slot="lead">
		<PagesRailToggle />
		<img src="{assets}/logo.png" alt="logo convertigo" class="w-10 h-full mr-5 ml-3" />
		<h1 class="font-normal dark:text-surface-200 text-surface-800 hidden md:block">
			Convertigo Admin Console
		</h1>
		{#if $monitorData.time > 0}
			<span class="m-2 p-1 border-[1px] rounded font-mono text-xs"
				>{new Date($monitorData.time).toTimeString().split(' ')[0]}</span
			>
		{/if}
	</svelte:fragment>

	<svelte:fragment slot="trail">
		<a
			href="https://github.com/convertigo/convertigo"
			target="_blank"
			class="flex items-center btn font-normal"
		>
			<p class="mr-2">Star us on</p>
			<Ico icon="~icons/octicon/logo-github-16" />
		</a>

		<div class="pr-4 p-1 flex items-center space-x-5">
			<a type="button" class="btn p-1 pl-5 pr-5 btn variant-filled" href="{base}/studio/">
				<span><Icon icon="material-symbols-light:design-services-outline-sharp" /></span>
				<span class="text-[13px] font-light">Go to studio</span>
			</a>
			<LightSwitch />
			<Icon icon="ph:gear-six-thin" class="w-6 h-6" />
		</div>
	</svelte:fragment>
</AppBar>
