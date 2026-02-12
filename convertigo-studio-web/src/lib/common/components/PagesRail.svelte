<script>
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
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
</script>

<nav class="layout-y-stretch-none h-full w-40 border-r border-color bg-surface-100-900">
	{#each parts as tiles, i (i)}
		{#each tiles as { title, icon, url, page, params, loading, external }, j (page ?? url ?? title ?? j)}
			{@const href = loading ? undefined : page ? resolve(page, params) : url}
			{@const isSelected = i == 0 && j == activeIndex}
			<a
				{href}
				rel={external ? 'external' : undefined}
				aria-current={isSelected ? 'page' : undefined}
				class="rail-link {loading ? 'blur-sm' : ''}"
				transition:slide={{ axis: 'y' }}
			>
				{#if isSelected}
					<span class="absolute inset-0 rounded-sm bg-primary-100/70 dark:bg-primary-500/20"></span>
				{/if}
				<Ico size="5" {icon} class="nav-ico z-10 {isSelected ? 'rail-active' : 'text-strong'}" />
				<span
					class="z-10 text-[14px] {isSelected
						? 'font-medium rail-active'
						: 'font-normal text-strong'}">{title}</span
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

<style lang="postcss">
	@reference "../../../app.css";

	:global(.nav-ico svg) {
		stroke-width: 1.4;
		stroke-linecap: round;
		stroke-linejoin: round;
	}
</style>
