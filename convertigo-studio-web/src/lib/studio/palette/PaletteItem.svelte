<script module>
	//data shared accross each instance of this component
	let selectedItem;
</script>

<script>
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import { draggedData } from '$lib/utils/dndStore';
	import { getUrl } from '$lib/utils/service';
	import { createEventDispatcher, onDestroy, onMount } from 'svelte';

	const dispatch = createEventDispatcher();

	/** @type {{item: any}} */
	let { item } = $props();

	let selected = $state();
	let interval;

	//read the data on interval
	onMount(() => {
		interval = setInterval(() => (selected = selectedItem), 100);
	});

	//destroy the timer
	onDestroy(() => clearInterval(interval));

	function handleDragStart(event) {
		const paletteData = { type: 'paletteData', data: item, options: {} };
		event.dataTransfer.setData('text/plain', JSON.stringify(paletteData));
		event.dataTransfer.setData('palettedata', JSON.stringify(paletteData));
		event.dataTransfer.effectAllowed = 'copy';
		$draggedData = paletteData;
	}

	function onClick() {
		selectedItem = item;
		selected = selectedItem;
		dispatch('itemClicked', {
			item: item
		});
	}
</script>

<button
	type="button"
	class={`palette-item ${item === selected ? 'palette-item--selected' : ''}`}
	draggable="true"
	ondragstart={handleDragStart}
	ondragend={(event) => ($draggedData = undefined)}
	onclick={onClick}
>
	<div class="palette-item__iconWrap">
		{#if item.icon?.includes('/')}
			<AutoSvg
				src={`${getUrl()}studio.dbo.GetIcon?iconPath=${item.icon}`}
				class="h-10 w-10 object-contain"
				fill="currentColor"
				alt=""
			/>
		{/if}
	</div>
	<span class="palette-item__name">{item.name}</span>
</button>

<style lang="postcss">
	@reference "../../../app.css";

	.palette-item {
		@apply layout-y-none h-24 w-full items-center overflow-hidden rounded-base border border-surface-300-700 bg-surface-100-900/40 p-2 text-center transition-surface hover:bg-surface-200-800/60 hover:shadow-follow;
	}

	.palette-item--selected {
		@apply border-primary-300-700/60 preset-filled-primary-200-800;
	}

	.palette-item__iconWrap {
		@apply grid h-10 w-full place-items-center overflow-hidden;
	}

	.palette-item__name {
		@apply mt-1 w-full text-[11px] leading-tight font-light text-surface-900 dark:text-gray-100;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		overflow: hidden;
	}
</style>
