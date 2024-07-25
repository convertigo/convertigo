<script>
	import { AppRail, getDrawerStore } from '@skeletonlabs/skeleton';
	import { page } from '$app/stores';
	import { fade, fly } from 'svelte/transition';
	import Ico from '$lib/utils/Ico.svelte';

	export let path;
	export let parts;
	let isRoot = false;
	let activeIndex = 0;
	let activeIndexLast = 0;
	let relativePath = '';
	$: {
		isRoot = $page.route.id == path;
		activeIndexLast = activeIndex;
		activeIndex = parts[0].findIndex((part) =>
			part.url == '' ? isRoot : $page.url.pathname.endsWith(`${part.url}/`)
		);
		// @ts-ignore
		relativePath = new Array($page?.route?.id?.substring(path.length).split('/').length - 1)
			.fill('../')
			.join('');
	}

	const drawerStore = getDrawerStore();
</script>

<AppRail width="w-auto" class="border-r-[0.5px] border-surface-200-700-token px-2">
	<div class="h-5" />
	{#each parts as tiles, i}
		{#each tiles as tile, j}
			{@const url = tile.url.length ? `${tile.url}/` : ''}
			<a
				href="{url.startsWith('http') ? '' : relativePath}{url}"
				class="nav-links"
				on:click={drawerStore.close}
			>
				{#if i == 0 && j == activeIndex}
					<span
						in:fly={{ y: (activeIndexLast - activeIndex) * 50 }}
						out:fade
						class="absolute inset-0 variant-filled-primary opacity-40 rounded-token"
					></span>
				{/if}
				<Ico size="nav" icon={tile.icon} class="z-10" />
				<span class="ml-3 text-[13px] z-10 font-{i == 0 && j == activeIndex ? 'medium' : 'light'}"
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
