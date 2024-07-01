<script>
	import { AppRail, getDrawerStore } from '@skeletonlabs/skeleton';
	import parts from './PagesRail.json';
	import { page } from '$app/stores';
	import { fade, fly } from 'svelte/transition';
	import Ico from '$lib/utils/Ico.svelte';

	let isRoot = false;
	let activeIndex = 0;
	let activeIndexLast = 0;
	$: {
		isRoot = $page.route.id == '/dashboard';
		activeIndexLast = activeIndex;
		activeIndex = parts[0].findIndex((part) =>
			part.url == '' ? isRoot : $page.url.pathname.endsWith(`${part.url}/`)
		);
	}

	const drawerStore = getDrawerStore();
</script>

<AppRail width="w-auto" class="border-r-[0.5px] border-surface-200-700-token px-4">
	<div class="h-8" />
	{#each parts as tiles, i}
		{#each tiles as tile, j}
			{@const url = tile.url.length ? `${tile.url}/` : ''}
			<a href={`${isRoot ? '' : '/'}${url}`} class="nav-links" on:click={drawerStore.close}>
				{#if i == 0 && j == activeIndex}
					<span
						in:fly={{ y: (activeIndexLast - activeIndex) * 50 }}
						out:fade
						class="absolute inset-0 variant-filled-primary opacity-40 rounded-token"
					></span>
				{/if}
				<Ico size="dashboard" icon={tile.icon} style="z-index: 10;" class="dark:text-surface-300" />
				<span
					class={`ml-3 text-[13px] dark:text-surface-200 z-10 font-${i == 0 && j == activeIndex ? 'medium' : 'light'}`}
					>{tile.title}</span
				>
			</a>
		{/each}
	{/each}
</AppRail>

<style lang="postcss">
	.nav-links {
		@apply relative flex gap-2 py-[8px] mt-1 px-2 items-center mb-3 dark:hover:bg-primary-900 hover:bg-primary-50 rounded-token;
	}
</style>
