<script>
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import Authentication from '$lib/common/Authentication.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { slide } from 'svelte/transition';

	/** @type {{parts: any}} */
	let { parts: _parts } = $props();
	let parts = $derived.by(() => {
		const groups = _parts.map((group) => [...group]);
		if (Authentication.authenticated) {
			groups.push([{ title: 'Logout', icon: 'mdi:close-circle-outline', page: '/(app)/logout' }]);
		}
		return groups;
	});
	let activeIndex = $derived.by(() => {
		const routeId = page.route.id ?? '';
		let bestIndex = -1;
		let bestScore = -1;
		parts[0].forEach((part, i) => {
			let score = -1;
			if (routeId == part.page || routeId == part.id) {
				score = 10_000 + Math.max(part.page?.length ?? 0, part.id?.length ?? 0);
			} else if (part.page && routeId.startsWith(`${part.page}/`)) {
				score = part.page.length;
			} else if (part.id && routeId.startsWith(`${part.id}/`)) {
				score = part.id.length;
			}
			if (score > bestScore) {
				bestScore = score;
				bestIndex = i;
			}
		});
		return bestIndex == -1 ? 0 : bestIndex;
	});
</script>

<nav class="layout-y-stretch-none h-full w-40 border-r border-color bg-surface-100-900">
	{#each parts as tiles, i (i)}
		{#each tiles as { title, icon, url, page, params, loading, external, targetBlank }, j (page ?? url ?? title ?? j)}
			{@const href = loading ? undefined : page ? resolve(page, params) : url}
			{@const isSelected = i == 0 && j == activeIndex}
			<a
				{href}
				rel={targetBlank
					? external
						? 'external noopener noreferrer'
						: 'noopener noreferrer'
					: external
						? 'external'
						: undefined}
				target={targetBlank ? '_blank' : undefined}
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
