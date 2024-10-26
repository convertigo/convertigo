<script>
	import { AppRail, getDrawerStore } from '@skeletonlabs/skeleton';
	import { page } from '$app/stores';
	import { fade, fly } from 'svelte/transition';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{path: any, parts: any}} */
	let { path, parts } = $props();
	let isRoot = $derived($page.route.id == path);
	let relativePath = $derived(
		new Array(($page?.route?.id?.substring(path.length).split('/').length ?? 1) - 1)
			.fill('../')
			.join('')
	);
	let activeIndex = $derived(
		parts[0].findIndex((part) =>
			part.url == '' ? isRoot : $page.url.pathname.endsWith(`${part.url}/`)
		)
	);
	let activeIndexLast = $state(0);
	$effect(() => {
		activeIndexLast = activeIndex;
	});

	const drawerStore = getDrawerStore();
</script>

<AppRail width="w-auto" class="border-r-[0.5px] border-color px-low pt-5">
	{#each parts as tiles, i}
		{#each tiles as tile, j}
			{@const url = tile.url.length ? `${tile.url}/` : ''}
			<a
				href="{url.startsWith('http') ? '' : relativePath}{url}"
				class="relative layout-x-p-low !gap py-2 hover:bg-surface-200-700-token rounded-token"
				onclick={drawerStore.close}
			>
				{#if i == 0 && j == activeIndex}
					<span
						in:fly={{ y: (activeIndexLast - activeIndex) * 50 }}
						out:fade
						class="absolute inset-0 variant-filled-primary opacity-40 rounded-token"
					></span>
				{/if}
				<Ico size="nav" icon={tile.icon} class="z-10" />
				<span class="text-[13px] z-10 font-{i == 0 && j == activeIndex ? 'medium' : 'light'}"
					>{tile.title}</span
				>
			</a>
		{/each}

		{#if i < parts.length - 1}
			<div class="w-full p-5">
				<div class="border-[1px] border-surface-700-200-token"></div>
			</div>
		{/if}
	{/each}
</AppRail>
