<script>
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import SelectionHighlight from '$lib/common/components/SelectionHighlight.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { slide } from 'svelte/transition';

	/** @type {{parts: any}} */
	let { parts: _parts } = $props();
	let parts = $derived([
		..._parts,
		[{ title: 'Logout', icon: 'mdi:close-circle-outline', page: '/(app)/logout' }]
	]);
	let activeIndex = $derived.by(() => {
		const i = parts[0].findIndex((part) => page.route.id == part.page || page.route.id == part.id);
		return i == -1 ? 0 : i;
	});

	let activeIndexLast = $derived(activeIndex);
</script>

<nav class="layout-y-stretch-none h-full border-r border-color preset-filled-surface-50-950">
	{#each parts as tiles, i (i)}
		{#each tiles as { title, icon, url, page, params, loading, external }, j (page ?? url ?? title ?? j)}
			{@const href = loading ? undefined : page ? resolve(page, params) : url}
			<a
				{href}
				rel={external ? 'external' : undefined}
				class="relative layout-x-p-low min-w-36 !gap rounded-base border border-transparent transition-soft hover:border-surface-200-800 hover:bg-surface-100-900 {loading
					? 'blur-sm'
					: ''}"
				transition:slide={{ axis: 'y' }}
			>
				{#if i == 0 && j == activeIndex}
					<SelectionHighlight delta={activeIndexLast - activeIndex} />
				{/if}
				<Ico size="5" {icon} class="z-10" />
				<span class="z-10 text-[14px] font-{i == 0 && j == activeIndex ? 'medium' : 'normal'}"
					>{title}</span
				>
			</a>
		{/each}

		{#if i < parts.length - 1}
			<div class="w-full py-3">
				<div class="border-b border-surface-200-800"></div>
			</div>
		{/if}
	{/each}
</nav>
