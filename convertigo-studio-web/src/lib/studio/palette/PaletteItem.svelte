<script module>
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

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<span
	class="item-center hover:preset-filled-surface m-1 chip flex h-18 w-24 flex-col justify-center border-b border-surface-500 text-left hover:shadow-lg {item ===
	selected
		? 'preset-filled-primary'
		: ''}"
	draggable="true"
	ondragstart={handleDragStart}
	ondragend={(event) => ($draggedData = undefined)}
	onclick={onClick}
>
	{#if item.icon.includes('/')}
		<span class="">
			<AutoSvg
				src="{getUrl()}studio.dbo.GetIcon?iconPath={item.icon}"
				height="35px"
				width="35px"
				fill="currentColor"
			/>
		</span>
	{/if}
	<span class="mt-3 text-center whitespace-normal">
		<span class="text-center text-[11.5px] font-extralight text-surface-900 dark:text-gray-100">
			{item.name}
		</span>
	</span>
</span>
