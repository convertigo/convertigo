<script>
	import { page } from '$app/state';
	import { fade, fly, slide } from 'svelte/transition';
	import Ico from '$lib/utils/Ico.svelte';
	import { resolveRoute } from '$app/paths';

	/** @type {{parts: any}} */
	let { parts: _parts } = $props();
	let parts = $derived([
		..._parts,
		[{ title: 'Logout', icon: 'material-symbols-light:cancel-outline', page: '/(app)/logout' }]
	]);
	let activeIndex = $derived.by(() => {
		const i = parts[0].findIndex((part) => page.route.id == part.page || page.route.id == part.id);
		return i == -1 ? 0 : i;
	});

	let activeIndexLast = $state(0);
	$effect(() => {
		activeIndexLast = activeIndex;
	});
</script>

<nav class="bg-surface-200-800 border-r-[0.5px] border-color layout-y-none h-full">
	{#each parts as tiles, i}
		{#each tiles as { title, icon, url, page, params, loading }, j}
			{@const href = loading ? undefined : page ? resolveRoute(page, params) : url}
			<a
				{href}
				class="relative layout-x-p-low !gap hover:bg-surface-200-800 rounded min-w-36 {loading
					? 'blur-sm'
					: ''}"
				transition:slide={{ axis: 'y' }}
			>
				{#if i == 0 && j == activeIndex}
					<span
						in:fly={{ y: (activeIndexLast - activeIndex) * 50 }}
						out:fade
						class="absolute inset-0 preset-filled-primary-500 opacity-40 rounded-sm"
					></span>
				{/if}
				<Ico size="5" {icon} class="z-10" />
				<span class="text-[14px] z-10 font-{i == 0 && j == activeIndex ? 'semibold' : 'medium'}"
					>{title}</span
				>
			</a>
		{/each}

		{#if i < parts.length - 1}
			<div class="w-full p-5">
				<div class="border-[1px] border-surface-700-300"></div>
			</div>
		{/if}
	{/each}
</nav>
