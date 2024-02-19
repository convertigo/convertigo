<script context="module">
	//data shared accross each instance of this component
	let selectedItem;
</script>

<script>
	import { createEventDispatcher } from 'svelte';
	import { onMount, onDestroy } from 'svelte';
	import { getUrl } from '$lib/utils/service';
	import { draggedData } from '$lib/utils/dndStore';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';

	const dispatch = createEventDispatcher();

	export let item;

	let selected;
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

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<span
	class="chip text-left flex flex-col m-1 w-24 h-18 justify-center item-center hover:shadow-lg hover:variant-filled-surface border-b border-surface-500 {item ===
	selected
		? 'variant-filled-primary'
		: ''}"
	draggable="true"
	on:dragstart={handleDragStart}
	on:dragend={(event) => ($draggedData = undefined)}
	on:click={onClick}
>
	{#if item.icon.includes('/')}
		<span class="">
			<AutoSvg
				src={`${getUrl()}studio.dbo.GetIcon?iconPath=${item.icon}`}
				height="35px"
				width="35px"
				fill="currentColor"
			/>
		</span>
	{/if}
	<span class="whitespace-normal mt-3 text-center">
		<span class="font-extralight text-center text-[11.5px] dark:text-gray-100 text-surface-900">
			{item.name}
		</span>
	</span>
</span>
