<script>
	import { AppRail, getDrawerStore } from '@skeletonlabs/skeleton';
	import parts from './PagesRail.json';
	import { page } from '$app/stores';
	import NavIco from '$lib/utils/NavIco.svelte';
	import { fade, fly } from 'svelte/transition';

	let isRoot = false;
	let activeIndex = 0;
	let activeIndexLast = 0;
	$: {
		isRoot = $page.route.id == '/admin';
		activeIndexLast = activeIndex;
		activeIndex = parts[0].findIndex((part) =>
			part.url == '' ? isRoot : $page.url.pathname.endsWith(`${part.url}/`)
		);
	}

	const drawerStore = getDrawerStore();
</script>

<AppRail width="w-auto" class="border-r-[0.5px] border-surface-200-700-token px-1">
	{#each parts as tiles, i}
		{#each tiles as tile, j}
			{@const url = tile.url.length ? `${tile.url}/` : ''}
			<a href={`${isRoot ? '' : '../'}${url}`} class="nav-links" on:click={drawerStore.close}>
				{#if i == 0 && j == activeIndex}
					<span
						in:fly={{ y: (activeIndexLast - activeIndex) * 50 }}
						out:fade
						class="absolute inset-0 variant-filled-primary opacity-40 rounded-token"
					></span>
				{/if}
				<NavIco icon={tile.icon} style="z-index: 10;" />
				<span
					class={`ml-3 text-[14px] z-10 font-${i == 0 && j == activeIndex ? 'medium' : 'extralight'}`}
					>{tile.title}</span
				>
			</a>
		{/each}

		{#if i < parts.length - 1}
			<div class="w-full p-5">
				<separator class="border-[1px] flex border-surface-700-200-token" />
			</div>
		{/if}
	{/each}
</AppRail>

<style lang="postcss">
	.nav-links {
		@apply relative flex py-[5px] mt-1 px-2 items-center dark:hover:bg-surface-700 hover:bg-surface-50 rounded-token;
	}
</style>
